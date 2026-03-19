package com.example.uninest.ui.auth;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.BillsRequest;
import com.example.uninest.model.Calendar;
import com.example.uninest.model.HomeAlert;
import com.example.uninest.model.User;
import com.example.uninest.notifications.LocalNotificationHelper;
import com.example.uninest.utils.ContactUtils;
import com.example.uninest.utils.ImageUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TenantHomeActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private String houseCode;
    private String currentUserId;

    private androidx.cardview.widget.CardView cardLeaderboard;
    private LinearLayout layoutLeaderboardRows;
    private View layoutYourStanding;
    private TextView tvYourRank;
    private TextView tvYourPoints;
    private ImageView ivYourLeaderBadge;

    private Button btnEmergencyContact;
    private Button btnSupport;
    private View btnOpenNotifications;
    private TextView tvAlertBadge;
    private TextView tvAlertSummary;

    private View heroCard;
    private View heroOrbLarge;
    private View heroOrbSmall;
    private View layoutSupportActions;
    private View layoutQuickAccess;

    private FirebaseFirestore firestore;
    private final List<HomeAlert> notificationList = new ArrayList<>();
    private final List<HomeAlert> filteredNotificationList = new ArrayList<>();
    private final List<HomeAlert> firestoreNotificationList = new ArrayList<>();
    private final List<BillsRequest> alertBills = new ArrayList<>();
    private final List<Calendar> alertCalendarItems = new ArrayList<>();

    private static final String ALERT_FILTER_ALL = "ALL";
    private static final String ALERT_FILTER_BILLS = "RENT";
    private static final String ALERT_FILTER_CHORES = "CHORE";
    private static final String ALERT_FILTER_MAINTENANCE = "MAINTENANCE";
    private static final String ALERT_FILTER_CALENDAR = "CALENDAR";
    private static final String ALERT_FILTER_MESSAGES = "MESSAGE";
    private String activeAlertFilter = ALERT_FILTER_ALL;
    private int notificationLoadToken = 0;
    private int pendingNotificationSourceLoads = 0;

    private BottomSheetDialog notificationCenterDialog;
    private HomeAlertAdapter notificationCenterAdapter;
    private RecyclerView rvNotificationCenter;
    private View layoutCenterEmpty;
    private TextView tvCenterEmpty;
    private TextView tvCenterEmptyTitle;
    private TextView tvCenterAlertCount;
    private TextView tvCenterCountChip;
    private View btnCenterClearAll;
    private FrameLayout layoutCenterEmptyIconShell;
    private ImageView imgCenterEmptyIcon;
    private TextView filterAlertsAll;
    private TextView filterAlertsBills;
    private TextView filterAlertsChores;
    private TextView filterAlertsMaintenance;
    private TextView filterAlertsCalendar;
    private TextView filterAlertsMessages;
    private AnimatorSet bellPulseAnimator;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_home);

        requestNotificationPermissionIfNeeded();
        sessionManager = new SessionManager(this);
        houseCode = sessionManager.fetchHouseCode();
        currentUserId = FirebaseAuth.getInstance().getUid();
        firestore = FirebaseFirestore.getInstance();

        bindViews();
        setupQuickActions();
        setupHomeAnimations();

        btnEmergencyContact.setOnClickListener(v -> ContactUtils.dialEmergency(this));
        btnSupport.setOnClickListener(v -> ContactUtils.emailSupport(this));
        btnOpenNotifications.setOnClickListener(v -> showNotificationCenter());

        initHeader();

        if (houseCode != null) {
            loadLeaderboardData();
        }

        loadNotifications();
        setupBottomNav();

        if (getIntent() != null && getIntent().getBooleanExtra("open_notifications", false)) {
            btnOpenNotifications.post(this::showNotificationCenter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void bindViews() {
        heroCard = findViewById(R.id.heroCard);
        heroOrbLarge = findViewById(R.id.heroOrbLarge);
        heroOrbSmall = findViewById(R.id.heroOrbSmall);
        layoutSupportActions = findViewById(R.id.layoutSupportActions);
        layoutQuickAccess = findViewById(R.id.layoutQuickAccess);

        btnEmergencyContact = findViewById(R.id.btnEmergencyContact);
        btnSupport = findViewById(R.id.btnSupport);
        btnOpenNotifications = findViewById(R.id.btnOpenNotifications);
        tvAlertBadge = findViewById(R.id.tvAlertBadge);
        tvAlertSummary = findViewById(R.id.tvAlertSummary);

        cardLeaderboard = findViewById(R.id.cardLeaderboard);
        layoutLeaderboardRows = findViewById(R.id.layoutLeaderboardRows);
        layoutYourStanding = findViewById(R.id.layoutYourStanding);
        tvYourRank = findViewById(R.id.tvYourRank);
        tvYourPoints = findViewById(R.id.tvYourPoints);
        ivYourLeaderBadge = findViewById(R.id.ivYourLeaderBadge);
    }

    private void initHeader() {
        TextView tvGreeting = findViewById(R.id.tvGreeting);
        String fullName = sessionManager.getUserFullName();
        String firstName = (fullName != null && fullName.contains(" "))
                ? fullName.split(" ")[0]
                : (fullName != null ? fullName : "User");

        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String timeGreeting = (hour < 12) ? "Good morning" : (hour < 17) ? "Good afternoon" : "Good evening";
        tvGreeting.setText(timeGreeting + ",\n" + firstName + "!");
    }

    private void setupHomeAnimations() {
        heroCard.setAlpha(0f);
        heroCard.setTranslationY(dp(18));
        layoutSupportActions.setAlpha(0f);
        layoutSupportActions.setTranslationY(dp(14));
        layoutQuickAccess.setAlpha(0f);
        layoutQuickAccess.setTranslationY(dp(14));

        heroCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(520)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        layoutSupportActions.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(110)
                .setDuration(420)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        layoutQuickAccess.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(180)
                .setDuration(420)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        startFloatingAnimation(heroOrbLarge, 18f, 4800L, 0L);
        startFloatingAnimation(heroOrbSmall, -14f, 4200L, 240L);
    }

    private void startFloatingAnimation(View target, float travel, long duration, long startDelay) {
        ObjectAnimator translateY = ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, 0f, travel, 0f);
        translateY.setDuration(duration);
        translateY.setRepeatCount(ObjectAnimator.INFINITE);
        translateY.setStartDelay(startDelay);
        translateY.setInterpolator(new LinearInterpolator());

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 1.06f, 1f);
        scaleX.setDuration(duration);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setStartDelay(startDelay);
        scaleX.setInterpolator(new LinearInterpolator());

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 1.06f, 1f);
        scaleY.setDuration(duration);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setStartDelay(startDelay);
        scaleY.setInterpolator(new LinearInterpolator());

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(translateY, scaleX, scaleY);
        animatorSet.start();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void loadNotifications() {
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        final int loadToken = ++notificationLoadToken;
        pendingNotificationSourceLoads = 0;

        requestFirestoreNotifications(loadToken);
        requestBillNotifications(loadToken);

        if (houseCode != null && !houseCode.trim().isEmpty()) {
            requestCalendarNotifications(loadToken);
        }
    }

    private void requestFirestoreNotifications(int loadToken) {
        pendingNotificationSourceLoads++;
        firestore.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (loadToken != notificationLoadToken) {
                        return;
                    }

                    firestoreNotificationList.clear();

                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            HomeAlert alert = new HomeAlert();
                            alert.setId(doc.getId());
                            alert.setTitle(doc.getString("title"));
                            alert.setSubtitle(doc.getString("body"));
                            alert.setType(resolveAlertType(
                                    doc.getString("type"),
                                    doc.getString("targetScreen"),
                                    doc.getString("title"),
                                    doc.getString("body")
                            ));
                            alert.setTargetScreen(doc.getString("targetScreen"));
                            alert.setEntityId(doc.getString("entityId"));
                            alert.setCreatedAt(parseMillis(doc.get("createdAt")));
                            alert.setEventTime(parseMillis(doc.get("eventTime")));
                            alert.setDismissible(true);
                            firestoreNotificationList.add(alert);
                        }
                    } else {
                        Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                    }

                    onNotificationSourceLoaded(loadToken);
                });
    }

    private void requestBillNotifications(int loadToken) {
        pendingNotificationSourceLoads++;
        ApiClient.getBillsApi().getBills(currentUserId).enqueue(new Callback<List<BillsRequest>>() {
            @Override
            public void onResponse(Call<List<BillsRequest>> call, Response<List<BillsRequest>> response) {
                if (loadToken != notificationLoadToken) {
                    return;
                }

                alertBills.clear();
                if (response.isSuccessful() && response.body() != null) {
                    alertBills.addAll(response.body());
                }
                onNotificationSourceLoaded(loadToken);
            }

            @Override
            public void onFailure(Call<List<BillsRequest>> call, Throwable t) {
                if (loadToken != notificationLoadToken) {
                    return;
                }

                alertBills.clear();
                onNotificationSourceLoaded(loadToken);
            }
        });
    }

    private void requestCalendarNotifications(int loadToken) {
        pendingNotificationSourceLoads++;
        ApiClient.getCalendarApi().getByApartment(houseCode).enqueue(new Callback<List<Calendar>>() {
            @Override
            public void onResponse(Call<List<Calendar>> call, Response<List<Calendar>> response) {
                if (loadToken != notificationLoadToken) {
                    return;
                }

                alertCalendarItems.clear();
                if (response.isSuccessful() && response.body() != null) {
                    alertCalendarItems.addAll(response.body());
                }
                onNotificationSourceLoaded(loadToken);
            }

            @Override
            public void onFailure(Call<List<Calendar>> call, Throwable t) {
                if (loadToken != notificationLoadToken) {
                    return;
                }

                alertCalendarItems.clear();
                onNotificationSourceLoaded(loadToken);
            }
        });
    }

    private void onNotificationSourceLoaded(int loadToken) {
        if (loadToken != notificationLoadToken) {
            return;
        }

        pendingNotificationSourceLoads = Math.max(0, pendingNotificationSourceLoads - 1);
        if (pendingNotificationSourceLoads > 0) {
            return;
        }

        rebuildNotificationFeed();
        rebuildFilteredNotifications();
        updateAlertState();
    }

    private long parseMillis(Object raw) {
        if (raw instanceof Long) {
            return (Long) raw;
        }
        if (raw instanceof Double) {
            return ((Double) raw).longValue();
        }
        if (raw instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) raw).toDate().getTime();
        }
        if (raw instanceof String) {
            try {
                return Long.parseLong((String) raw);
            } catch (Exception ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private void rebuildNotificationFeed() {
        LinkedHashMap<String, HomeAlert> feed = new LinkedHashMap<>();

        for (HomeAlert rawAlert : firestoreNotificationList) {
            String normalizedType = normalizeAlertType(rawAlert.getType());

            if (ALERT_FILTER_MESSAGES.equals(normalizedType) || ALERT_FILTER_MAINTENANCE.equals(normalizedType)) {
                HomeAlert preservedAlert = cloneAlert(rawAlert);
                preservedAlert.setType(normalizedType);
                preservedAlert.setDismissible(true);
                addAlertIfMissing(feed, rawAlertKey(rawAlert, normalizedType), preservedAlert);
            }
        }

        appendDerivedBillAlerts(feed);
        appendDerivedChoreAlerts(feed);
        appendDerivedEventAlerts(feed);

        notificationList.clear();
        notificationList.addAll(feed.values());
        Collections.sort(notificationList, (first, second) ->
                Long.compare(notificationSortTime(second), notificationSortTime(first)));
    }

    private HomeAlert buildBillAlertFromRaw(HomeAlert rawAlert,
                                            Map<String, BillsRequest> billsById,
                                            Map<String, Calendar> calendarById) {
        BillsRequest matchedBill = findBillForAlert(rawAlert, billsById);
        if (matchedBill != null) {
            return createBillAlertFromBill(matchedBill, rawAlert.getId(), true, rawAlert.getCreatedAt());
        }

        Calendar matchedBillReminder = findCalendarItemForAlert(rawAlert, calendarById, null, ALERT_FILTER_BILLS);
        if (matchedBillReminder != null) {
            return createBillAlertFromCalendar(matchedBillReminder, rawAlert.getId(), true, rawAlert.getCreatedAt());
        }

        return null;
    }

    private HomeAlert buildChoreAlertFromRaw(HomeAlert rawAlert,
                                             Map<String, Calendar> calendarById,
                                             Map<String, Calendar> choresByRelatedId) {
        Calendar matchedChore = findCalendarItemForAlert(rawAlert, calendarById, choresByRelatedId, ALERT_FILTER_CHORES);
        if (matchedChore == null || !shouldKeepRawChoreAlert(matchedChore)) {
            return null;
        }

        return createChoreAlert(matchedChore, rawAlert.getId(), true, rawAlert.getCreatedAt());
    }

    private HomeAlert buildEventAlertFromRaw(HomeAlert rawAlert, Map<String, Calendar> calendarById) {
        Calendar matchedEvent = findCalendarItemForAlert(rawAlert, calendarById, null, ALERT_FILTER_CALENDAR);
        if (matchedEvent == null || !shouldShowEventAlert(matchedEvent)) {
            return null;
        }

        return createEventAlert(matchedEvent, rawAlert.getId(), true, rawAlert.getCreatedAt());
    }

    private void appendDerivedBillAlerts(LinkedHashMap<String, HomeAlert> feed) {
        long now = System.currentTimeMillis();

        for (BillsRequest bill : alertBills) {
            HomeAlert alert = createBillAlertFromBill(bill, null, false, now);
            String deliveryKey = deliveryKeyForBill(bill);
            if (alert != null && hasDeliveredAlert(deliveryKey)) {
                alert.setId(localAlertId(deliveryKey));
                alert.setDismissible(true);
                addAlertIfMissing(feed, feedKeyForAlert(alert), alert);
            }
        }

        for (Calendar item : alertCalendarItems) {
            HomeAlert alert = createBillAlertFromCalendar(item, null, false, now);
            String deliveryKey = deliveryKeyForCalendarBill(item);
            if (alert != null && hasDeliveredAlert(deliveryKey)) {
                alert.setId(localAlertId(deliveryKey));
                alert.setDismissible(true);
                addAlertIfMissing(feed, feedKeyForAlert(alert), alert);
            }
        }
    }

    private void appendDerivedChoreAlerts(LinkedHashMap<String, HomeAlert> feed) {
        long now = System.currentTimeMillis();
        for (Calendar item : alertCalendarItems) {
            if (!shouldShowDerivedChoreAlert(item)) {
                continue;
            }

            HomeAlert alert = createChoreAlert(item, null, false, now);
            String deliveryKey = deliveryKeyForChore(item);
            if (alert != null && hasDeliveredAlert(deliveryKey)) {
                alert.setId(localAlertId(deliveryKey));
                alert.setDismissible(true);
                addAlertIfMissing(feed, feedKeyForAlert(alert), alert);
            }
        }
    }

    private void appendDerivedEventAlerts(LinkedHashMap<String, HomeAlert> feed) {
        long now = System.currentTimeMillis();
        for (Calendar item : alertCalendarItems) {
            if (!shouldShowEventAlert(item)) {
                continue;
            }

            HomeAlert alert = createEventAlert(item, null, false, now);
            String deliveryKey = deliveryKeyForEvent(item);
            if (alert != null && hasDeliveredAlert(deliveryKey)) {
                alert.setId(localAlertId(deliveryKey));
                alert.setDismissible(true);
                addAlertIfMissing(feed, feedKeyForAlert(alert), alert);
            }
        }
    }

    private HomeAlert createBillAlertFromBill(BillsRequest bill,
                                              String rawId,
                                              boolean dismissible,
                                              long createdAt) {
        if (!shouldShowBillAlert(bill)) {
            return null;
        }

        BillsRequest.Split mySplit = findCurrentUserSplit(bill);
        Date dueDate = parseBillDate(bill.getDueDate());
        if (mySplit == null || dueDate == null) {
            return null;
        }

        HomeAlert alert = new HomeAlert();
        alert.setId(rawId);
        alert.setTitle(firstNonBlank(bill.getTitle(), "Bill due"));
        alert.setSubtitle(String.format(
                Locale.getDefault(),
                "You owe EUR %.2f of EUR %.2f total. %s",
                mySplit.getAmountOwed(),
                bill.getTotalAmount(),
                isOverdue(dueDate) ? "This bill is overdue." : "This bill is due today."
        ));
        alert.setType(ALERT_FILTER_BILLS);
        alert.setTargetScreen("BILLS");
        alert.setEntityId(bill.getId());
        alert.setCreatedAt(createdAt > 0 ? createdAt : System.currentTimeMillis());
        alert.setEventTime(dueDate.getTime());
        alert.setMetaOverride(buildDueMetaLabel(dueDate));
        alert.setDismissible(dismissible);
        return alert;
    }

    private HomeAlert createBillAlertFromCalendar(Calendar item,
                                                  String rawId,
                                                  boolean dismissible,
                                                  long createdAt) {
        if (!shouldShowCalendarBillAlert(item)) {
            return null;
        }

        Date dueDate = dateFromTimestamp(item.getStartDate());
        if (dueDate == null) {
            return null;
        }

        String description = safeTrim(item.getDescription());
        boolean reminderStyle = safeTrim(item.getTitle()).toLowerCase(Locale.getDefault()).contains("reminder");
        String fallbackDescription;
        if (item.getAmount() != null && item.getAmount() > 0) {
            fallbackDescription = String.format(
                    Locale.getDefault(),
                    "%s Amount due: EUR %.2f.",
                    reminderStyle
                            ? (isOverdue(dueDate) ? "This reminder is overdue." : "This reminder is due today.")
                            : (isOverdue(dueDate) ? "This bill is overdue." : "This bill is due today."),
                    item.getAmount()
            );
        } else {
            fallbackDescription = reminderStyle
                    ? (isOverdue(dueDate) ? "This reminder is overdue." : "This reminder is due today.")
                    : (isOverdue(dueDate) ? "This bill is overdue." : "This bill is due today.");
        }

        HomeAlert alert = new HomeAlert();
        alert.setId(rawId);
        alert.setTitle(firstNonBlank(item.getTitle(), reminderStyle ? "Reminder due" : "Bill due"));
        alert.setSubtitle(firstNonBlank(description, fallbackDescription));
        alert.setType(ALERT_FILTER_BILLS);
        alert.setTargetScreen("BILLS");
        alert.setEntityId(item.getId());
        alert.setCreatedAt(createdAt > 0 ? createdAt : System.currentTimeMillis());
        alert.setEventTime(dueDate.getTime());
        alert.setMetaOverride(buildDueMetaLabel(dueDate));
        alert.setDismissible(dismissible);
        return alert;
    }

    private HomeAlert createChoreAlert(Calendar item,
                                       String rawId,
                                       boolean dismissible,
                                       long createdAt) {
        if (item == null || !isAssignedToCurrentUser(item) || isCompleted(item)) {
            return null;
        }

        Date dueDate = dateFromTimestamp(item.getStartDate());
        boolean overdue = isOverdue(dueDate);
        boolean dueToday = isDueToday(dueDate);
        boolean assignedByOther = isAssignedByAnotherUser(item);

        if (!assignedByOther && !dueToday && !overdue) {
            return null;
        }

        String leadText;
        if (overdue) {
            leadText = "This chore is overdue.";
        } else if (dueToday) {
            leadText = "This chore is due today.";
        } else {
            leadText = "Assigned to you by another housemate.";
        }

        String description = safeTrim(item.getDescription());
        HomeAlert alert = new HomeAlert();
        alert.setId(rawId);
        alert.setTitle(firstNonBlank(item.getTitle(), "Chore update"));
        alert.setSubtitle(firstNonBlank(combineSentences(leadText, description), leadText));
        alert.setType(ALERT_FILTER_CHORES);
        alert.setTargetScreen("CHORES");
        alert.setEntityId(firstNonBlank(item.getRelatedChoreId(), item.getId()));
        alert.setCreatedAt(createdAt > 0 ? createdAt : System.currentTimeMillis());
        alert.setEventTime((overdue || dueToday) && dueDate != null ? dueDate.getTime() : 0L);
        alert.setMetaOverride((overdue || dueToday) ? buildDueMetaLabel(dueDate) : null);
        alert.setDismissible(dismissible);
        return alert;
    }

    private HomeAlert createEventAlert(Calendar item,
                                       String rawId,
                                       boolean dismissible,
                                       long createdAt) {
        if (!shouldShowEventAlert(item)) {
            return null;
        }

        Date startDate = dateFromTimestamp(item.getStartDate());
        if (startDate == null) {
            return null;
        }

        boolean allDay = item.isAllDay();
        String description = safeTrim(item.getDescription());
        String timeLabel = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(startDate);

        HomeAlert alert = new HomeAlert();
        alert.setId(rawId);
        alert.setTitle(firstNonBlank(item.getTitle(), "Today's event"));
        alert.setSubtitle(firstNonBlank(
                description,
                allDay ? "All-day event scheduled for today." : "Event scheduled for " + timeLabel + "."
        ));
        alert.setType(ALERT_FILTER_CALENDAR);
        alert.setTargetScreen("CALENDAR");
        alert.setEntityId(item.getId());
        alert.setCreatedAt(createdAt > 0 ? createdAt : System.currentTimeMillis());
        alert.setEventTime(startDate.getTime());
        alert.setMetaOverride(allDay ? "All day" : "Today at " + timeLabel);
        alert.setDismissible(dismissible);
        return alert;
    }

    private BillsRequest findBillForAlert(HomeAlert rawAlert, Map<String, BillsRequest> billsById) {
        String entityId = safeTrim(rawAlert.getEntityId());
        if (!entityId.isEmpty()) {
            BillsRequest directMatch = billsById.get(entityId);
            if (directMatch != null) {
                return directMatch;
            }

            for (BillsRequest bill : alertBills) {
                if (bill == null || bill.getSplits() == null) {
                    continue;
                }
                for (BillsRequest.Split split : bill.getSplits()) {
                    if (entityId.equals(split.getBillId())) {
                        return bill;
                    }
                }
            }
        }

        String rawText = (safeTrim(rawAlert.getTitle()) + " " + safeTrim(rawAlert.getSubtitle()))
                .toLowerCase(Locale.getDefault());
        BillsRequest matchedBill = null;

        for (BillsRequest bill : alertBills) {
            String billTitle = safeTrim(bill != null ? bill.getTitle() : null);
            if (billTitle.isEmpty()) {
                continue;
            }

            if (!rawText.contains(billTitle.toLowerCase(Locale.getDefault()))) {
                continue;
            }

            if (matchedBill != null && !safeTrim(matchedBill.getId()).equals(safeTrim(bill.getId()))) {
                return null;
            }
            matchedBill = bill;
        }

        return matchedBill;
    }

    private Calendar findCalendarItemForAlert(HomeAlert rawAlert,
                                              Map<String, Calendar> calendarById,
                                              Map<String, Calendar> choresByRelatedId,
                                              String filter) {
        String entityId = safeTrim(rawAlert.getEntityId());
        if (!entityId.isEmpty()) {
            Calendar directMatch = calendarById.get(entityId);
            if (matchesCalendarFilter(directMatch, filter)) {
                return directMatch;
            }

            if (choresByRelatedId != null) {
                Calendar choreMatch = choresByRelatedId.get(entityId);
                if (matchesCalendarFilter(choreMatch, filter)) {
                    return choreMatch;
                }
            }
        }

        String titleText = safeTrim(rawAlert.getTitle()).toLowerCase(Locale.getDefault());
        String bodyText = safeTrim(rawAlert.getSubtitle()).toLowerCase(Locale.getDefault());
        Calendar matchedItem = null;

        for (Calendar item : alertCalendarItems) {
            if (!matchesCalendarFilter(item, filter)) {
                continue;
            }

            String candidateTitle = safeTrim(item.getTitle()).toLowerCase(Locale.getDefault());
            if (candidateTitle.isEmpty()) {
                continue;
            }

            boolean titleMatches = !titleText.isEmpty() && candidateTitle.contains(titleText);
            boolean bodyMatches = !bodyText.isEmpty() && bodyText.contains(candidateTitle);
            boolean reverseTitleMatch = !candidateTitle.isEmpty() && titleText.contains(candidateTitle);

            if (!titleMatches && !bodyMatches && !reverseTitleMatch) {
                continue;
            }

            if (matchedItem != null && !safeTrim(matchedItem.getId()).equals(safeTrim(item.getId()))) {
                return null;
            }
            matchedItem = item;
        }

        return matchedItem;
    }

    private boolean shouldShowBillAlert(BillsRequest bill) {
        BillsRequest.Split mySplit = findCurrentUserSplit(bill);
        Date dueDate = parseBillDate(bill != null ? bill.getDueDate() : null);
        return mySplit != null && !mySplit.isPaid() && dueDate != null && (isDueToday(dueDate) || isOverdue(dueDate));
    }

    private boolean shouldShowCalendarBillAlert(Calendar item) {
        Date dueDate = dateFromTimestamp(item != null ? item.getStartDate() : null);
        return matchesCalendarFilter(item, ALERT_FILTER_BILLS)
                && !isCompleted(item)
                && dueDate != null
                && (isDueToday(dueDate) || isOverdue(dueDate));
    }

    private boolean shouldKeepRawChoreAlert(Calendar item) {
        if (item == null || !matchesCalendarFilter(item, ALERT_FILTER_CHORES) || !isAssignedToCurrentUser(item) || isCompleted(item)) {
            return false;
        }

        Date dueDate = dateFromTimestamp(item.getStartDate());
        return isAssignedByAnotherUser(item) || isDueToday(dueDate) || isOverdue(dueDate);
    }

    private boolean shouldShowDerivedChoreAlert(Calendar item) {
        if (item == null || !matchesCalendarFilter(item, ALERT_FILTER_CHORES) || !isAssignedToCurrentUser(item) || isCompleted(item)) {
            return false;
        }

        Date dueDate = dateFromTimestamp(item.getStartDate());
        return isDueToday(dueDate) || isOverdue(dueDate);
    }

    private boolean shouldShowEventAlert(Calendar item) {
        if (item == null || !matchesCalendarFilter(item, ALERT_FILTER_CALENDAR) || isCompleted(item) || !isVisibleEventForCurrentUser(item)) {
            return false;
        }

        Date startDate = dateFromTimestamp(item.getStartDate());
        if (startDate == null || !isDueToday(startDate)) {
            return false;
        }

        if (!item.isAllDay() && item.getEndDate() != null) {
            Date endDate = dateFromTimestamp(item.getEndDate());
            if (endDate != null && endDate.getTime() < System.currentTimeMillis()) {
                return false;
            }
        }

        return true;
    }

    private BillsRequest.Split findCurrentUserSplit(BillsRequest bill) {
        if (bill == null || bill.getSplits() == null) {
            return null;
        }

        for (BillsRequest.Split split : bill.getSplits()) {
            if (split != null && currentUserId.equals(split.getUserId())) {
                return split;
            }
        }
        return null;
    }

    private boolean matchesCalendarFilter(Calendar item, String filter) {
        if (item == null || item.getType() == null) {
            return false;
        }

        String eventType = item.getType().toUpperCase(Locale.getDefault());
        if (ALERT_FILTER_CHORES.equals(filter)) {
            return eventType.contains("CHORE");
        }
        if (ALERT_FILTER_BILLS.equals(filter)) {
            return eventType.contains("BILL");
        }
        if (ALERT_FILTER_CALENDAR.equals(filter)) {
            return eventType.contains("EVENT")
                    || eventType.contains("MOVE")
                    || eventType.contains("OTHER")
                    || eventType.contains("CUSTOM");
        }
        if (ALERT_FILTER_MAINTENANCE.equals(filter)) {
            return eventType.contains("MAINTENANCE") || eventType.contains("TICKET");
        }
        return false;
    }

    private boolean isAssignedToCurrentUser(Calendar item) {
        return item != null && currentUserId != null && currentUserId.equals(item.getAssignedTo());
    }

    private boolean isAssignedByAnotherUser(Calendar item) {
        return item != null
                && isAssignedToCurrentUser(item)
                && !isBlank(item.getCreatedBy())
                && !currentUserId.equals(item.getCreatedBy());
    }

    private boolean isVisibleEventForCurrentUser(Calendar item) {
        return item != null && currentUserId != null
                && (currentUserId.equals(item.getAssignedTo()) || currentUserId.equals(item.getCreatedBy()));
    }

    private boolean isCompleted(Calendar item) {
        return item != null && "COMPLETED".equalsIgnoreCase(item.getStatus());
    }

    private Date parseBillDate(String raw) {
        if (isBlank(raw)) {
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

    private Date dateFromTimestamp(com.example.uninest.model.FirestoreTimestamp timestamp) {
        return timestamp != null ? timestamp.toDate() : null;
    }

    private boolean isDueToday(Date date) {
        if (date == null) {
            return false;
        }

        java.util.Calendar today = java.util.Calendar.getInstance();
        java.util.Calendar target = java.util.Calendar.getInstance();
        target.setTime(date);
        normalizeCalendarDay(today);
        normalizeCalendarDay(target);
        return today.get(java.util.Calendar.YEAR) == target.get(java.util.Calendar.YEAR)
                && today.get(java.util.Calendar.DAY_OF_YEAR) == target.get(java.util.Calendar.DAY_OF_YEAR);
    }

    private boolean isOverdue(Date date) {
        if (date == null) {
            return false;
        }

        java.util.Calendar today = java.util.Calendar.getInstance();
        java.util.Calendar target = java.util.Calendar.getInstance();
        target.setTime(date);
        normalizeCalendarDay(today);
        normalizeCalendarDay(target);
        return target.before(today);
    }

    private void normalizeCalendarDay(java.util.Calendar calendar) {
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
    }

    private long startOfDayMillis(Date date) {
        if (date == null) {
            return 0L;
        }
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(date);
        normalizeCalendarDay(calendar);
        return calendar.getTimeInMillis();
    }

    private String buildDueMetaLabel(Date dueDate) {
        if (dueDate == null) {
            return "Needs attention";
        }
        if (isOverdue(dueDate)) {
            return "Overdue";
        }
        if (isDueToday(dueDate)) {
            return "Due today";
        }
        return new SimpleDateFormat("dd MMM", Locale.getDefault()).format(dueDate);
    }

    private String normalizeAlertType(String type) {
        if (isBlank(type)) {
            return ALERT_FILTER_MESSAGES;
        }

        String normalized = type.trim().toUpperCase(Locale.getDefault());
        if (normalized.contains("BILL") || normalized.contains("RENT") || normalized.contains("PAYMENT")) {
            return ALERT_FILTER_BILLS;
        }
        if (normalized.contains("CHORE") || normalized.contains("TASK")) {
            return ALERT_FILTER_CHORES;
        }
        if (normalized.contains("MAINTENANCE") || normalized.contains("TICKET")) {
            return ALERT_FILTER_MAINTENANCE;
        }
        if (normalized.contains("CALENDAR") || normalized.contains("EVENT") || normalized.contains("REMINDER")) {
            return ALERT_FILTER_CALENDAR;
        }
        if (normalized.contains("MESSAGE")) {
            return ALERT_FILTER_MESSAGES;
        }
        return normalized;
    }

    private HomeAlert cloneAlert(HomeAlert source) {
        HomeAlert clone = new HomeAlert();
        clone.setId(source.getId());
        clone.setTitle(source.getTitle());
        clone.setSubtitle(source.getSubtitle());
        clone.setType(source.getType());
        clone.setTargetScreen(source.getTargetScreen());
        clone.setEntityId(source.getEntityId());
        clone.setCreatedAt(source.getCreatedAt());
        clone.setEventTime(source.getEventTime());
        clone.setMetaOverride(source.getMetaOverride());
        clone.setDismissible(source.isDismissible());
        return clone;
    }

    private void addAlertIfMissing(LinkedHashMap<String, HomeAlert> feed, String key, HomeAlert alert) {
        if (feed == null || alert == null || isBlank(key) || feed.containsKey(key)) {
            return;
        }
        feed.put(key, alert);
    }

    private long notificationSortTime(HomeAlert alert) {
        return Math.max(alert != null ? alert.getCreatedAt() : 0L, alert != null ? alert.getEventTime() : 0L);
    }

    private String localAlertId(String deliveryKey) {
        return isBlank(deliveryKey) ? "" : "local:" + deliveryKey;
    }

    private String localKeyFromAlertId(String alertId) {
        return alertId != null && alertId.startsWith("local:")
                ? alertId.substring("local:".length())
                : alertId;
    }

    private boolean hasDeliveredAlert(String deliveryKey) {
        return !isBlank(deliveryKey) && LocalNotificationHelper.hasDeliveredNotification(this, deliveryKey);
    }

    private String deliveryKeyForBill(BillsRequest bill) {
        Date dueDate = parseBillDate(bill != null ? bill.getDueDate() : null);
        if (bill == null || dueDate == null || isBlank(bill.getId())) {
            return null;
        }
        return buildDeliveryKey("BILLS", bill.getId(), startOfDayMillis(dueDate));
    }

    private String deliveryKeyForCalendarBill(Calendar item) {
        Date dueDate = dateFromTimestamp(item != null ? item.getStartDate() : null);
        if (item == null || dueDate == null || isBlank(item.getId())) {
            return null;
        }
        long triggerAt = item.isAllDay() ? startOfDayMillis(dueDate) : dueDate.getTime();
        return buildDeliveryKey("BILLS", item.getId(), triggerAt);
    }

    private String deliveryKeyForChore(Calendar item) {
        Date dueDate = dateFromTimestamp(item != null ? item.getStartDate() : null);
        String entityId = item != null ? firstNonBlank(item.getRelatedChoreId(), item.getId()) : null;
        if (dueDate == null || isBlank(entityId)) {
            return null;
        }
        return buildDeliveryKey("CHORES", entityId, startOfDayMillis(dueDate));
    }

    private String deliveryKeyForEvent(Calendar item) {
        Date startDate = dateFromTimestamp(item != null ? item.getStartDate() : null);
        if (item == null || startDate == null || isBlank(item.getId())) {
            return null;
        }
        long triggerAt = item.isAllDay() ? startOfDayMillis(startDate) : startDate.getTime();
        return buildDeliveryKey("CALENDAR", item.getId(), triggerAt);
    }

    private String buildDeliveryKey(String targetScreen, String entityId, long triggerAtMillis) {
        return firstNonBlank(targetScreen, "HOME")
                + ":"
                + firstNonBlank(entityId, "unknown")
                + ":"
                + triggerAtMillis;
    }

    private String feedKeyForAlert(HomeAlert alert) {
        if (alert == null) {
            return "";
        }

        String type = normalizeAlertType(alert.getType());
        String target = safeTrim(alert.getTargetScreen()).toUpperCase(Locale.getDefault());
        String entityId = safeTrim(alert.getEntityId());

        if (!entityId.isEmpty()) {
            return type + ":" + target + ":" + entityId;
        }

        return rawAlertKey(alert, type);
    }

    private String rawAlertKey(HomeAlert alert, String normalizedType) {
        return normalizedType + ":raw:" + firstNonBlank(
                alert != null ? alert.getId() : null,
                alert != null ? alert.getEntityId() : null,
                safeTrim(alert != null ? alert.getTitle() : null) + ":" + (alert != null ? alert.getCreatedAt() : 0L)
        );
    }

    private String combineSentences(String first, String second) {
        String safeFirst = safeTrim(first);
        String safeSecond = safeTrim(second);
        if (safeFirst.isEmpty()) {
            return safeSecond;
        }
        if (safeSecond.isEmpty()) {
            return safeFirst;
        }
        return safeFirst + " " + safeSecond;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void updateAlertState() {
        int count = notificationList.size();
        if (count > 0) {
            tvAlertBadge.setVisibility(View.VISIBLE);
            tvAlertBadge.setText(count > 9 ? "9+" : String.valueOf(count));
            tvAlertSummary.setText(count == 1
                    ? "1 alert needs your attention today."
                    : count + " alerts need your attention today.");
        } else {
            tvAlertBadge.setVisibility(View.GONE);
            tvAlertSummary.setText("No new alerts right now.");
        }

        updateAlertButtonAccessibility(count);
        updateNotificationCenterState();
        toggleBellPulse(count > 0);
    }

    private void updateAlertButtonAccessibility(int count) {
        if (btnOpenNotifications == null) {
            return;
        }

        String description = count > 0
                ? String.format(Locale.getDefault(), "Open alerts. %d active alerts.", count)
                : "Open alerts. No active alerts.";
        btnOpenNotifications.setContentDescription(description);
    }

    private void toggleBellPulse(boolean active) {
        if (!active) {
            if (bellPulseAnimator != null) {
                bellPulseAnimator.cancel();
                bellPulseAnimator = null;
            }
            btnOpenNotifications.setScaleX(1f);
            btnOpenNotifications.setScaleY(1f);
            btnOpenNotifications.setAlpha(1f);
            return;
        }

        if (bellPulseAnimator != null && bellPulseAnimator.isRunning()) {
            return;
        }

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(btnOpenNotifications, View.SCALE_X, 1f, 1.06f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(btnOpenNotifications, View.SCALE_Y, 1f, 1.06f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(btnOpenNotifications, View.ALPHA, 1f, 0.9f, 1f);
        scaleX.setDuration(1800L);
        scaleY.setDuration(1800L);
        alpha.setDuration(1800L);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setInterpolator(new LinearInterpolator());
        scaleY.setInterpolator(new LinearInterpolator());
        alpha.setInterpolator(new LinearInterpolator());

        bellPulseAnimator = new AnimatorSet();
        bellPulseAnimator.playTogether(scaleX, scaleY, alpha);
        bellPulseAnimator.start();
    }

    private void showNotificationCenter() {
        if (notificationCenterDialog != null && notificationCenterDialog.isShowing()) {
            return;
        }

        activeAlertFilter = ALERT_FILTER_ALL;
        notificationCenterDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_notification_center, null);
        notificationCenterDialog.setContentView(sheetView);

        rvNotificationCenter = sheetView.findViewById(R.id.rvNotificationCenter);
        layoutCenterEmpty = sheetView.findViewById(R.id.layoutCenterEmpty);
        tvCenterEmpty = sheetView.findViewById(R.id.tvCenterEmpty);
        tvCenterEmptyTitle = sheetView.findViewById(R.id.tvCenterEmptyTitle);
        tvCenterAlertCount = sheetView.findViewById(R.id.tvCenterAlertCount);
        tvCenterCountChip = sheetView.findViewById(R.id.tvCenterCountChip);
        btnCenterClearAll = sheetView.findViewById(R.id.btnCenterClearAll);
        layoutCenterEmptyIconShell = sheetView.findViewById(R.id.layoutCenterEmptyIconShell);
        imgCenterEmptyIcon = sheetView.findViewById(R.id.imgCenterEmptyIcon);
        filterAlertsAll = sheetView.findViewById(R.id.filterAlertsAll);
        filterAlertsBills = sheetView.findViewById(R.id.filterAlertsBills);
        filterAlertsChores = sheetView.findViewById(R.id.filterAlertsChores);
        filterAlertsMaintenance = sheetView.findViewById(R.id.filterAlertsMaintenance);
        filterAlertsCalendar = sheetView.findViewById(R.id.filterAlertsCalendar);
        filterAlertsMessages = sheetView.findViewById(R.id.filterAlertsMessages);

        rvNotificationCenter.setLayoutManager(new LinearLayoutManager(this));
        rvNotificationCenter.setNestedScrollingEnabled(true);
        rvNotificationCenter.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        setupAlertFilterChips();
        notificationCenterAdapter = new HomeAlertAdapter(
                filteredNotificationList,
                this::deleteNotification,
                alert -> {
                    if (!isAlertNavigable(alert)) {
                        markNotificationAsRead(alert);
                        return;
                    }
                    if (notificationCenterDialog != null) {
                        notificationCenterDialog.dismiss();
                    }
                    openNotificationDestination(alert);
                }
        );
        rvNotificationCenter.setAdapter(notificationCenterAdapter);

        rebuildFilteredNotifications();
        btnCenterClearAll.setOnClickListener(v -> clearAllNotifications());
        updateNotificationCenterState();
        notificationCenterDialog.setOnShowListener(dialog -> configureNotificationCenterSheet());

        notificationCenterDialog.setOnDismissListener(dialog -> {
            notificationCenterAdapter = null;
            rvNotificationCenter = null;
            layoutCenterEmpty = null;
            tvCenterEmpty = null;
            tvCenterEmptyTitle = null;
            tvCenterAlertCount = null;
            tvCenterCountChip = null;
            btnCenterClearAll = null;
            layoutCenterEmptyIconShell = null;
            imgCenterEmptyIcon = null;
            filterAlertsAll = null;
            filterAlertsBills = null;
            filterAlertsChores = null;
            filterAlertsMaintenance = null;
            filterAlertsCalendar = null;
            filterAlertsMessages = null;
            notificationCenterDialog = null;
        });

        notificationCenterDialog.show();
    }

    private void configureNotificationCenterSheet() {
        if (notificationCenterDialog == null) {
            return;
        }

        FrameLayout bottomSheet = notificationCenterDialog.findViewById(
                com.google.android.material.R.id.design_bottom_sheet
        );
        if (bottomSheet == null) {
            return;
        }

        ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
        if (layoutParams != null) {
            layoutParams.height = Math.round(getResources().getDisplayMetrics().heightPixels * 0.88f);
            bottomSheet.setLayoutParams(layoutParams);
        }

        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setFitToContents(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void updateNotificationCenterState() {
        if (tvCenterAlertCount == null || tvCenterCountChip == null
                || tvCenterEmpty == null || tvCenterEmptyTitle == null
                || layoutCenterEmpty == null || rvNotificationCenter == null
                || btnCenterClearAll == null) {
            return;
        }

        int totalCount = notificationList.size();
        int filteredCount = filteredNotificationList.size();
        int dismissibleCount = countDismissibleAlerts();
        tvCenterAlertCount.setText(buildNotificationCenterSummary(totalCount, filteredCount));
        int chipCount = ALERT_FILTER_ALL.equals(activeAlertFilter) ? totalCount : filteredCount;
        tvCenterCountChip.setText(chipCount > 99 ? "99+" : String.valueOf(chipCount));
        layoutCenterEmpty.setVisibility(filteredCount == 0 ? View.VISIBLE : View.GONE);
        rvNotificationCenter.setVisibility(filteredCount == 0 ? View.GONE : View.VISIBLE);
        btnCenterClearAll.setVisibility(dismissibleCount == 0 ? View.GONE : View.VISIBLE);
        btnCenterClearAll.setContentDescription(dismissibleCount > 0
                ? String.format(Locale.getDefault(), "Clear all %d alerts", dismissibleCount)
                : "Clear all alerts");

        bindNotificationCenterEmptyState(totalCount, filteredCount);
    }

    private void deleteNotification(HomeAlert notification) {
        if (notification == null || !notification.isDismissible() || notification.getId() == null) {
            return;
        }

        if (isLocalAlert(notification)) {
            LocalNotificationHelper.clearDeliveredNotification(this, localKeyFromAlertId(notification.getId()));
            rebuildNotificationFeed();
            rebuildFilteredNotifications();
            updateAlertState();
            Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserId == null) {
            return;
        }

        firestore.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .document(notification.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    removeFirestoreNotification(notification.getId());
                    rebuildNotificationFeed();
                    rebuildFilteredNotifications();
                    updateAlertState();
                    Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete notification", Toast.LENGTH_SHORT).show());
    }

    private void clearAllNotifications() {
        List<HomeAlert> alertsToClear = getDismissibleAlertsForActiveFilter();
        if (alertsToClear.isEmpty()) {
            return;
        }

        List<String> firestoreIds = new ArrayList<>();
        for (HomeAlert alert : alertsToClear) {
            if (alert == null || alert.getId() == null) {
                continue;
            }
            if (isLocalAlert(alert)) {
                LocalNotificationHelper.clearDeliveredNotification(this, localKeyFromAlertId(alert.getId()));
            } else {
                firestoreIds.add(alert.getId());
            }
        }

        if (firestoreIds.isEmpty() || currentUserId == null) {
            if (currentUserId == null) {
                for (String notificationId : firestoreIds) {
                    removeFirestoreNotification(notificationId);
                }
            }
            rebuildNotificationFeed();
            rebuildFilteredNotifications();
            updateAlertState();
            Toast.makeText(this, buildClearNotificationsMessage(alertsToClear.size()), Toast.LENGTH_SHORT).show();
            return;
        }

        WriteBatch batch = firestore.batch();
        for (String notificationId : firestoreIds) {
            batch.delete(
                    firestore.collection("users")
                            .document(currentUserId)
                            .collection("notifications")
                            .document(notificationId)
            );
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    for (String notificationId : firestoreIds) {
                        removeFirestoreNotification(notificationId);
                    }
                    rebuildNotificationFeed();
                    rebuildFilteredNotifications();
                    updateAlertState();
                    Toast.makeText(this, buildClearNotificationsMessage(alertsToClear.size()), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to clear notifications", Toast.LENGTH_SHORT).show());
    }

    private void loadLeaderboardData() {
        ApiClient.getUserApi().getRoommates(houseCode).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, User> userMap = new HashMap<>();
                    for (User user : response.body()) {
                        userMap.put(user.getId(), user);
                    }

                    User me = new User();
                    me.setId(currentUserId);
                    me.setFirstName("You");
                    me.setProfileImageUrl(sessionManager.getUserImage());
                    userMap.put(currentUserId, me);

                    fetchTasksAndBuildLeaderboard(userMap);
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void fetchTasksAndBuildLeaderboard(Map<String, User> userMap) {
        ApiClient.getCalendarApi().getByApartment(houseCode).enqueue(new Callback<List<Calendar>>() {
            @Override
            public void onResponse(Call<List<Calendar>> call, Response<List<Calendar>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    calculatePointsAndDisplay(response.body(), userMap);
                }
            }

            @Override
            public void onFailure(Call<List<Calendar>> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void calculatePointsAndDisplay(List<Calendar> tasks, Map<String, User> userMap) {
        Map<String, Integer> pointsMap = new HashMap<>();

        for (Calendar item : tasks) {
            if ("CHORE".equalsIgnoreCase(item.getType()) && "COMPLETED".equalsIgnoreCase(item.getStatus())) {
                String uid = item.getAssignedTo();
                if (uid != null) {
                    int taskPoints = calculateTaskPoints(item);
                    pointsMap.put(uid, pointsMap.getOrDefault(uid, 0) + taskPoints);
                }
            }
        }

        if (pointsMap.isEmpty()) {
            cardLeaderboard.setVisibility(View.GONE);
            return;
        }

        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(pointsMap.entrySet());
        Collections.sort(sortedEntries, (a, b) -> b.getValue().compareTo(a.getValue()));

        int myRank = 0;
        int myPoints = 0;
        for (int i = 0; i < sortedEntries.size(); i++) {
            if (sortedEntries.get(i).getKey().equals(currentUserId)) {
                myRank = i + 1;
                myPoints = sortedEntries.get(i).getValue();
                break;
            }
        }

        tvYourRank.setText(myRank > 0 ? rankLabel(myRank) : "-");
        tvYourPoints.setText(myPoints + " pts");
        styleLeaderboardEntry(layoutYourStanding, tvYourRank, ivYourLeaderBadge, myRank, true);

        ImageView ivYourProfile = findViewById(R.id.ivYourProfile);
        User meUser = userMap.get(currentUserId);
        if (meUser != null) {
            ImageUtils.loadProfileImage(ivYourProfile, meUser.getProfileImageUrl());
        }

        layoutLeaderboardRows.removeAllViews();
        for (int i = 0; i < sortedEntries.size(); i++) {
            String uid = sortedEntries.get(i).getKey();
            if (uid.equals(currentUserId)) {
                continue;
            }

            int rank = i + 1;
            int points = sortedEntries.get(i).getValue();
            User user = userMap.get(uid);

            View row = getLayoutInflater().inflate(R.layout.item_leaderboard_row, layoutLeaderboardRows, false);
            View rowContainer = row.findViewById(R.id.rowContainer);
            TextView rowRank = row.findViewById(R.id.tvRank);
            ImageView rowProfile = row.findViewById(R.id.ivProfile);
            TextView rowName = row.findViewById(R.id.tvName);
            TextView rowPoints = row.findViewById(R.id.tvPoints);
            ImageView rowBadge = row.findViewById(R.id.ivLeaderBadge);

            rowRank.setText(rankLabel(rank));
            rowName.setText(user != null ? user.getFirstName() : "Roommate");
            rowPoints.setText(points + " pts");
            styleLeaderboardEntry(rowContainer, rowRank, rowBadge, rank, false);

            if (user != null) {
                ImageUtils.loadProfileImage(rowProfile, user.getProfileImageUrl());
            }

            layoutLeaderboardRows.addView(row);
        }

        boolean wasHidden = cardLeaderboard.getVisibility() != View.VISIBLE;
        cardLeaderboard.setVisibility(View.VISIBLE);
        if (wasHidden) {
            animateLeaderboardReveal();
        }
    }

    private void styleLeaderboardEntry(View container, TextView rankView, ImageView badgeView, int rank, boolean isPinnedCard) {
        if (rank == 1) {
            rankView.setBackgroundResource(R.drawable.bg_status_progress);
            rankView.setTextColor(ContextCompat.getColor(this, R.color.app_warning));
            badgeView.setVisibility(View.VISIBLE);
            container.setBackgroundResource(R.drawable.bg_home_leaderboard_champion);
        } else if (rank == 2) {
            rankView.setBackgroundResource(R.drawable.bg_status_pending);
            rankView.setTextColor(ContextCompat.getColor(this, R.color.app_accent_pink));
            badgeView.setVisibility(View.GONE);
            container.setBackgroundResource(isPinnedCard ? R.drawable.bg_leaderboard_you : 0);
        } else if (rank == 3) {
            rankView.setBackgroundResource(R.drawable.bg_status_completed);
            rankView.setTextColor(ContextCompat.getColor(this, R.color.app_accent_green));
            badgeView.setVisibility(View.GONE);
            container.setBackgroundResource(isPinnedCard ? R.drawable.bg_leaderboard_you : 0);
        } else {
            rankView.setBackgroundResource(R.drawable.bg_soft_badge);
            rankView.setTextColor(ContextCompat.getColor(this, R.color.app_text_secondary));
            badgeView.setVisibility(View.GONE);
            container.setBackgroundResource(isPinnedCard ? R.drawable.bg_leaderboard_you : 0);
        }
    }

    private void animateLeaderboardReveal() {
        cardLeaderboard.setAlpha(0f);
        cardLeaderboard.setTranslationY(dp(18));
        cardLeaderboard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(420)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private int calculateTaskPoints(Calendar item) {
        int difficulty = Math.max(item.getDifficultyScore(), 1);
        int estimatedTime = Math.max(item.getEstDuration(), 10);
        return difficulty * estimatedTime;
    }

    private String rankLabel(int rank) {
        return "#" + rank;
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private void setupQuickActions() {
        findViewById(R.id.btnOpenBills).setOnClickListener(v ->
                startActivity(new Intent(this, TenantBillsActivity.class)));
        findViewById(R.id.btnOpenCalendar).setOnClickListener(v ->
                startActivity(new Intent(this, TenantCalendarActivity.class)));
        findViewById(R.id.btnOpenTickets).setOnClickListener(v ->
                startActivity(new Intent(this, TenantTicketsActivity.class)));
    }

    private String resolveAlertType(String type, String targetScreen, String title, String body) {
        if (type != null && !type.trim().isEmpty()) {
            return type.trim().toUpperCase(Locale.getDefault());
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

    private void setupAlertFilterChips() {
        setupAlertFilterChip(filterAlertsAll, ALERT_FILTER_ALL);
        setupAlertFilterChip(filterAlertsMessages, ALERT_FILTER_MESSAGES);
        setupAlertFilterChip(filterAlertsBills, ALERT_FILTER_BILLS);
        setupAlertFilterChip(filterAlertsChores, ALERT_FILTER_CHORES);
        setupAlertFilterChip(filterAlertsMaintenance, ALERT_FILTER_MAINTENANCE);
        setupAlertFilterChip(filterAlertsCalendar, ALERT_FILTER_CALENDAR);
        updateAlertFilterChipStyles();
    }

    private void setupAlertFilterChip(TextView chip, String filter) {
        if (chip == null) {
            return;
        }

        chip.setOnClickListener(v -> {
            if (filter.equals(activeAlertFilter)) {
                return;
            }

            activeAlertFilter = filter;
            rebuildFilteredNotifications();
            updateAlertFilterChipStyles();
            updateNotificationCenterState();
        });
    }

    private void rebuildFilteredNotifications() {
        filteredNotificationList.clear();
        for (HomeAlert alert : notificationList) {
            if (matchesActiveAlertFilter(alert)) {
                filteredNotificationList.add(alert);
            }
        }

        if (notificationCenterAdapter != null) {
            notificationCenterAdapter.notifyDataSetChanged();
        }
    }

    private boolean matchesActiveAlertFilter(HomeAlert alert) {
        if (ALERT_FILTER_ALL.equals(activeAlertFilter)) {
            return true;
        }

        String type = normalizeAlertType(alert != null ? alert.getType() : null);
        return activeAlertFilter.equals(type);
    }

    private void updateAlertFilterChipStyles() {
        styleAlertFilterChip(filterAlertsAll, ALERT_FILTER_ALL);
        styleAlertFilterChip(filterAlertsMessages, ALERT_FILTER_MESSAGES);
        styleAlertFilterChip(filterAlertsBills, ALERT_FILTER_BILLS);
        styleAlertFilterChip(filterAlertsChores, ALERT_FILTER_CHORES);
        styleAlertFilterChip(filterAlertsMaintenance, ALERT_FILTER_MAINTENANCE);
        styleAlertFilterChip(filterAlertsCalendar, ALERT_FILTER_CALENDAR);
    }

    private void styleAlertFilterChip(TextView chip, String filter) {
        if (chip == null) {
            return;
        }

        boolean selected = filter.equals(activeAlertFilter);
        int accentColor = accentColorForFilter(filter);
        int textColor = textColorForFilter(filter);
        boolean isAllFilter = ALERT_FILTER_ALL.equals(filter);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(18));
        drawable.setStroke(
                dp(1),
                selected
                        ? withAlpha(accentColor, 0.34f)
                        : (isAllFilter ? getColor(R.color.calendar_stroke) : withAlpha(accentColor, 0.2f))
        );
        drawable.setColor(
                selected
                        ? withAlpha(accentColor, 0.16f)
                        : getColor(R.color.app_surface)
        );
        chip.setBackground(drawable);
        chip.setTextColor(selected || !isAllFilter ? textColor : getColor(R.color.calendar_text_primary));
        chip.setContentDescription((selected ? "Selected filter: " : "Filter: ") + chip.getText());
    }

    private String buildNotificationCenterSummary(int totalCount, int filteredCount) {
        if (totalCount == 0) {
            return "0 active alerts";
        }

        if (ALERT_FILTER_ALL.equals(activeAlertFilter)) {
            return totalCount == 1 ? "1 active alert" : totalCount + " active alerts";
        }

        String label = filterDisplayLabel(activeAlertFilter).toLowerCase(Locale.getDefault());
        if (filteredCount == 0) {
            return "0 " + label + " alerts";
        }

        if (filteredCount == totalCount) {
            return filteredCount == 1
                    ? "1 " + label + " alert"
                    : filteredCount + " " + label + " alerts";
        }

        return filteredCount == 1
                ? "1 " + label + " alert of " + totalCount
                : filteredCount + " " + label + " alerts of " + totalCount;
    }

    private void bindNotificationCenterEmptyState(int totalCount, int filteredCount) {
        int accentColor = accentColorForFilter(activeAlertFilter);
        int iconRes = iconForFilter(activeAlertFilter);

        if (layoutCenterEmptyIconShell != null) {
            GradientDrawable shell = new GradientDrawable();
            shell.setCornerRadius(dp(24));
            shell.setStroke(dp(1), withAlpha(accentColor, 0.26f));
            shell.setColor(withAlpha(accentColor, 0.12f));
            layoutCenterEmptyIconShell.setBackground(shell);
        }

        if (imgCenterEmptyIcon != null) {
            imgCenterEmptyIcon.setImageResource(iconRes);
            imgCenterEmptyIcon.setColorFilter(accentColor);
        }

        if (totalCount == 0) {
            tvCenterEmptyTitle.setText("All caught up");
            tvCenterEmpty.setText("Fresh apartment updates will land here as soon as something needs your attention.");
            return;
        }

        if (filteredCount == 0) {
            tvCenterEmptyTitle.setText("No " + filterDisplayLabel(activeAlertFilter).toLowerCase(Locale.getDefault()) + " alerts");
            tvCenterEmpty.setText("Try a different filter or check back later for new updates in this category.");
            return;
        }

        tvCenterEmptyTitle.setText("");
        tvCenterEmpty.setText("");
    }

    private String filterDisplayLabel(String filter) {
        switch (filter) {
            case ALERT_FILTER_BILLS:
                return "Bills";
            case ALERT_FILTER_CHORES:
                return "Chores";
            case ALERT_FILTER_MAINTENANCE:
                return "Maintenance";
            case ALERT_FILTER_CALENDAR:
                return "Events";
            case ALERT_FILTER_MESSAGES:
                return "Messages";
            case ALERT_FILTER_ALL:
            default:
                return "All";
        }
    }

    private int accentColorForFilter(String filter) {
        switch (filter) {
            case ALERT_FILTER_BILLS:
                return getColor(R.color.calendar_bill_border);
            case ALERT_FILTER_CHORES:
                return getColor(R.color.calendar_chore_border);
            case ALERT_FILTER_MAINTENANCE:
                return getColor(R.color.alert_maintenance_accent);
            case ALERT_FILTER_CALENDAR:
                return getColor(R.color.calendar_event_border);
            case ALERT_FILTER_MESSAGES:
                return getColor(R.color.alert_message_accent);
            case ALERT_FILTER_ALL:
            default:
                return getColor(R.color.calendar_primary_dark);
        }
    }

    private int textColorForFilter(String filter) {
        switch (filter) {
            case ALERT_FILTER_BILLS:
                return getColor(R.color.calendar_bill_text);
            case ALERT_FILTER_CHORES:
                return getColor(R.color.calendar_chore_text);
            case ALERT_FILTER_MAINTENANCE:
                return getColor(R.color.alert_maintenance_accent);
            case ALERT_FILTER_CALENDAR:
                return getColor(R.color.calendar_event_text);
            case ALERT_FILTER_MESSAGES:
                return getColor(R.color.alert_message_accent);
            case ALERT_FILTER_ALL:
            default:
                return getColor(R.color.calendar_primary_dark);
        }
    }

    private int iconForFilter(String filter) {
        switch (filter) {
            case ALERT_FILTER_BILLS:
                return R.drawable.ic_alert_bills;
            case ALERT_FILTER_CHORES:
                return R.drawable.ic_alert_chore;
            case ALERT_FILTER_MAINTENANCE:
                return R.drawable.ic_alert_maintenance;
            case ALERT_FILTER_CALENDAR:
                return R.drawable.ic_alert_calendar;
            case ALERT_FILTER_MESSAGES:
                return R.drawable.ic_alert_message;
            case ALERT_FILTER_ALL:
            default:
                return R.drawable.ic_notifications;
        }
    }

    private int countDismissibleAlerts() {
        int count = 0;
        for (HomeAlert alert : filteredNotificationList) {
            if (alert != null && alert.isDismissible()) {
                count++;
            }
        }
        return count;
    }

    private List<HomeAlert> getDismissibleAlertsForActiveFilter() {
        List<HomeAlert> alerts = new ArrayList<>();
        for (HomeAlert alert : filteredNotificationList) {
            if (alert != null && alert.isDismissible() && alert.getId() != null) {
                alerts.add(alert);
            }
        }
        return alerts;
    }

    private String buildClearNotificationsMessage(int clearedCount) {
        if (ALERT_FILTER_ALL.equals(activeAlertFilter)) {
            return clearedCount == 1
                    ? "1 notification cleared"
                    : clearedCount + " notifications cleared";
        }

        String pluralLabel = filterDisplayLabel(activeAlertFilter).toLowerCase(Locale.getDefault());
        String singularLabel = singularFilterLabel(activeAlertFilter);
        return clearedCount == 1
                ? "1 " + singularLabel + " alert cleared"
                : clearedCount + " " + pluralLabel + " alerts cleared";
    }

    private String singularFilterLabel(String filter) {
        switch (filter) {
            case ALERT_FILTER_BILLS:
                return "bill";
            case ALERT_FILTER_CHORES:
                return "chore";
            case ALERT_FILTER_MAINTENANCE:
                return "maintenance";
            case ALERT_FILTER_CALENDAR:
                return "event";
            case ALERT_FILTER_MESSAGES:
                return "message";
            case ALERT_FILTER_ALL:
            default:
                return "notification";
        }
    }

    private void removeFirestoreNotification(String notificationId) {
        if (isBlank(notificationId)) {
            return;
        }

        for (int i = firestoreNotificationList.size() - 1; i >= 0; i--) {
            HomeAlert alert = firestoreNotificationList.get(i);
            if (alert != null && notificationId.equals(alert.getId())) {
                firestoreNotificationList.remove(i);
            }
        }
    }

    private int withAlpha(int color, float alpha) {
        int alphaChannel = Math.round(255 * alpha);
        return (color & 0x00FFFFFF) | (alphaChannel << 24);
    }

    private void openNotificationDestination(HomeAlert alert) {
        if (alert == null) {
            return;
        }

        if (!isAlertNavigable(alert)) {
            markNotificationAsRead(alert);
            return;
        }

        String target = alert.getTargetScreen() != null
                ? alert.getTargetScreen().trim().toUpperCase(Locale.getDefault())
                : "HOME";

        Intent intent;

        switch (target) {
            case "BILLS":
                intent = new Intent(this, TenantBillsActivity.class);
                intent.putExtra("highlight_bill_id", alert.getEntityId());
                intent.putExtra(LocalNotificationHelper.EXTRA_HIGHLIGHT_MESSAGE, "View bills here");
                break;
            case "CHORES":
                intent = buildChoreAlertIntent(alert);
                if (intent == null) {
                    intent = new Intent(this, ViewAllTasksActivity.class);
                    intent.putExtra("highlight_task_id", alert.getEntityId());
                }
                break;
            case "CALENDAR":
                intent = new Intent(this, TenantCalendarActivity.class);
                intent.putExtra("highlight_event_id", alert.getEntityId());
                intent.putExtra(LocalNotificationHelper.EXTRA_HIGHLIGHT_MESSAGE, "View events here");
                break;
            case "TICKETS":
                intent = new Intent(this, TenantTicketsActivity.class);
                intent.putExtra("highlight_ticket_id", alert.getEntityId());
                intent.putExtra(LocalNotificationHelper.EXTRA_HIGHLIGHT_MESSAGE, "View maintenance here");
                break;
            case "MESSAGE":
            case "HOME":
            default:
                markNotificationAsRead(alert);
                return;
        }

        markNotificationAsRead(alert);
        startActivity(intent);
    }

    private Intent buildChoreAlertIntent(HomeAlert alert) {
        if (alert == null || isBlank(alert.getEntityId())) {
            return null;
        }

        Calendar matchedChore = null;
        for (Calendar item : alertCalendarItems) {
            if (item == null || item.getType() == null || !item.getType().toUpperCase(Locale.getDefault()).contains("CHORE")) {
                continue;
            }

            if (alert.getEntityId().equals(item.getRelatedChoreId()) || alert.getEntityId().equals(item.getId())) {
                matchedChore = item;
                break;
            }
        }

        if (matchedChore == null) {
            return null;
        }

        Intent intent = new Intent(this, ChoreDetailActivity.class);
        intent.putExtra("CHORE_ID", firstNonBlank(matchedChore.getRelatedChoreId(), matchedChore.getId()));
        intent.putExtra("HOUSE_CODE", houseCode);
        intent.putExtra("TITLE", matchedChore.getTitle());
        intent.putExtra("DESCRIPTION", matchedChore.getDescription());
        intent.putExtra("EST_DURATION", matchedChore.getEstDuration());
        intent.putExtra("CURRENT_STATUS", matchedChore.getStatus());
        intent.putExtra("LOCATION", matchedChore.getLocation());
        intent.putExtra("ASSIGNED_TO_NAME", currentUserId != null && currentUserId.equals(matchedChore.getAssignedTo()) ? "Me" : "Roommate");
        intent.putExtra("CREATED_BY_NAME", currentUserId != null && currentUserId.equals(matchedChore.getCreatedBy()) ? "Me" : "Roommate");

        if (matchedChore.getStartDate() != null) {
            intent.putExtra(
                    "DUE_DATE",
                    new SimpleDateFormat("MMM dd, yyyy", Locale.US).format(matchedChore.getStartDate().toDate())
            );
        }

        return intent;
    }

    private void markNotificationAsRead(HomeAlert alert) {
        if (alert == null || alert.getId() == null || isLocalAlert(alert) || currentUserId == null) {
            return;
        }

        firestore.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .document(alert.getId())
                .update("read", true);
    }

    private boolean isAlertNavigable(HomeAlert alert) {
        return alert != null && !ALERT_FILTER_MESSAGES.equals(normalizeAlertType(alert.getType()));
    }

    private boolean isLocalAlert(HomeAlert alert) {
        return alert != null && alert.getId() != null && alert.getId().startsWith("local:");
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            }

            if (id == R.id.nav_bills) {
                startActivity(new Intent(this, TenantBillsActivity.class));
            } else if (id == R.id.nav_calendar) {
                startActivity(new Intent(this, TenantCalendarActivity.class));
            } else if (id == R.id.nav_tickets) {
                startActivity(new Intent(this, TenantTicketsActivity.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, TenantProfileActivity.class));
            }

            overridePendingTransition(0, 0);
            finish();
            return true;
        });
    }
}
