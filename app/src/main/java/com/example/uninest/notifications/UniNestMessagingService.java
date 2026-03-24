package com.example.uninest.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.BillsRequest;
import com.example.uninest.model.Calendar;
import com.example.uninest.model.RegisterTokenRequest;
import com.google.firebase.Timestamp;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UniNestMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        Log.d(TAG, "New FCM token: " + token);

        RegisterTokenRequest request = new RegisterTokenRequest(token);

        ApiClient.getNotificationApi()
                .registerToken(request)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        Log.d(TAG, "Token registered with backend: " + response.code());
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e(TAG, "Token registration failed", t);
                    }
                });
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        String title = "UniNest";
        String body = "You have a new notification";
        String targetScreen = "HOME";
        String entityId = "";
        String type = "";
        long eventTime = 0L;
        long createdAt = 0L;

        Map<String, String> data = message.getData();
        if (data != null && !data.isEmpty()) {
            if (data.get("title") != null) {
                title = data.get("title");
            }
            if (data.get("body") != null) {
                body = data.get("body");
            }
            if (data.get("targetScreen") != null) {
                targetScreen = data.get("targetScreen");
            }
            if (data.get("entityId") != null) {
                entityId = data.get("entityId");
            }
            if (data.get("type") != null) {
                type = data.get("type");
            }

            eventTime = parseMillis(data.get("eventTime"));
            if (eventTime <= 0L) {
                eventTime = parseMillis(data.get("scheduledAt"));
            }
            createdAt = parseMillis(data.get("createdAt"));
        }

        Log.d(TAG, "Push received: " + title + " | " + body);

        NotificationPlan plan = buildNotificationPlan(title, body, targetScreen, entityId, type, eventTime, createdAt);
        if (plan == null) {
            Log.d(TAG, "Suppressed notification outside due window: " + title);
            return;
        }

        dispatchNotificationPlan(plan);
    }

    private NotificationPlan buildNotificationPlan(String title,
                                                   String body,
                                                   String targetScreen,
                                                   String entityId,
                                                   String rawType,
                                                   long eventTime,
                                                   long createdAt) {
        String alertType = resolveAlertType(rawType, targetScreen, title, body);

        if ("MESSAGE".equals(alertType) && TenantRealtimeNotificationMonitor.isAppVisible()) {
            // Foreground tenant sessions use the realtime Firestore listener for
            // direct agent-message banners to avoid duplicate pop-ups.
            return null;
        }

        if (shouldAlwaysShowNotification(alertType, targetScreen)) {
            String immediateTarget = "MESSAGE".equals(alertType) ? "MESSAGE" : targetScreen;
            String notificationEntityId = entityId;
            if ("MESSAGE".equals(alertType)) {
                notificationEntityId = firstNonBlank(entityId, title + "|" + body + "|" + createdAt);
            }
            return new NotificationPlan(title, body, immediateTarget, notificationEntityId, System.currentTimeMillis(), null);
        }

        SessionManager sessionManager = new SessionManager(this);
        String currentUserId = sessionManager.getUserId();
        String houseCode = sessionManager.fetchHouseCode();

        if ("RENT".equals(alertType) && currentUserId != null) {
            NotificationPlan billPlan = buildBillPlan(currentUserId, entityId, title, body);
            if (billPlan != null) {
                return billPlan;
            }
        }

        if (("RENT".equals(alertType)
                || "CHORE".equals(alertType)
                || "CALENDAR".equals(alertType)
                || "MAINTENANCE".equals(alertType))
                && houseCode != null
                && !houseCode.trim().isEmpty()) {
            NotificationPlan calendarPlan = buildCalendarPlan(houseCode, currentUserId, entityId, title, body, alertType);
            if (calendarPlan != null) {
                return calendarPlan;
            }
        }

        if (eventTime > 0L) {
            return buildGenericTimedPlan(title, targetScreen, entityId, alertType, eventTime);
        }

        return null;
    }

    private NotificationPlan buildBillPlan(String currentUserId,
                                           String entityId,
                                           String title,
                                           String body) {
        try {
            Response<List<BillsRequest>> response = ApiClient.getBillsApi().getBills(currentUserId).execute();
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }

            BillsRequest bill = findMatchingBill(response.body(), currentUserId, entityId, title, body);
            BillsRequest.Split split = findMySplit(bill, currentUserId);
            Date dueDate = parseBillDate(bill != null ? bill.getDueDate() : null);
            if (bill == null || split == null || split.isPaid() || dueDate == null) {
                return null;
            }

            return new NotificationPlan(
                    firstNonBlank(bill.getTitle(), title, "Bill due"),
                    isOverdue(dueDate) ? "Overdue" : "Due today",
                    "BILLS",
                    bill.getId(),
                    startOfDayMillis(dueDate),
                    buildDedupeKey("BILLS", bill.getId(), startOfDayMillis(dueDate))
            );
        } catch (Exception e) {
            Log.w(TAG, "Unable to evaluate bill push timing", e);
            return null;
        }
    }

    private NotificationPlan buildCalendarPlan(String houseCode,
                                               String currentUserId,
                                               String entityId,
                                               String title,
                                               String body,
                                               String alertType) {
        try {
            Response<List<Calendar>> response = ApiClient.getCalendarApi().getByApartment(houseCode).execute();
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }

            Calendar item = findMatchingCalendarItem(response.body(), entityId, title, body, alertType);
            if (item == null || item.getStartDate() == null || "COMPLETED".equalsIgnoreCase(item.getStatus())) {
                return null;
            }

            Date startDate = item.getStartDate().toDate();
            if (startDate == null) {
                return null;
            }

            String itemType = item.getType() != null
                    ? item.getType().toUpperCase(Locale.getDefault())
                    : "";

            if (itemType.contains("CHORE")) {
                if (currentUserId != null && item.getAssignedTo() != null && !currentUserId.equals(item.getAssignedTo())) {
                    return null;
                }
                return new NotificationPlan(
                        firstNonBlank(item.getTitle(), title, "Chore due"),
                        isOverdue(startDate) ? "Overdue" : "Due today",
                        "CHORES",
                        firstNonBlank(item.getRelatedChoreId(), item.getId()),
                        startOfDayMillis(startDate),
                        buildDedupeKey("CHORES", firstNonBlank(item.getRelatedChoreId(), item.getId()), startOfDayMillis(startDate))
                );
            }

            if (itemType.contains("EVENT") || itemType.contains("MOVE") || itemType.contains("OTHER") || itemType.contains("CUSTOM")) {
                long triggerAt = item.isAllDay() ? startOfDayMillis(startDate) : startDate.getTime();
                String message = item.isAllDay()
                        ? "All-day event today"
                        : "Event starts now";
                return new NotificationPlan(
                        firstNonBlank(item.getTitle(), title, "Event reminder"),
                        message,
                        "CALENDAR",
                        item.getId(),
                        triggerAt,
                        buildDedupeKey("CALENDAR", item.getId(), triggerAt)
                );
            }

            if (itemType.contains("BILL")) {
                long triggerAt = item.isAllDay() ? startOfDayMillis(startDate) : startDate.getTime();
                String message = item.getTitle() != null && item.getTitle().toLowerCase(Locale.getDefault()).contains("reminder")
                        ? (isOverdue(startDate) ? "Reminder overdue" : "Reminder due today")
                        : (isOverdue(startDate) ? "Bill overdue" : "Bill due today");
                return new NotificationPlan(
                        firstNonBlank(item.getTitle(), title, "Bill due"),
                        message,
                        "BILLS",
                        item.getId(),
                        triggerAt,
                        buildDedupeKey("BILLS", item.getId(), triggerAt)
                );
            }

            if (itemType.contains("REMINDER") || itemType.contains("MAINTENANCE")) {
                long triggerAt = item.isAllDay() ? startOfDayMillis(startDate) : startDate.getTime();
                String message = isOverdue(startDate)
                        ? "Overdue"
                        : item.isAllDay() ? "Reminder for today" : "Reminder due now";
                return new NotificationPlan(
                        firstNonBlank(item.getTitle(), title, "Reminder"),
                        message,
                        "CALENDAR",
                        item.getId(),
                        triggerAt,
                        buildDedupeKey("CALENDAR", item.getId(), triggerAt)
                );
            }

            return null;
        } catch (Exception e) {
            Log.w(TAG, "Unable to evaluate calendar push timing", e);
            return null;
        }
    }

    private NotificationPlan buildGenericTimedPlan(String title,
                                                   String targetScreen,
                                                   String entityId,
                                                   String alertType,
                                                   long eventTime) {
        Date eventDate = new Date(eventTime);
        if ("RENT".equals(alertType) || "CHORE".equals(alertType)) {
            return new NotificationPlan(
                    title,
                    isOverdue(eventDate) ? "Overdue" : "Due today",
                    targetScreen,
                    entityId,
                    startOfDayMillis(eventDate),
                    buildDedupeKey(targetScreen, entityId, startOfDayMillis(eventDate))
            );
        }

        return new NotificationPlan(
                title,
                "Event starts now",
                targetScreen,
                entityId,
                eventTime,
                buildDedupeKey(targetScreen, entityId, eventTime)
        );
    }

    private void dispatchNotificationPlan(NotificationPlan plan) {
        if (plan == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (plan.triggerAtMillis <= now) {
            LocalNotificationHelper.showNotification(
                    this,
                    plan.title,
                    plan.body,
                    plan.targetScreen,
                    plan.entityId,
                    plan.dedupeKey
            );
            return;
        }

        scheduleNotification(plan);
    }

    private void scheduleNotification(NotificationPlan plan) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) {
            LocalNotificationHelper.showNotification(this, plan.title, plan.body, plan.targetScreen, plan.entityId);
            return;
        }

        Intent intent = new Intent(this, ScheduledNotificationReceiver.class);
        intent.putExtra(LocalNotificationHelper.EXTRA_TITLE, plan.title);
        intent.putExtra(LocalNotificationHelper.EXTRA_BODY, plan.body);
        intent.putExtra(LocalNotificationHelper.EXTRA_TARGET_SCREEN, plan.targetScreen);
        intent.putExtra(LocalNotificationHelper.EXTRA_ENTITY_ID, plan.entityId);
        intent.putExtra(LocalNotificationHelper.EXTRA_DEDUPE_KEY, plan.dedupeKey);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        int requestCode = Math.abs((plan.targetScreen + ":" + plan.entityId + ":" + plan.triggerAtMillis).hashCode());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, flags);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.triggerAtMillis, pendingIntent);
            } catch (SecurityException exactAlarmError) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.triggerAtMillis, pendingIntent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, plan.triggerAtMillis, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, plan.triggerAtMillis, pendingIntent);
        }

        Log.d(TAG, "Scheduled notification for " + new Date(plan.triggerAtMillis) + " | " + plan.title);
    }

    private String buildDedupeKey(String targetScreen, String entityId, long triggerAtMillis) {
        return firstNonBlank(targetScreen, "HOME") + ":"
                + firstNonBlank(entityId, "unknown") + ":"
                + triggerAtMillis;
    }

    private boolean shouldAlwaysShowNotification(String alertType, String targetScreen) {
        String normalizedTarget = targetScreen != null
                ? targetScreen.trim().toUpperCase(Locale.getDefault())
                : "HOME";
        if ("MESSAGE".equals(alertType)) {
            return true;
        }
        return "MAINTENANCE".equals(alertType) && "TICKETS".equals(normalizedTarget);
    }

    private BillsRequest findMatchingBill(List<BillsRequest> bills,
                                          String currentUserId,
                                          String entityId,
                                          String title,
                                          String body) {
        if (bills == null || bills.isEmpty()) {
            return null;
        }

        if (entityId != null && !entityId.trim().isEmpty()) {
            for (BillsRequest bill : bills) {
                if (bill == null) {
                    continue;
                }
                if (entityId.equals(bill.getId())) {
                    return bill;
                }
                if (bill.getSplits() == null) {
                    continue;
                }
                for (BillsRequest.Split split : bill.getSplits()) {
                    if (split != null && currentUserId.equals(split.getUserId()) && entityId.equals(split.getBillId())) {
                        return bill;
                    }
                }
            }
        }

        String haystack = ((title != null ? title : "") + " " + (body != null ? body : ""))
                .toLowerCase(Locale.getDefault());
        for (BillsRequest bill : bills) {
            if (bill == null || bill.getTitle() == null) {
                continue;
            }
            if (haystack.contains(bill.getTitle().toLowerCase(Locale.getDefault()))) {
                return bill;
            }
        }

        return null;
    }

    private BillsRequest.Split findMySplit(BillsRequest bill, String currentUserId) {
        if (bill == null || bill.getSplits() == null || currentUserId == null) {
            return null;
        }

        for (BillsRequest.Split split : bill.getSplits()) {
            if (split != null && currentUserId.equals(split.getUserId())) {
                return split;
            }
        }
        return null;
    }

    private Calendar findMatchingCalendarItem(List<Calendar> items,
                                              String entityId,
                                              String title,
                                              String body,
                                              String alertType) {
        if (items == null || items.isEmpty()) {
            return null;
        }

        if (entityId != null && !entityId.trim().isEmpty()) {
            for (Calendar item : items) {
                if (item == null) {
                    continue;
                }
                if (entityId.equals(item.getId()) || entityId.equals(item.getRelatedChoreId())) {
                    return item;
                }
            }
        }

        String haystack = ((title != null ? title : "") + " " + (body != null ? body : ""))
                .toLowerCase(Locale.getDefault());
        String normalizedTitle = title != null ? title.toLowerCase(Locale.getDefault()) : "";

        for (Calendar item : items) {
            if (item == null || item.getTitle() == null || !matchesAlertType(item, alertType)) {
                continue;
            }

            String itemTitle = item.getTitle().toLowerCase(Locale.getDefault());
            if (haystack.contains(itemTitle) || (!normalizedTitle.isEmpty() && itemTitle.contains(normalizedTitle))) {
                return item;
            }
        }

        return null;
    }

    private boolean matchesAlertType(Calendar item, String alertType) {
        if (item == null || item.getType() == null) {
            return false;
        }

        String type = item.getType().toUpperCase(Locale.getDefault());
        if ("RENT".equals(alertType)) {
            return type.contains("BILL");
        }
        if ("CHORE".equals(alertType)) {
            return type.contains("CHORE");
        }
        if ("MAINTENANCE".equals(alertType)) {
            return type.contains("MAINTENANCE");
        }
        return type.contains("EVENT")
                || type.contains("REMINDER")
                || type.contains("BILL")
                || type.contains("MAINTENANCE")
                || type.contains("MOVE")
                || type.contains("OTHER")
                || type.contains("CUSTOM");
    }

    private long startOfDayMillis(Date date) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private boolean isOverdue(Date date) {
        if (date == null) {
            return false;
        }

        java.util.Calendar today = java.util.Calendar.getInstance();
        java.util.Calendar target = java.util.Calendar.getInstance();
        target.setTime(date);
        normalizeDay(today);
        normalizeDay(target);
        return target.before(today);
    }

    private void normalizeDay(java.util.Calendar calendar) {
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
    }

    private Date parseBillDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }

        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd"
        };

        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern, Locale.US).parse(raw);
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private long parseMillis(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0L;
        }

        try {
            return Long.parseLong(raw.trim());
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private String resolveAlertType(String type, String targetScreen, String title, String body) {
        if (type != null && !type.trim().isEmpty()) {
            String normalized = type.trim().toUpperCase(Locale.getDefault());
            if (normalized.contains("BILL") || normalized.contains("RENT") || normalized.contains("PAYMENT")) {
                return "RENT";
            }
            if (normalized.contains("CHORE") || normalized.contains("TASK")) {
                return "CHORE";
            }
            if (normalized.contains("MAINTENANCE") || normalized.contains("TICKET")) {
                return "MAINTENANCE";
            }
            if (normalized.contains("EVENT") || normalized.contains("CALENDAR") || normalized.contains("REMINDER")) {
                return "CALENDAR";
            }
            if (normalized.contains("MESSAGE")) {
                return "MESSAGE";
            }
        }

        String haystack = ((targetScreen != null ? targetScreen : "") + " "
                + (title != null ? title : "") + " "
                + (body != null ? body : "")).toLowerCase(Locale.getDefault());

        if (haystack.contains("ticket") || haystack.contains("maintenance")) {
            return "MAINTENANCE";
        }
        if (haystack.contains("bill") || haystack.contains("rent") || haystack.contains("payment")) {
            return "RENT";
        }
        if (haystack.contains("chore") || haystack.contains("task")) {
            return "CHORE";
        }
        if (haystack.contains("calendar") || haystack.contains("event") || haystack.contains("reminder")) {
            return "CALENDAR";
        }
        return "MESSAGE";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static final class NotificationPlan {
        final String title;
        final String body;
        final String targetScreen;
        final String entityId;
        final long triggerAtMillis;
        final String dedupeKey;

        NotificationPlan(String title, String body, String targetScreen, String entityId, long triggerAtMillis, String dedupeKey) {
            this.title = title;
            this.body = body;
            this.targetScreen = targetScreen;
            this.entityId = entityId;
            this.triggerAtMillis = triggerAtMillis;
            this.dedupeKey = dedupeKey;
        }
    }
}
