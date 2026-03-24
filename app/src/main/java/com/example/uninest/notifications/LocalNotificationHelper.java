package com.example.uninest.notifications;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.ui.auth.LettingAgentBuildingsActivity;
import com.example.uninest.ui.auth.LettingAgentTicketsActivity;
import com.example.uninest.ui.auth.TenantBillsActivity;
import com.example.uninest.ui.auth.TenantCalendarActivity;
import com.example.uninest.ui.auth.TenantHomeActivity;
import com.example.uninest.ui.auth.TenantTicketsActivity;
import com.example.uninest.ui.auth.ViewAllTasksActivity;

import java.util.Locale;

public final class LocalNotificationHelper {

    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_BODY = "body";
    public static final String EXTRA_TARGET_SCREEN = "targetScreen";
    public static final String EXTRA_ENTITY_ID = "entityId";
    public static final String EXTRA_DEDUPE_KEY = "dedupeKey";
    public static final String EXTRA_HIGHLIGHT_MESSAGE = "highlightMessage";

    private static final String CHANNEL_ID = "uninest_notifications_v2";
    private static final String CHANNEL_NAME = "UniNest Notifications";
    private static final String MESSAGE_CHANNEL_ID = "uninest_messages_v2";
    private static final String MESSAGE_CHANNEL_NAME = "UniNest Messages";
    private static final String PREF_NAME = "local_notification_state";

    private LocalNotificationHelper() {
    }

    public static void showNotification(Context context,
                                        String title,
                                        String body,
                                        String targetScreen,
                                        String entityId) {
        showNotification(context, title, body, targetScreen, entityId, null);
    }

