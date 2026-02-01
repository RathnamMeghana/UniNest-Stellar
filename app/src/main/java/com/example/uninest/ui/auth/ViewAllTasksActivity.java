package com.example.uninest.ui.auth;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.Calendar;
import com.example.uninest.model.Recurrence;
import com.example.uninest.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewAllTasksActivity extends AppCompatActivity {

    private RecyclerView rvRoommateFilter;
    private LinearLayout containerOnce, containerWeekly, containerMonthly, containerHistory;
    private TextView tvHeaderOnce, tvHeaderWeekly, tvHeaderMonthly, tvHeaderHistory;

    private SessionManager sessionManager;
    private String houseCode, currentUserId;
    private List<Calendar> allTasks = new ArrayList<>();
    private List<User> filterList = new ArrayList<>();
    private String selectedUserId = "ALL"; // Default filter

    private Map<String, String> roommateNameMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_tasks);

        sessionManager = new SessionManager(this);
        houseCode = sessionManager.fetchHouseCode();
        currentUserId = sessionManager.getUserId();

        rvRoommateFilter = findViewById(R.id.rvRoommateFilter);
        containerOnce = findViewById(R.id.containerOnce);
        containerWeekly = findViewById(R.id.containerWeekly);
        containerMonthly = findViewById(R.id.containerMonthly);
        containerHistory = findViewById(R.id.containerHistory);
        tvHeaderOnce = findViewById(R.id.tvHeaderOnce);
        tvHeaderWeekly = findViewById(R.id.tvHeaderWeekly);
        tvHeaderMonthly = findViewById(R.id.tvHeaderMonthly);
        tvHeaderHistory = findViewById(R.id.tvHeaderHistory);

        rvRoommateFilter.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        fetchRoommatesAndTasks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (houseCode != null) {
            loadTasks();
        }
    }

    private void fetchRoommatesAndTasks() {
        ApiClient.getUserApi().getRoommates(houseCode).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Clear and rebuild map
                    roommateNameMap.clear();
                    for (User u : response.body()) {
                        roommateNameMap.put(u.getId(), u.getFullName());
                    }

                    // Prepare filter list
                    filterList.clear();
                    User all = new User(); all.setId("ALL"); all.setFirstName("All");
                    filterList.add(all);
                    User you = new User(); you.setId(currentUserId); you.setFirstName("You");
                    filterList.add(you);
                    for (User u : response.body()) {
                        if (!u.getId().equals(currentUserId)) filterList.add(u);
                    }
                    rvRoommateFilter.setAdapter(new FilterAdapter());


                    loadTasks();
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) { loadTasks(); }
        });
    }

    private void loadTasks() {
        ApiClient.getCalendarApi().getByApartment(houseCode).enqueue(new Callback<List<Calendar>>() {
            @Override
            public void onResponse(Call<List<Calendar>> call, Response<List<Calendar>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allTasks = response.body();
                    updateUi();
                }
            }

            @Override
            public void onFailure(Call<List<Calendar>> call, Throwable t) {
            }
        });
    }

    private void updateUi() {
        containerOnce.removeAllViews();
        containerWeekly.removeAllViews();
        containerMonthly.removeAllViews();
        containerHistory.removeAllViews();

        boolean hasOnce = false, hasWeekly = false, hasMonthly = false, hasHistory = false;

        for (Calendar c : allTasks) {
            String type = c.getType() != null ? c.getType() : "";

            // Only allow CHORE. This hides Bills, Reminders, etc.
            if (!type.equalsIgnoreCase("CHORE")) continue;

            if (!selectedUserId.equals("ALL") && !c.getAssignedTo().equals(selectedUserId)) continue;

            // 2. CHECK STATUS FIRST: If completed, it goes to History immediately
            String status = c.getStatus() != null ? c.getStatus() : "NOT_STARTED";
            if (status.equalsIgnoreCase("COMPLETED")) {
                containerHistory.addView(createCard(c, containerHistory));
                hasHistory = true;
                continue; // Skip the frequency sorting below
            }

            String freq = "ONCE";
            if (c.getRecurrence() != null && c.getRecurrence().getFrequency() != null) {
                freq = c.getRecurrence().getFrequency().toUpperCase();
            }

            if (freq.equals("WEEKLY")) {
                containerWeekly.addView(createCard(c, containerWeekly));
                hasWeekly = true;
            } else if (freq.equals("MONTHLY")) {
                containerMonthly.addView(createCard(c, containerMonthly));
                hasMonthly = true;
            } else {
                containerOnce.addView(createCard(c, containerOnce));
                hasOnce = true;
            }
        }

        tvHeaderOnce.setVisibility(hasOnce ? View.VISIBLE : View.GONE);
        tvHeaderWeekly.setVisibility(hasWeekly ? View.VISIBLE : View.GONE);
        tvHeaderMonthly.setVisibility(hasMonthly ? View.VISIBLE : View.GONE);
        tvHeaderHistory.setVisibility(hasHistory ? View.VISIBLE : View.GONE);
    }

    private View createCard(Calendar c, ViewGroup parent) {

        View view = LayoutInflater.from(this).inflate(R.layout.item_task_status_card, parent, false);

        View card = view.findViewById(R.id.cardContainer);
        TextView title = view.findViewById(R.id.tvTitle);
        TextView statusBadge = view.findViewById(R.id.tvStatusBadge);
        TextView extraInfo = view.findViewById(R.id.tvExtraInfo);
        TextView tvCreatedBy = view.findViewById(R.id.tvCreatedBy);
        TextView tvDateInfo = view.findViewById(R.id.tvDateInfo);
        com.google.android.material.button.MaterialButton btnMore = view.findViewById(R.id.btnViewMore);


        String status = c.getStatus() != null ? c.getStatus() : "NOT_STARTED";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);

        // 1. Title and Status Text
        title.setText(status.equalsIgnoreCase("NOT_STARTED") ? c.getTitle() + " !!!" : c.getTitle());
        statusBadge.setText(formatStatus(status));

        // 2. Resolve Roommate Names (Ensuring 'effectively final' for lambda)
        String rawAssignee = roommateNameMap.get(c.getAssignedTo());
        final String assigneeName = (c.getAssignedTo() != null && c.getAssignedTo().equals(currentUserId))
                ? "Me" : (rawAssignee != null ? rawAssignee : "Unassigned");

        String rawCreator = roommateNameMap.get(c.getCreatedBy());
        final String creatorName = (c.getCreatedBy() != null && c.getCreatedBy().equals(currentUserId))
                ? "Me" : (rawCreator != null ? rawCreator : "Unknown");

        extraInfo.setText("Assigned to: " + assigneeName);
        tvCreatedBy.setText("Created by: " + creatorName);

        // 3. LOGIC FOR DATE (DUE ON vs COMPLETED ON)
        if (status.equalsIgnoreCase("COMPLETED")) {
            tvDateInfo.setVisibility(View.VISIBLE); // Ensure it is visible
            if (c.getEndDate() != null) {
                // Show the actual day it was finished
                tvDateInfo.setText("Completed on: " + sdf.format(c.getEndDate().toDate()));
            } else {
                tvDateInfo.setText("Completed Today");
            }
        } else {
            if (c.getStartDate() != null) {
                tvDateInfo.setVisibility(View.VISIBLE);
                tvDateInfo.setText("Due on: " + sdf.format(c.getStartDate().toDate()));
            } else {
                tvDateInfo.setVisibility(View.GONE);
            }
        }

        // 4. Dynamic Coloring
        if (status.equals("COMPLETED")) {
            card.setBackgroundResource(R.drawable.bg_card_green);
            statusBadge.setBackgroundResource(R.drawable.bg_status_completed);
            statusBadge.setTextColor(Color.parseColor("#2E7D32"));
        } else if (status.equals("IN_PROGRESS")) {
            card.setBackgroundResource(R.drawable.bg_card_orange);
            statusBadge.setBackgroundResource(R.drawable.bg_status_progress);
            statusBadge.setTextColor(Color.parseColor("#EF6C00"));
        } else {
            card.setBackgroundResource(R.drawable.bg_card_red);
            statusBadge.setBackgroundResource(R.drawable.bg_status_pending);
            statusBadge.setTextColor(Color.parseColor("#C62828"));
        }

        // 5. Click Logic: ONLY ON THE ARROW
        btnMore.setOnClickListener(v -> {
            if (status.equalsIgnoreCase("COMPLETED")) {
                showTaskPopup(c);
            } else if (c.getAssignedTo() != null && c.getAssignedTo().equals(currentUserId)) {
                Intent intent = new Intent(this, ChoreDetailActivity.class);
                intent.putExtra("CHORE_ID", c.getRelatedChoreId());
                intent.putExtra("HOUSE_CODE", houseCode);
                intent.putExtra("TITLE", c.getTitle());
                intent.putExtra("CURRENT_STATUS", status);
                intent.putExtra("DESCRIPTION", c.getDescription());
                intent.putExtra("LOCATION", c.getLocation());
                intent.putExtra("EST_DURATION", c.getEstDuration());
                intent.putExtra("ASSIGNED_TO_NAME", assigneeName);
                intent.putExtra("CREATED_BY_NAME", creatorName);

                if (c.getStartDate() != null) {
                    intent.putExtra("DUE_DATE", sdf.format(c.getStartDate().toDate()));
                }
                startActivity(intent);
            } else {
                showTaskPopup(c);
            }
        });

        return view;
    }

    // Update FilterAdapter to use your "All" image
    class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_roommate_filter, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            User u = filterList.get(pos);
            h.name.setText(u.getFirstName());

            // 1. SET THE IMAGE & ADJUST FITTING
            if (u.getId().equals("ALL")) {
                h.profile.setImageResource(R.drawable.ic_all_users);
                h.profile.setScaleType(ImageView.ScaleType.FIT_CENTER);
                // Padding prevents the icon from touching the blue border
                h.profile.setPadding(5, 5, 5, 5);
            } else {
                h.profile.setImageResource(R.drawable.ic_profile_tenant);
                h.profile.setScaleType(ImageView.ScaleType.CENTER_CROP);
                h.profile.setPadding(0, 0, 0, 0);
            }

            // 2. HIGHLIGHT SELECTED (BLUE SHADOW)
            if (u.getId().equals(selectedUserId)) {
                h.frame.setBackgroundResource(R.drawable.bg_circle_outline_selected);
            } else {
                h.frame.setBackgroundResource(R.drawable.bg_circle_outline);
            }

            h.itemView.setOnClickListener(v -> {
                selectedUserId = u.getId();
                notifyDataSetChanged();
                updateUi();
            });
        }

        @Override
        public int getItemCount() {
            return filterList.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView name;
            View frame;
            ImageView profile;

            VH(View v) {
                super(v);
                name = v.findViewById(R.id.tvRoommateName);
                frame = v.findViewById(R.id.frameCircle);
                profile = v.findViewById(R.id.imgProfile);
            }
        }
    }

    private void showTaskPopup(Calendar c) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_task_details_mini);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // New/Updated view finders
        TextView tvDueDate = dialog.findViewById(R.id.popDueDate);
        TextView tvActual = dialog.findViewById(R.id.popActualTime);
        View layoutActual = dialog.findViewById(R.id.layoutActualTime);

        // Existing finders
        TextView tvTitle = dialog.findViewById(R.id.popTitle);
        TextView tvStatusBadge = dialog.findViewById(R.id.popStatusBadge);
        TextView tvDesc = dialog.findViewById(R.id.popDesc);
        TextView tvLocation = dialog.findViewById(R.id.popLocation);
        TextView tvAssignee = dialog.findViewById(R.id.popAssignee);
        TextView tvEst = dialog.findViewById(R.id.popEstTime);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        String status = c.getStatus() != null ? c.getStatus() : "NOT_STARTED";

        // 1. Populate Basic Info
        tvTitle.setText(c.getTitle());
        tvDesc.setText(c.getDescription() != null && !c.getDescription().isEmpty() ? c.getDescription() : "No description provided.");
        tvLocation.setText(c.getLocation() != null ? c.getLocation() : "General");

        String rawName = roommateNameMap.get(c.getAssignedTo());
        String displayName;
        if (c.getAssignedTo() != null && c.getAssignedTo().equals(currentUserId)) {
            displayName = "Me";
        } else {
            displayName = (rawName != null) ? rawName : "Unassigned";
        }
        tvAssignee.setText(displayName);

        // 2. Dates
        if (c.getStartDate() != null) {
            tvDueDate.setText(sdf.format(c.getStartDate().toDate()));
        }

        // 3. Time Taken & Status Styling
        tvEst.setText(c.getEstDuration() + " Mins");

        if (c.getStatus() != null && c.getStatus().equalsIgnoreCase("COMPLETED")) {
            layoutActual.setVisibility(View.VISIBLE);
            // Ensure you have added getActualDuration() to your Calendar.java model!
            tvActual.setText(c.getActualDuration() + " Mins");

            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_completed);
            tvStatusBadge.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            layoutActual.setVisibility(View.GONE); // Hide "Actual Time" if not finished
            tvStatusBadge.setBackgroundResource(status.equalsIgnoreCase("IN_PROGRESS") ?
                    R.drawable.bg_status_progress : R.drawable.bg_status_pending);
            tvStatusBadge.setTextColor(status.equalsIgnoreCase("IN_PROGRESS") ?
                    Color.parseColor("#EF6C00") : Color.parseColor("#C62828"));
        }

        tvStatusBadge.setText(formatStatus(status));
        dialog.findViewById(R.id.btnPopClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String formatStatus(String s) {
        if ("NOT_STARTED".equals(s)) return "Not Started";
        if ("IN_PROGRESS".equals(s)) return "In Progress";
        if ("COMPLETED".equals(s)) return "Completed";
        return "Pending";
    }
}