package com.example.uninest.ui.auth;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
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
import com.example.uninest.model.Calendar;
import com.example.uninest.model.HomeAlert;
import com.example.uninest.model.User;
import com.example.uninest.utils.ContactUtils;
import com.example.uninest.utils.ImageUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

    private RecyclerView recyclerNotifications;
    private Button btnClearAllNotifications;
    private Button btnEmergencyContact;
    private Button btnSupport;
    private TextView tvNotificationsEmpty;

    private View heroCard;
    private View heroOrbLarge;
    private View heroOrbSmall;
    private View chipLiveState;
    private View layoutSupportActions;
    private View layoutQuickAccess;

    private HomeAlertAdapter homeAlertAdapter;
    private final List<HomeAlert> notificationList = new ArrayList<>();

    private FirebaseFirestore firestore;

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
        setupNotificationsList();
        setupQuickActions();
        setupHomeAnimations();

        btnClearAllNotifications.setOnClickListener(v -> clearAllNotifications());
        btnEmergencyContact.setOnClickListener(v -> ContactUtils.dialEmergency(this));
        btnSupport.setOnClickListener(v -> ContactUtils.emailSupport(this));

        initHeader();

        if (houseCode != null) {
            loadLeaderboardData();
        }

        loadNotifications();
        setupBottomNav();

        if (getIntent() != null && getIntent().getBooleanExtra("open_notifications", false)) {
            recyclerNotifications.post(() -> recyclerNotifications.smoothScrollToPosition(0));
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
        chipLiveState = findViewById(R.id.chipLiveState);
        layoutSupportActions = findViewById(R.id.layoutSupportActions);
        layoutQuickAccess = findViewById(R.id.layoutQuickAccess);

        cardLeaderboard = findViewById(R.id.cardLeaderboard);
        layoutLeaderboardRows = findViewById(R.id.layoutLeaderboardRows);
        layoutYourStanding = findViewById(R.id.layoutYourStanding);
        tvYourRank = findViewById(R.id.tvYourRank);
        tvYourPoints = findViewById(R.id.tvYourPoints);
        ivYourLeaderBadge = findViewById(R.id.ivYourLeaderBadge);

        recyclerNotifications = findViewById(R.id.recyclerNotifications);
        btnClearAllNotifications = findViewById(R.id.btnClearAllNotifications);
        btnEmergencyContact = findViewById(R.id.btnEmergencyContact);
        btnSupport = findViewById(R.id.btnSupport);
        tvNotificationsEmpty = findViewById(R.id.tvNotificationsEmpty);
    }

    private void setupNotificationsList() {
        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        homeAlertAdapter = new HomeAlertAdapter(
                notificationList,
                this::deleteNotification,
                this::openNotificationDestination
        );
        recyclerNotifications.setAdapter(homeAlertAdapter);
        updateNotificationState();
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
        startPulseAnimation(chipLiveState);
    }

    private void startFloatingAnimation(View target, float travel, long duration, long startDelay) {
        ObjectAnimator translateY = ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, 0f, travel, 0f);
        translateY.setDuration(duration);
        translateY.setRepeatCount(ObjectAnimator.INFINITE);
        translateY.setRepeatMode(ObjectAnimator.RESTART);
        translateY.setStartDelay(startDelay);
        translateY.setInterpolator(new LinearInterpolator());

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 1.06f, 1f);
        scaleX.setDuration(duration);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setRepeatMode(ObjectAnimator.RESTART);
        scaleX.setStartDelay(startDelay);
        scaleX.setInterpolator(new LinearInterpolator());

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 1.06f, 1f);
        scaleY.setDuration(duration);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatMode(ObjectAnimator.RESTART);
        scaleY.setStartDelay(startDelay);
        scaleY.setInterpolator(new LinearInterpolator());

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(translateY, scaleX, scaleY);
        animatorSet.start();
    }

    private void startPulseAnimation(View target) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 1.03f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 1.03f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(target, View.ALPHA, 1f, 0.88f, 1f);

        scaleX.setDuration(2200L);
        scaleY.setDuration(2200L);
        alpha.setDuration(2200L);

        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        alpha.setRepeatCount(ObjectAnimator.INFINITE);

        scaleX.setInterpolator(new LinearInterpolator());
        scaleY.setInterpolator(new LinearInterpolator());
        alpha.setInterpolator(new LinearInterpolator());

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, alpha);
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

        firestore.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    notificationList.clear();

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
                        notificationList.add(alert);
                    }

                    homeAlertAdapter.notifyDataSetChanged();
                    updateNotificationState();
                });
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

    private void deleteNotification(HomeAlert notification) {
        if (currentUserId == null || notification.getId() == null) {
            return;
        }

        firestore.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .document(notification.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    notificationList.remove(notification);
                    homeAlertAdapter.notifyDataSetChanged();
                    updateNotificationState();
                    Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete notification", Toast.LENGTH_SHORT).show());
    }

    private void clearAllNotifications() {
        if (currentUserId == null) {
            return;
        }

        firestore.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }

                    notificationList.clear();
                    homeAlertAdapter.notifyDataSetChanged();
                    updateNotificationState();
                    Toast.makeText(this, "All notifications cleared", Toast.LENGTH_SHORT).show();
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
            if (isPinnedCard) {
                container.setBackgroundResource(R.drawable.bg_leaderboard_you);
            } else {
                container.setBackgroundResource(0);
            }
        } else if (rank == 3) {
            rankView.setBackgroundResource(R.drawable.bg_status_completed);
            rankView.setTextColor(ContextCompat.getColor(this, R.color.app_accent_green));
            badgeView.setVisibility(View.GONE);
            if (isPinnedCard) {
                container.setBackgroundResource(R.drawable.bg_leaderboard_you);
            } else {
                container.setBackgroundResource(0);
            }
        } else {
            rankView.setBackgroundResource(R.drawable.bg_soft_badge);
            rankView.setTextColor(ContextCompat.getColor(this, R.color.app_text_secondary));
            badgeView.setVisibility(View.GONE);
            if (isPinnedCard) {
                container.setBackgroundResource(R.drawable.bg_leaderboard_you);
            } else {
                container.setBackgroundResource(0);
            }
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

    private void updateNotificationState() {
        boolean empty = notificationList.isEmpty();
        recyclerNotifications.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvNotificationsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        btnClearAllNotifications.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private String resolveAlertType(String type, String targetScreen, String title, String body) {
        if (type != null && !type.trim().isEmpty()) {
            return type.trim().toUpperCase(Locale.getDefault());
        }

        String haystack = ((targetScreen != null ? targetScreen : "") + " "
                + (title != null ? title : "") + " "
                + (body != null ? body : "")).toLowerCase(Locale.getDefault());

        if (haystack.contains("ticket") || haystack.contains("maintenance") || haystack.contains("agent")) {
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

    private void openNotificationDestination(HomeAlert alert) {
        if (alert == null) {
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
                break;
            case "CHORES":
                intent = new Intent(this, ViewAllTasksActivity.class);
                intent.putExtra("highlight_task_id", alert.getEntityId());
                break;
            case "CALENDAR":
                intent = new Intent(this, TenantCalendarActivity.class);
                intent.putExtra("highlight_event_id", alert.getEntityId());
                break;
            case "TICKETS":
                intent = new Intent(this, TenantTicketsActivity.class);
                intent.putExtra("highlight_ticket_id", alert.getEntityId());
                break;
            case "HOME":
            default:
                markNotificationAsRead(alert);
                Toast.makeText(this, "Opened notification", Toast.LENGTH_SHORT).show();
                return;
        }

        markNotificationAsRead(alert);
        startActivity(intent);
    }

    private void markNotificationAsRead(HomeAlert alert) {
        if (currentUserId == null || alert == null || alert.getId() == null) {
            return;
        }

        firestore.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .document(alert.getId())
                .update("read", true);
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