    public static void showNotification(Context context,
                                        String title,
                                        String body,
                                        String targetScreen,
                                        String entityId,
                                        String dedupeKey) {
        if (context == null) {
            return;
        }

        if (dedupeKey != null && !dedupeKey.trim().isEmpty() && wasDelivered(context, dedupeKey)) {
            return;
        }

        createNotificationChannel(context);
        String channelId = resolveChannelId(targetScreen);

        PendingIntent pendingIntent = buildPendingIntent(context, targetScreen, entityId);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_uninest_notification_small)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher_round))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(resolveCategory(targetScreen))
                .setColor(ContextCompat.getColor(context, R.color.calendar_primary_dark))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVibrate(new long[]{0L, 220L, 120L, 220L})
                .setLights(ContextCompat.getColor(context, R.color.calendar_primary_dark), 700, 400)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true);

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManagerCompat.from(context)
                .notify((int) System.currentTimeMillis(), builder.build());

        if (dedupeKey != null && !dedupeKey.trim().isEmpty()) {
            markDelivered(context, dedupeKey);
        }
    }

    public static void scheduleOrShowNotification(Context context,
                                                  String title,
                                                  String body,
                                                  String targetScreen,
                                                  String entityId,
                                                  long triggerAtMillis,
                                                  String dedupeKey) {
        if (context == null) {
            return;
        }

        if (dedupeKey != null && !dedupeKey.trim().isEmpty() && wasDelivered(context, dedupeKey)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (triggerAtMillis <= 0L || triggerAtMillis <= now) {
            showNotification(context, title, body, targetScreen, entityId, dedupeKey);
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            showNotification(context, title, body, targetScreen, entityId, dedupeKey);
            return;
        }

        Intent intent = new Intent(context, ScheduledNotificationReceiver.class);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_BODY, body);
        intent.putExtra(EXTRA_TARGET_SCREEN, targetScreen);
        intent.putExtra(EXTRA_ENTITY_ID, entityId);
        intent.putExtra(EXTRA_DEDUPE_KEY, dedupeKey);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        int requestCode = Math.abs((firstNonBlank(targetScreen, "HOME")
                + ":" + firstNonBlank(entityId, "unknown")
                + ":" + triggerAtMillis).hashCode());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } catch (SecurityException exactAlarmError) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    private static PendingIntent buildPendingIntent(Context context, String targetScreen, String entityId) {
        String target = targetScreen != null
                ? targetScreen.trim().toUpperCase(Locale.getDefault())
                : "HOME";

        if ("MESSAGE".equals(target)) {
            return null;
        }

        Intent intent = buildIntent(context, target, entityId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                intent,
                flags
        );
    }

    private static Intent buildIntent(Context context, String target, String entityId) {
        SessionManager sessionManager = new SessionManager(context);
        String role = sessionManager.getUserRole();
        boolean isAgent = "1".equals(role) || "LETTINGAGENT".equalsIgnoreCase(role);

        Intent intent;
        switch (target) {
            case "TICKETS":
                if (isAgent) {
                    intent = new Intent(context, LettingAgentTicketsActivity.class);
                } else {
                    intent = new Intent(context, TenantTicketsActivity.class);
                }
                intent.putExtra("highlight_ticket_id", entityId);
                intent.putExtra(EXTRA_HIGHLIGHT_MESSAGE, "View maintenance here");
                break;
            case "BILLS":
                intent = new Intent(context, TenantBillsActivity.class);
                intent.putExtra("highlight_bill_id", entityId);
                intent.putExtra(EXTRA_HIGHLIGHT_MESSAGE, "View bills here");
                break;
            case "CHORES":
                intent = new Intent(context, ViewAllTasksActivity.class);
                intent.putExtra("highlight_task_id", entityId);
                break;
            case "CALENDAR":
                intent = new Intent(context, TenantCalendarActivity.class);
                intent.putExtra("highlight_event_id", entityId);
                intent.putExtra(EXTRA_HIGHLIGHT_MESSAGE, "View events here");
                break;
            case "HOME":
            default:
                if (isAgent) {
                    intent = new Intent(context, LettingAgentBuildingsActivity.class);
                } else {
                    intent = new Intent(context, TenantHomeActivity.class);
                }
                intent.putExtra("open_notifications", true);
                break;
        }
        return intent;
    }

    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notifications from UniNest");
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0L, 220L, 120L, 220L});
        channel.enableLights(true);
        channel.setLightColor(ContextCompat.getColor(context, R.color.calendar_primary_dark));
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setSound(defaultSound, audioAttributes);
        manager.createNotificationChannel(channel);

        NotificationChannel messageChannel = new NotificationChannel(
                MESSAGE_CHANNEL_ID,
                MESSAGE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        messageChannel.setDescription("Instant messages from your letting agent");
        messageChannel.enableVibration(true);
        messageChannel.setVibrationPattern(new long[]{0L, 260L, 120L, 260L});
        messageChannel.enableLights(true);
        messageChannel.setLightColor(ContextCompat.getColor(context, R.color.calendar_primary_dark));
        messageChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        messageChannel.setSound(defaultSound, audioAttributes);
        manager.createNotificationChannel(messageChannel);
    }

    private static String resolveChannelId(String targetScreen) {
        String target = targetScreen != null
                ? targetScreen.trim().toUpperCase(Locale.getDefault())
                : "HOME";
        return "MESSAGE".equals(target) ? MESSAGE_CHANNEL_ID : CHANNEL_ID;
    }

    private static String resolveCategory(String targetScreen) {
        String target = targetScreen != null
                ? targetScreen.trim().toUpperCase(Locale.getDefault())
                : "HOME";
        return "MESSAGE".equals(target)
                ? NotificationCompat.CATEGORY_MESSAGE
                : NotificationCompat.CATEGORY_REMINDER;
    }

    private static boolean wasDelivered(Context context, String dedupeKey) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(dedupeKey, false);
    }

    private static void markDelivered(Context context, String dedupeKey) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(dedupeKey, true).apply();
    }

    public static boolean hasDeliveredNotification(Context context, String dedupeKey) {
        if (context == null || dedupeKey == null || dedupeKey.trim().isEmpty()) {
            return false;
        }
        return wasDelivered(context, dedupeKey);
    }

    public static void clearDeliveredNotification(Context context, String dedupeKey) {
        if (context == null || dedupeKey == null || dedupeKey.trim().isEmpty()) {
            return;
        }
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        preferences.edit().remove(dedupeKey).apply();
    }

    public static void clearAllDeliveredNotifications(Context context) {
        if (context == null) {
            return;
        }
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        preferences.edit().clear().apply();
    }

    private static String firstNonBlank(String... values) {
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
}
