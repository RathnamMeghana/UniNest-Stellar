package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.Calendar;
import com.example.uninest.model.HomeAlert;
import com.example.uninest.model.User;
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
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TenantHomeActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private String houseCode, currentUserId;

    private androidx.cardview.widget.CardView cardLeaderboard;
    private TextView tvYourRank, tvYourPoints;
    private LinearLayout layoutLeaderboardRows;

    private RecyclerView recyclerNotifications;
    private Button btnClearAllNotifications;

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

        recyclerNotifications = findViewById(R.id.recyclerNotifications);
        btnClearAllNotifications = findViewById(R.id.btnClearAllNotifications);

        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        homeAlertAdapter = new HomeAlertAdapter(
                notificationList,
                this::deleteNotification,
                this::openNotificationDestination
        );
        recyclerNotifications.setAdapter(homeAlertAdapter);

        btnClearAllNotifications.setOnClickListener(v -> clearAllNotifications());

        initHeader();
        initLeaderboardViews();

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

    private void initHeader() {
        TextView tvGreeting = findViewById(R.id.tvGreeting);
        String fullName = sessionManager.getUserFullName();
        String firstName = (fullName != null && fullName.contains(" "))
                ? fullName.split(" ")[0]
                : (fullName != null ? fullName : "User");

        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String timeGreeting = (hour < 12) ? "Good Morning" : (hour < 17) ? "Good Afternoon" : "Good Evening";
        tvGreeting.setText(timeGreeting + ",\n" + firstName + "!");
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

    private void initLeaderboardViews() {
        cardLeaderboard = findViewById(R.id.cardLeaderboard);
        tvYourRank = findViewById(R.id.tvYourRank);
        tvYourPoints = findViewById(R.id.tvYourPoints);
        layoutLeaderboardRows = findViewById(R.id.layoutLeaderboardRows);
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

                        String type = doc.getString("type");
                        if (type == null || type.trim().isEmpty()) {
                            type = "MESSAGE";
                        }
                        alert.setType(type);
                        alert.setTargetScreen(doc.getString("targetScreen"));
                        alert.setEntityId(doc.getString("entityId"));

                        Object createdAtObj = doc.get("createdAt");
                        long createdAt = 0L;

                        if (createdAtObj instanceof Long) {
                            createdAt = (Long) createdAtObj;
                        } else if (createdAtObj instanceof Double) {
                            createdAt = ((Double) createdAtObj).longValue();
                        } else if (createdAtObj instanceof com.google.firebase.Timestamp) {
                            createdAt = ((com.google.firebase.Timestamp) createdAtObj).toDate().getTime();
                        } else if (createdAtObj instanceof String) {
                            try {
                                createdAt = Long.parseLong((String) createdAtObj);
                            } catch (Exception ignored) {
                                createdAt = 0L;
                            }
                        }

                        alert.setCreatedAt(createdAt);

                        Object eventTimeObj = doc.get("eventTime");
                        long eventTime = 0L;

                        if (eventTimeObj instanceof Long) {
                            eventTime = (Long) eventTimeObj;
                        } else if (eventTimeObj instanceof Double) {
                            eventTime = ((Double) eventTimeObj).longValue();
                        } else if (eventTimeObj instanceof com.google.firebase.Timestamp) {
                            eventTime = ((com.google.firebase.Timestamp) eventTimeObj).toDate().getTime();
                        } else if (eventTimeObj instanceof String) {
                            try {
                                eventTime = Long.parseLong((String) eventTimeObj);
                            } catch (Exception ignored) {
                                eventTime = 0L;
                            }
                        }

                        alert.setEventTime(eventTime);
                        notificationList.add(alert);
                    }

                    homeAlertAdapter.notifyDataSetChanged();
                });
    }

    private void deleteNotification(HomeAlert notification) {
        if (currentUserId == null || notification.getId() == null) return;

        firestore.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .document(notification.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    notificationList.remove(notification);
                    homeAlertAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete notification", Toast.LENGTH_SHORT).show()
                );
    }

    private void clearAllNotifications() {
        if (currentUserId == null) return;

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
                    Toast.makeText(this, "All notifications cleared", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to clear notifications", Toast.LENGTH_SHORT).show()
                );
    }

    private void loadLeaderboardData() {
        ApiClient.getUserApi().getRoommates(houseCode).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, User> userMap = new HashMap<>();
                    for (User u : response.body()) {
                        userMap.put(u.getId(), u);
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

        for (Calendar c : tasks) {
            if ("CHORE".equalsIgnoreCase(c.getType()) && "COMPLETED".equalsIgnoreCase(c.getStatus())) {
                String uid = c.getAssignedTo();
                if (uid != null) {
                    int taskPoints = calculateTaskPoints(c);
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

        String myMedal = (myRank == 1) ? "🥇" : (myRank == 2) ? "🥈" : (myRank == 3) ? "🥉" : "#" + myRank;
        tvYourRank.setText(myRank > 0 ? myMedal : "-");
        tvYourPoints.setText(myPoints + " pts");

        ImageView ivYourProfile = findViewById(R.id.ivYourProfile);
        User meUser = userMap.get(currentUserId);
        if (meUser != null) {
            ImageUtils.loadProfileImage(ivYourProfile, meUser.getProfileImageUrl());
        }

        layoutLeaderboardRows.removeAllViews();
        for (int i = 0; i < sortedEntries.size(); i++) {
            String uid = sortedEntries.get(i).getKey();
            if (uid.equals(currentUserId)) continue;

            int rank = i + 1;
            int pts = sortedEntries.get(i).getValue();
            User user = userMap.get(uid);

            View row = getLayoutInflater().inflate(R.layout.item_leaderboard_row, layoutLeaderboardRows, false);
            TextView rowRank = row.findViewById(R.id.tvRank);
            ImageView rowProfile = row.findViewById(R.id.ivProfile);
            TextView rowName = row.findViewById(R.id.tvName);
            TextView rowPts = row.findViewById(R.id.tvPoints);

            String medal = (rank == 1) ? "🥇" : (rank == 2) ? "🥈" : (rank == 3) ? "🥉" : "#" + rank;
            rowRank.setText(medal);
            rowName.setText(user != null ? user.getFirstName() : "Roommate");
            rowPts.setText(pts + " pts");

            if (user != null) {
                ImageUtils.loadProfileImage(rowProfile, user.getProfileImageUrl());
            }

            layoutLeaderboardRows.addView(row);
        }

        cardLeaderboard.setVisibility(View.VISIBLE);
    }

    private int calculateTaskPoints(Calendar c) {
        int diff = Math.max(c.getDifficultyScore(), 1);
        int estTime = Math.max(c.getEstDuration(), 10);
        return diff * estTime;
    }

    private void openNotificationDestination(HomeAlert alert) {
        if (alert == null) return;

        String target = alert.getTargetScreen() != null
                ? alert.getTargetScreen().trim().toUpperCase()
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
            if (id == R.id.nav_home) return true;

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