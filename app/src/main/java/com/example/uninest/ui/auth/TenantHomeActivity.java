package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.utils.ImageUtils;
import com.example.uninest.model.User;
import com.example.uninest.model.Calendar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TenantHomeActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private String houseCode, currentUserId;

    // Leaderboard views
    private androidx.cardview.widget.CardView cardLeaderboard;
    private TextView tvYourRank, tvYourPoints;
    private LinearLayout layoutLeaderboardRows;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_home);

        sessionManager = new SessionManager(this);
        houseCode = sessionManager.fetchHouseCode();
        currentUserId = sessionManager.getUserId();

        initHeader();
        initLeaderboardViews();

        if (houseCode != null) {
            loadLeaderboardData();
        }

        setupBottomNav();
    }

    private void initHeader() {
        TextView tvGreeting = findViewById(R.id.tvGreeting);
        String fullName = sessionManager.getUserFullName();
        String firstName = (fullName != null && fullName.contains(" "))
                ? fullName.split(" ")[0] : (fullName != null ? fullName : "User");


        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String timeGreeting = (hour < 12) ? "Good Morning" : (hour < 17) ? "Good Afternoon" : "Good Evening";
        tvGreeting.setText(timeGreeting + ",\n" + firstName + "!");
    }

    private void initLeaderboardViews() {
        cardLeaderboard = findViewById(R.id.cardLeaderboard);
        tvYourRank = findViewById(R.id.tvYourRank);
        tvYourPoints = findViewById(R.id.tvYourPoints);
        layoutLeaderboardRows = findViewById(R.id.layoutLeaderboardRows);
    }

    private void loadLeaderboardData() {
        // 1. Fetch Roommates to get names and images
        com.example.uninest.data.api.ApiClient.getUserApi().getRoommates(houseCode).enqueue(
                new retrofit2.Callback<List<User>>() {
                    @Override
                    public void onResponse(retrofit2.Call<List<User>> call, retrofit2.Response<List<User>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Map<String, User> userMap = new HashMap<>();
                            for (User u : response.body()) {
                                userMap.put(u.getId(), u);
                            }

                            // Add "You" to the map from session
                            User me = new User();
                            me.setId(currentUserId);
                            me.setFirstName("You");
                            me.setProfileImageUrl(sessionManager.getUserImage());
                            userMap.put(currentUserId, me);

                            fetchTasksAndBuildLeaderboard(userMap);
                        }
                    }
                    @Override public void onFailure(retrofit2.Call<List<User>> call, Throwable t) {}
                });
    }

    private void fetchTasksAndBuildLeaderboard(Map<String, User> userMap) {
        // 2. Fetch all tasks for the apartment
        com.example.uninest.data.api.ApiClient.getCalendarApi().getByApartment(houseCode).enqueue(
                new retrofit2.Callback<List<Calendar>>() {
                    @Override
                    public void onResponse(retrofit2.Call<List<Calendar>> call, retrofit2.Response<List<Calendar>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            calculatePointsAndDisplay(response.body(), userMap);
                        }
                    }
                    @Override public void onFailure(retrofit2.Call<List<Calendar>> call, Throwable t) {}
                });
    }

    private void calculatePointsAndDisplay(List<Calendar> tasks, Map<String, User> userMap) {
        // 3. Calculate points per user
        Map<String, Integer> pointsMap = new HashMap<>();

        for (Calendar c : tasks) {
            // Only count Completed Chores
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

        // 4. Sort entries by points descending
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(pointsMap.entrySet());
        Collections.sort(sortedEntries, (a, b) -> b.getValue().compareTo(a.getValue()));

        // 5. Update "YOU" header row
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
            com.example.uninest.utils.ImageUtils.loadProfileImage(ivYourProfile, meUser.getProfileImageUrl());
        }

        // 6. Build the other rows dynamically
        layoutLeaderboardRows.removeAllViews();
        for (int i = 0; i < sortedEntries.size(); i++) {
            String uid = sortedEntries.get(i).getKey();
            if (uid.equals(currentUserId)) continue;

            int rank = i + 1;
            int pts = sortedEntries.get(i).getValue();
            User user = userMap.get(uid);

            // Inflate row
            View row = getLayoutInflater().inflate(R.layout.item_leaderboard_row, layoutLeaderboardRows, false);
            TextView rowRank = row.findViewById(R.id.tvRank);
            ImageView rowProfile = row.findViewById(R.id.ivProfile);
            TextView rowName = row.findViewById(R.id.tvName);
            TextView rowPts = row.findViewById(R.id.tvPoints);

            // Set Data
            String medal = (rank == 1) ? "🥇" : (rank == 2) ? "🥈" : (rank == 3) ? "🥉" : "#" + rank;
            rowRank.setText(medal);
            rowName.setText(user != null ? user.getFirstName() : "Roommate");
            rowPts.setText(pts + " pts");

            // Load Circular Image
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

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;

            if (id == R.id.nav_bills) startActivity(new Intent(this, TenantBillsActivity.class));
            else if (id == R.id.nav_calendar) startActivity(new Intent(this, TenantCalendarActivity.class));
            else if (id == R.id.nav_tickets) startActivity(new Intent(this, TenantTicketsActivity.class));
            else if (id == R.id.nav_profile) startActivity(new Intent(this, TenantProfileActivity.class));
            
            overridePendingTransition(0, 0);
            finish();
            return true;
        });
    }
}