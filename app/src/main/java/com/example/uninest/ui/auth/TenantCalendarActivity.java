package com.example.uninest.ui.auth;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.CalendarApi;
import com.example.uninest.data.api.UserApi;
import com.example.uninest.data.api.BillsApi;
import com.example.uninest.model.BillsRequest;
import com.example.uninest.model.Calendar;
import com.example.uninest.model.CalendarUpdateRequest;
import com.example.uninest.model.Recurrence;
import com.example.uninest.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TenantCalendarActivity extends AppCompatActivity {

    // UI Components
    private CalendarView calendarView;
    private LinearLayout weekViewContainer;
    private FrameLayout btnToggleCalendar;
    private ImageView imgToggleArrow;
    private TextView tvCurrentDateHeader;

    // Task Sections
    private LinearLayout containerChores, containerEvents, containerReminders;
    private TextView headerChores, headerEvents, headerReminders;

    // Filters
    private TextView filterAll, filterChores, filterEvents, filterReminders;
    private String activeFilter = "ALL";

    // Data
    private CalendarApi calendarApi;
    private UserApi userApi;
    private SessionManager sessionManager;
    private String houseCode;
    private String currentUserId;
    private List<Calendar> allEvents = new ArrayList<>();
    private Map<String, String> roommateNamesMap = new HashMap<>();
    private List<Calendar> filteredEvents = new ArrayList<>();

    private BillsApi billsApi;
    private List<BillsRequest> allBills = new ArrayList<>();

    // State
    private boolean isCalendarExpanded = false; // Start collapsed
    private java.util.Calendar currentSelectedDate = java.util.Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_calendar);

        sessionManager = new SessionManager(this);
        houseCode = sessionManager.fetchHouseCode();
        currentUserId = sessionManager.getUserId();

        if (houseCode == null || currentUserId == null) {
            Toast.makeText(this, "Session Error. Please login again.", Toast.LENGTH_SHORT).show();
        }

        calendarApi = ApiClient.getCalendarApi();
        userApi = ApiClient.getUserApi();
        billsApi = ApiClient.getBillsApi();

        initViews();
        setupBottomNav();
        setupFilters();
        setupWeekView();

        findViewById(R.id.btnAddEvent).setOnClickListener(v ->
                startActivity(new Intent(this, AddCalendarEventActivity.class))
        );

        View btnViewAll = findViewById(R.id.btnViewTasks);
        if (btnViewAll != null) {
            btnViewAll.setOnClickListener(v -> {
                startActivity(new Intent(this, ViewAllTasksActivity.class));
            });
        }

        btnToggleCalendar.setOnClickListener(v -> toggleCalendarMode());

        calendarView.setOnDayClickListener(eventDay -> {
            currentSelectedDate = eventDay.getCalendar();
            updateDateHeader();
            displayTasksForDate(currentSelectedDate);
            setupWeekView();
            highlightMonthViewDate(currentSelectedDate);
        });

        updateDateHeader();
        highlightMonthViewDate(currentSelectedDate);
    }

    private void initViews() {
        calendarView = findViewById(R.id.calendarView);
        try {
            calendarView.setCalendarDayLayout(R.layout.item_custom_calendar_day);
        } catch (Exception e) {
            e.printStackTrace();
        }
        makeHeaderBold(calendarView);

        weekViewContainer = findViewById(R.id.weekViewContainer);
        btnToggleCalendar = findViewById(R.id.btnToggleCalendar);
        imgToggleArrow = findViewById(R.id.imgToggleArrow);
        tvCurrentDateHeader = findViewById(R.id.tvCurrentDateHeader);

        containerChores = findViewById(R.id.containerChores);
        containerEvents = findViewById(R.id.containerEvents);
        containerReminders = findViewById(R.id.containerReminders);

        headerChores = findViewById(R.id.tvHeaderChores);
        headerEvents = findViewById(R.id.tvHeaderEvents);
        headerReminders = findViewById(R.id.tvHeaderReminders);

        filterAll = findViewById(R.id.filterAll);
        filterChores = findViewById(R.id.filterChores);
        filterEvents = findViewById(R.id.filterEvents);
        filterReminders = findViewById(R.id.filterReminders);
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_calendar);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_calendar) return true;

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, TenantHomeActivity.class));
            } else if (itemId == R.id.nav_bills) {
                startActivity(new Intent(this, TenantBillsActivity.class));
            } else if (itemId == R.id.nav_tickets) {
                startActivity(new Intent(this, TenantTicketsActivity.class));
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, TenantProfileActivity.class));
            }
            overridePendingTransition(0, 0);
            finish();
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (houseCode != null) {
            fetchRoommatesAndEvents();
        }
    }

    private void fetchRoommatesAndEvents() {
        userApi.getRoommates(houseCode).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (User u : response.body()) {
                        String name = u.getFullName();
                        if (u.getId() != null) {
                            roommateNamesMap.put(u.getId(), name);
                        }
                    }
                }
                loadEvents();
                fetchBills();
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                loadEvents();
                fetchBills();
            }
        });
    }

    private void fetchBills() {
        billsApi.getBills(currentUserId).enqueue(new Callback<List<BillsRequest>>() {
            @Override
            public void onResponse(Call<List<BillsRequest>> call, Response<List<BillsRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allBills = response.body();
                    updateCalendarDots(); // Refresh dots to show bill due dates
                    displayTasksForDate(currentSelectedDate); // Refresh list
                }
            }
            @Override public void onFailure(Call<List<BillsRequest>> call, Throwable t) {}
        });
    }

    private void loadEvents() {
        calendarApi.getByApartment(houseCode).enqueue(new Callback<List<Calendar>>() {
            @Override
            public void onResponse(Call<List<Calendar>> call, Response<List<Calendar>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allEvents = response.body();
                    applyUserFilter();
                    updateCalendarDots();
                    setupWeekView();
                    displayTasksForDate(currentSelectedDate);
                }
            }
            @Override
            public void onFailure(Call<List<Calendar>> call, Throwable t) {
                Toast.makeText(TenantCalendarActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyUserFilter() {
        filteredEvents.clear();
        for (Calendar c : allEvents) {
            boolean include = false;
            String type = c.getType() != null ? c.getType() : "";
            if (isTypeMatch(type, "CHORE")) {
                if (isUserMatch(c.getAssignedTo())) {
                    include = true;
                }
            }
            else {
                if (isUserMatch(c.getCreatedBy()) || isUserMatch(c.getAssignedTo())) {
                    include = true;
                }
            }

            if (include) filteredEvents.add(c);
        }
    }

    private boolean isUserMatch(String idToCheck) {
        return idToCheck != null && idToCheck.equals(currentUserId);
    }

    private void displayTasksForDate(java.util.Calendar selectedDate) {
        containerChores.removeAllViews();
        containerEvents.removeAllViews();
        containerReminders.removeAllViews();

        boolean hasChores = false;
        boolean hasEvents = false;
        boolean hasReminders = false;

        java.util.Calendar today = java.util.Calendar.getInstance();
        // Normalize today to start of day for accurate comparison
        today.set(java.util.Calendar.HOUR_OF_DAY, 0);
        today.set(java.util.Calendar.MINUTE, 0);
        today.set(java.util.Calendar.SECOND, 0);
        today.set(java.util.Calendar.MILLISECOND, 0);

        long nowMillis = System.currentTimeMillis();

        for (Calendar c : filteredEvents) {
            // 1. Skip if already completed
            if ("COMPLETED".equalsIgnoreCase(c.getStatus())) continue;
            if (c.getStartDate() == null) continue;

            String type = c.getType() != null ? c.getType() : "";
            java.util.Calendar eventStart = java.util.Calendar.getInstance();
            eventStart.setTime(c.getStartDate().toDate());

            // --- 2. EVENT EXPIRY LOGIC ---
            // If it's an event and the end time has passed, skip it entirely
            if (type.equalsIgnoreCase("EVENT")) {
                if (c.getEndDate() != null) {
                    if (c.getEndDate().toDate().getTime() < nowMillis) {
                        continue;
                    }
                }
            }

            // --- 3. MATCHING LOGIC ---
            boolean isMatch = false;

            // Check if type is a "Sticky" type
            boolean isOverdue = false;
            if (type.contains("REMINDER") || type.contains("BILL") || type.contains("MAINTENANCE")) {
                boolean isScheduledForSelectedDay = isSameDay(eventStart, selectedDate);
                isOverdue = eventStart.before(today);
                boolean isViewingToday = isSameDay(selectedDate, today);

                // Only show on Today if overdue (removed from past dates)
                if (isOverdue) {
                    if (isViewingToday) isMatch = true;
                } else if (isScheduledForSelectedDay) {
                    isMatch = true;
                }
            } else {
                // Standard Logic for Chores and Events
                isMatch = isSameDay(eventStart, selectedDate);

                // Handle Recurring Chores
                Recurrence r = c.getRecurrence();
                if (r != null && r.getFrequency() != null) {
                    if (!selectedDate.before(eventStart)) {
                        if ("WEEKLY".equalsIgnoreCase(r.getFrequency())) {
                            if (eventStart.get(java.util.Calendar.DAY_OF_WEEK) == selectedDate.get(java.util.Calendar.DAY_OF_WEEK))
                                isMatch = true;
                        } else if ("MONTHLY".equalsIgnoreCase(r.getFrequency())) {
                            if (eventStart.get(java.util.Calendar.DAY_OF_MONTH) == selectedDate.get(java.util.Calendar.DAY_OF_MONTH))
                                isMatch = true;
                        }
                    }
                }
            }

            // --- 4. UI POPULATION ---
            if (isMatch && isTypeMatch(c.getType(), activeFilter)) {
                if (isTypeMatch(c.getType(), "CHORE")) {
                    addCard(c, containerChores, false);
                    hasChores = true;
                } else if (isTypeMatch(c.getType(), "EVENT")) {
                    addCard(c, containerEvents, false);
                    hasEvents = true;
                } else {
                    addCard(c, containerReminders, isOverdue);
                    hasReminders = true;
                }
            }
        }

        // Bills logic (overdue bills only show on Today, not past dates)
        if (activeFilter.equals("ALL") || activeFilter.equals("REMINDER")) {
            for (BillsRequest b : allBills) {
                java.util.Calendar billDue = parseIsoToCalendar(b.getDueDate());
                if (billDue != null) {
                    boolean isOverdue = billDue.before(today);
                    boolean isViewingToday = isSameDay(selectedDate, today);

                    if (isOverdue) {
                        // Overdue bill: only show on today
                        if (isViewingToday) {
                            addBillCard(b, containerReminders);
                            hasReminders = true;
                        }
                    } else if (isSameDay(billDue, selectedDate)) {
                        // Future/today bill: show on its due date
                        addBillCard(b, containerReminders);
                        hasReminders = true;
                    }
                }
            }
        }

        headerChores.setVisibility(hasChores ? View.VISIBLE : View.GONE);
        headerEvents.setVisibility(hasEvents ? View.VISIBLE : View.GONE);
        headerReminders.setVisibility(hasReminders ? View.VISIBLE : View.GONE);
    }

    private void addBillCard(BillsRequest b, ViewGroup parent) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_calendar_task_card, parent, false);
        View container = view.findViewById(R.id.cardContainer);
        TextView title = view.findViewById(R.id.tvTitle);
        TextView desc = view.findViewById(R.id.tvDesc);
        TextView extraInfo = view.findViewById(R.id.tvExtraInfo);
        TextView statusBadge = view.findViewById(R.id.popStatusBadge);
        com.google.android.material.button.MaterialButton btnMore = view.findViewById(R.id.btnViewMore);

        container.setBackgroundResource(R.drawable.bg_card_orange);

        title.setText(b.getTitle());
        title.setTextColor(Color.BLACK);
        title.setTypeface(null, Typeface.BOLD);


        statusBadge.setVisibility(View.VISIBLE);
        statusBadge.setText("BILL DUE");
        statusBadge.setBackgroundResource(R.drawable.bg_status_pending);
        statusBadge.setTextColor(Color.parseColor("#BF360C"));
        statusBadge.setTypeface(null, Typeface.BOLD);

        // Calculate My Share
        double myShare = 0;
        if (b.getSplits() != null) {
            for (BillsRequest.Split s : b.getSplits()) {
                if (currentUserId.equals(s.getUserId())) {
                    myShare = s.getAmountOwed();
                    break;
                }
            }
        }


        desc.setVisibility(View.VISIBLE);
        desc.setText(String.format(Locale.getDefault(), "You owe: €%.2f", myShare));
        desc.setTextColor(Color.parseColor("#E65100"));
        desc.setTypeface(null, Typeface.BOLD);
        desc.setTextSize(18f);


        String creator = roommateNamesMap.get(b.getCreatorId());
        if (currentUserId.equals(b.getCreatorId())) creator = "Me";

        extraInfo.setVisibility(View.VISIBLE);
        extraInfo.setText(String.format(Locale.getDefault(),
                "Total bill: €%.2f\nCreated by: %s",
                b.getTotalAmount(),
                (creator != null ? creator : "Unknown")));
        extraInfo.setTextColor(Color.parseColor("#E65100"));


        btnMore.setText("View");

        btnMore.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E65100")));
        btnMore.setTextColor(Color.WHITE);

        btnMore.setOnClickListener(v -> {
            startActivity(new Intent(this, TenantBillsActivity.class));
        });

        parent.addView(view);
    }

    private void addCard(Calendar c, ViewGroup parent, boolean isOverdue) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_calendar_task_card, parent, false);

        View container = view.findViewById(R.id.cardContainer);
        TextView title = view.findViewById(R.id.tvTitle);
        TextView desc = view.findViewById(R.id.tvDesc);
        TextView extraInfo = view.findViewById(R.id.tvExtraInfo);
        TextView tvStatus = view.findViewById(R.id.popStatusBadge);
        com.google.android.material.button.MaterialButton btnMore = view.findViewById(R.id.btnViewMore);

        title.setText(c.getTitle());
        String dVal = c.getDescription() != null ? c.getDescription() : "";
        desc.setText(dVal);
        desc.setVisibility(View.GONE);

        String type = c.getType() != null ? c.getType() : "";

        if (isTypeMatch(type, "CHORE")) {
            // --- CHORE LOGIC ---
            container.setBackgroundResource(R.drawable.bg_card_green);

            String creatorName = roommateNamesMap.get(c.getCreatedBy());
            if (isUserMatch(c.getCreatedBy())) creatorName = "Me";
            if (creatorName == null) creatorName = "Unknown";

            String assigneeName = roommateNamesMap.get(c.getAssignedTo());
            if (isUserMatch(c.getAssignedTo())) assigneeName = "Me";
            if (assigneeName == null) assigneeName = "Unassigned";

            extraInfo.setText("Created by: " + creatorName + "\nAssigned to: " + assigneeName);
            extraInfo.setTextColor(Color.parseColor("#1B5E20"));
            extraInfo.setVisibility(View.VISIBLE);

            String status = c.getStatus() != null ? c.getStatus() : "NOT_STARTED";
            tvStatus.setText(formatStatus(status));
            tvStatus.setVisibility(View.VISIBLE);

            if (status.equals("COMPLETED")) {
                tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
                tvStatus.setTextColor(Color.parseColor("#2E7D32"));
            } else if (status.equals("IN_PROGRESS")) {
                tvStatus.setBackgroundResource(R.drawable.bg_status_progress);
                tvStatus.setTextColor(Color.parseColor("#EF6C00"));
            } else {
                tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                tvStatus.setTextColor(Color.parseColor("#C62828"));
            }

            final String finalCreator = creatorName;
            final String finalAssignee = assigneeName;

            btnMore.setOnClickListener(v -> {
                Intent intent = new Intent(TenantCalendarActivity.this, ChoreDetailActivity.class);
                intent.putExtra("CHORE_ID", c.getRelatedChoreId());
                intent.putExtra("HOUSE_CODE", houseCode);
                intent.putExtra("TITLE", c.getTitle());
                intent.putExtra("DESCRIPTION", c.getDescription());
                intent.putExtra("EST_DURATION", c.getEstDuration());
                intent.putExtra("CURRENT_STATUS", status);
                intent.putExtra("LOCATION", c.getLocation());
                intent.putExtra("CREATED_BY_NAME", finalCreator);
                intent.putExtra("ASSIGNED_TO_NAME", finalAssignee);

                if (c.getStartDate() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                    intent.putExtra("DUE_DATE", sdf.format(c.getStartDate().toDate()));
                }
                startActivity(intent);
            });

        } else if (isTypeMatch(type, "EVENT")) {
            // --- EVENT LOGIC ---
            container.setBackgroundResource(R.drawable.bg_card_pink);
            tvStatus.setVisibility(View.GONE);
            btnMore.setVisibility(View.GONE);

            if (c.getStartDate() != null) {
                SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm a", Locale.US);
                extraInfo.setText(timeFmt.format(c.getStartDate().toDate()));
                extraInfo.setTextColor(Color.parseColor("#C2185B"));
                extraInfo.setVisibility(View.VISIBLE);
            }

            // Click the card to view details
            container.setOnClickListener(v -> showEventDetailsMiniModal(c));
        } else {
            // --- REMINDER / STICKY LOGIC ---
            container.setBackgroundResource(R.drawable.bg_card_orange);

            // Show OVERDUE badge if the reminder is from a past date
            if (isOverdue && c.getStartDate() != null) {
                tvStatus.setVisibility(View.VISIBLE);
                SimpleDateFormat overdueFmt = new SimpleDateFormat("MMM d", Locale.US);
                tvStatus.setText("\u26A0\uFE0F Overdue since " + overdueFmt.format(c.getStartDate().toDate()));
                tvStatus.setBackgroundResource(R.drawable.bg_status_overdue);
                tvStatus.setTextColor(Color.parseColor("#D32F2F"));
                tvStatus.setTypeface(null, Typeface.BOLD);
                tvStatus.setTextSize(12f);
                int padH = (int) (10 * getResources().getDisplayMetrics().density);
                int padV = (int) (4 * getResources().getDisplayMetrics().density);
                tvStatus.setPadding(padH, padV, padH, padV);
            } else {
                tvStatus.setVisibility(View.GONE);
            }

            // Transform the button into a "Done" action
            btnMore.setVisibility(View.VISIBLE);
            btnMore.setText("Done");
            btnMore.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E65100")));
            btnMore.setTextColor(Color.WHITE);

            if (c.getAmount() != null && c.getAmount() > 0) {
                extraInfo.setText(String.format(Locale.US, "Amount: €%.2f", c.getAmount()));
                extraInfo.setTextColor(Color.parseColor("#E65100"));
                extraInfo.setVisibility(View.VISIBLE);
            } else {
                extraInfo.setVisibility(View.GONE);
            }

            // Click logic to mark the reminder as completed
            btnMore.setOnClickListener(v -> markReminderAsDone(c));

            // Click the card to view details
            container.setOnClickListener(v -> showEventDetailsMiniModal(c));
        }

        parent.addView(view);
    }

    /**
     * Method to update the reminder status to COMPLETED on the server.
     * Uses CalendarUpdateRequest to send only the fields the backend expects,
     * avoiding FirestoreTimestamp serialization mismatches.
     */
    private void markReminderAsDone(Calendar c) {
        CalendarUpdateRequest updateReq = new CalendarUpdateRequest();
        updateReq.setTitle(c.getTitle());
        updateReq.setStatus("COMPLETED");

        calendarApi.updateEventStatus(c.getId(), updateReq).enqueue(new Callback<Calendar>() {
            @Override
            public void onResponse(Call<Calendar> call, Response<Calendar> response) {
                if (response.isSuccessful()) {
                    c.setStatus("COMPLETED");
                    Toast.makeText(TenantCalendarActivity.this, "Marked as Done", Toast.LENGTH_SHORT).show();
                    loadEvents(); // Refresh data to update dots and task lists
                } else {
                    Toast.makeText(TenantCalendarActivity.this, "Update Failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Calendar> call, Throwable t) {
                Toast.makeText(TenantCalendarActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper for status text
    private String formatStatus(String s) {
        if (s.equals("NOT_STARTED")) return "Not Started";
        if (s.equals("IN_PROGRESS")) return "In Progress";
        if (s.equals("COMPLETED")) return "Completed";
        return s;
    }

    private void showEventDetailsMiniModal(Calendar c) {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_event_details_mini);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvTitle = dialog.findViewById(R.id.popTitle);
        TextView tvTypeBadge = dialog.findViewById(R.id.popTypeBadge);
        TextView tvDate = dialog.findViewById(R.id.popDate);
        LinearLayout layoutTime = dialog.findViewById(R.id.layoutTime);
        TextView tvTime = dialog.findViewById(R.id.popTime);
        TextView tvDesc = dialog.findViewById(R.id.popDesc);
        LinearLayout layoutAmount = dialog.findViewById(R.id.layoutAmount);
        TextView tvAmount = dialog.findViewById(R.id.popAmount);
        Button btnClose = dialog.findViewById(R.id.btnPopClose);

        tvTitle.setText(c.getTitle());

        // Type Badge
        String type = c.getType() != null ? c.getType() : "EVENT";
        tvTypeBadge.setText(type.toUpperCase());
        if (type.contains("REMINDER") || type.contains("BILL") || type.contains("MAINTENANCE")) {
            tvTypeBadge.setBackgroundResource(R.drawable.bg_status_pending);
            tvTypeBadge.setTextColor(Color.parseColor("#E65100")); // Orange
        } else {
            tvTypeBadge.setBackgroundResource(R.drawable.bg_status_pending);
            tvTypeBadge.setTextColor(Color.parseColor("#C62828")); // Pink/Red
        }

        // Description
        if (c.getDescription() != null && !c.getDescription().isEmpty()) {
            tvDesc.setText(c.getDescription());
        } else {
            tvDesc.setText("No description provided.");
            tvDesc.setTypeface(null, Typeface.ITALIC);
        }

        // Date & Time logic
        if (c.getStartDate() != null) {
            SimpleDateFormat dateFmt = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            tvDate.setText(dateFmt.format(c.getStartDate().toDate()));

            // Only show time for Events
            if (isTypeMatch(type, "EVENT")) {
                SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm a", Locale.US);
                tvTime.setText(timeFmt.format(c.getStartDate().toDate()));
                layoutTime.setVisibility(View.VISIBLE);
            } else {
                layoutTime.setVisibility(View.GONE);
            }
        }

        // Amount
        if (c.getAmount() != null && c.getAmount() > 0) {
            layoutAmount.setVisibility(View.VISIBLE);
            tvAmount.setText(String.format(Locale.US, "€%.2f", c.getAmount()));
        } else {
            layoutAmount.setVisibility(View.GONE);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void updateDateHeader() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
        tvCurrentDateHeader.setText(sdf.format(currentSelectedDate.getTime()));
    }

    private void updateCalendarDots() {
        List<EventDay> mapEvents = new ArrayList<>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
        java.util.Map<String, java.util.Set<String>> dailyEventsMap = new java.util.HashMap<>();
        java.util.Map<String, java.util.Calendar> dateObjectMap = new java.util.HashMap<>();
        java.util.Calendar limitDate = java.util.Calendar.getInstance();
        limitDate.add(java.util.Calendar.MONTH, 6);

        long nowMillis = System.currentTimeMillis();
        java.util.Calendar today = java.util.Calendar.getInstance();
        today.set(java.util.Calendar.HOUR_OF_DAY, 0);
        today.set(java.util.Calendar.MINUTE, 0);
        today.set(java.util.Calendar.SECOND, 0);
        today.set(java.util.Calendar.MILLISECOND, 0);

        // Iterate over FILTERED events to show dots only for user's tasks
        for (Calendar c : filteredEvents) {

            if ("COMPLETED".equalsIgnoreCase(c.getStatus())) continue;

            if (!isTypeMatch(c.getType(), activeFilter)) continue;
            if (c.getStartDate() == null) continue;

            String rawType = c.getType() != null ? c.getType() : "";

            // EVENT EXPIRY: skip events whose end time has already passed
            if (isTypeMatch(rawType, "EVENT")) {
                if (c.getEndDate() != null && c.getEndDate().toDate().getTime() < nowMillis) {
                    continue;
                }
            }

            String simplifiedType = "";
            if (isTypeMatch(rawType, "CHORE")) simplifiedType = "CHORE";
            else if (isTypeMatch(rawType, "EVENT")) simplifiedType = "EVENT";
            else if (isTypeMatch(rawType, "REMINDER")) simplifiedType = "REMINDER";

            if (simplifiedType.isEmpty()) continue;

            java.util.Calendar eventDate = java.util.Calendar.getInstance();
            eventDate.setTime(c.getStartDate().toDate());

            boolean isWeekly = false;
            boolean isMonthly = false;
            if (c.getRecurrence() != null && c.getRecurrence().getFrequency() != null) {
                if ("WEEKLY".equalsIgnoreCase(c.getRecurrence().getFrequency())) isWeekly = true;
                else if ("MONTHLY".equalsIgnoreCase(c.getRecurrence().getFrequency())) isMonthly = true;
            }

            // For overdue reminders: skip the normal dot placement (will be added on today below)
            boolean isReminderOverdue = false;
            if (isTypeMatch(rawType, "REMINDER")) {
                java.util.Calendar normalizedStart = java.util.Calendar.getInstance();
                normalizedStart.setTime(c.getStartDate().toDate());
                normalizedStart.set(java.util.Calendar.HOUR_OF_DAY, 0);
                normalizedStart.set(java.util.Calendar.MINUTE, 0);
                normalizedStart.set(java.util.Calendar.SECOND, 0);
                normalizedStart.set(java.util.Calendar.MILLISECOND, 0);
                isReminderOverdue = normalizedStart.before(today);
            }

            // Only add dots on original/recurring dates if NOT an overdue reminder
            if (!isReminderOverdue) {
                do {
                    String dateKey = sdf.format(eventDate.getTime());
                    if (!dailyEventsMap.containsKey(dateKey)) {
                        dailyEventsMap.put(dateKey, new java.util.HashSet<>());
                        dateObjectMap.put(dateKey, (java.util.Calendar) eventDate.clone());
                    }
                    dailyEventsMap.get(dateKey).add(simplifiedType);

                    if (isWeekly) eventDate.add(java.util.Calendar.DAY_OF_MONTH, 7);
                    else if (isMonthly) eventDate.add(java.util.Calendar.MONTH, 1);
                } while ((isWeekly || isMonthly) && eventDate.before(limitDate));
            }

            // STICKY REMINDERS: show dot ONLY on "Today" if overdue
            if (isReminderOverdue) {
                String todayKey = sdf.format(today.getTime());
                if (!dailyEventsMap.containsKey(todayKey)) {
                    dailyEventsMap.put(todayKey, new java.util.HashSet<>());
                    dateObjectMap.put(todayKey, (java.util.Calendar) today.clone());
                }
                dailyEventsMap.get(todayKey).add("REMINDER");
            }
        }

        // --- PROCESS BILLS (As Reminders — overdue bills only show on Today) ---
        if (activeFilter.equals("ALL") || activeFilter.equals("REMINDER")) {
            for (BillsRequest b : allBills) {
                java.util.Calendar billDate = parseIsoToCalendar(b.getDueDate());
                if (billDate == null) continue;

                java.util.Calendar normalizedBill = (java.util.Calendar) billDate.clone();
                normalizedBill.set(java.util.Calendar.HOUR_OF_DAY, 0);
                normalizedBill.set(java.util.Calendar.MINUTE, 0);
                normalizedBill.set(java.util.Calendar.SECOND, 0);
                normalizedBill.set(java.util.Calendar.MILLISECOND, 0);

                String dateKey;
                java.util.Calendar dotDate;
                if (normalizedBill.before(today)) {
                    // Overdue bill: show dot only on today
                    dateKey = sdf.format(today.getTime());
                    dotDate = (java.util.Calendar) today.clone();
                } else {
                    // Future/today bill: show dot on its due date
                    dateKey = sdf.format(billDate.getTime());
                    dotDate = (java.util.Calendar) billDate.clone();
                }

                if (!dailyEventsMap.containsKey(dateKey)) {
                    dailyEventsMap.put(dateKey, new java.util.HashSet<>());
                    dateObjectMap.put(dateKey, dotDate);
                }
                dailyEventsMap.get(dateKey).add("REMINDER");
            }
        }


        for (String dateKey : dailyEventsMap.keySet()) {
            java.util.Set<String> types = dailyEventsMap.get(dateKey);
            java.util.Calendar cal = dateObjectMap.get(dateKey);

            boolean hasChore = types.contains("CHORE");
            boolean hasEvent = types.contains("EVENT");
            boolean hasReminder = types.contains("REMINDER");

            int iconRes = 0;
            if (activeFilter.equals("CHORE") && hasChore) iconRes = R.drawable.ic_dot_green;
            else if (activeFilter.equals("EVENT") && hasEvent) iconRes = R.drawable.ic_dot_pink;
            else if (activeFilter.equals("REMINDER") && hasReminder) iconRes = R.drawable.ic_dot_orange;
            else {
                if (hasChore && hasEvent && hasReminder) iconRes = R.drawable.ic_dots_all_three;
                else if (hasChore && hasEvent) iconRes = R.drawable.ic_dots_green_pink;
                else if (hasChore && hasReminder) iconRes = R.drawable.ic_dots_green_orange;
                else if (hasEvent && hasReminder) iconRes = R.drawable.ic_dots_pink_orange;
                else if (hasChore) iconRes = R.drawable.ic_dot_green;
                else if (hasEvent) iconRes = R.drawable.ic_dot_pink;
                else if (hasReminder) iconRes = R.drawable.ic_dot_orange;
            }
            if (iconRes != 0) mapEvents.add(new EventDay(cal, iconRes));
        }
        calendarView.setEvents(mapEvents);
    }

    // Helper to parse ISO Strings from Bills to Calendar objects
    private java.util.Calendar parseIsoToCalendar(String iso) {
        if (iso == null || iso.isEmpty()) return null;
        try {
            String cleanIso = iso.replaceAll("Z$", "+0000");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date date = sdf.parse(cleanIso);

            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(date);
            return cal;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isMatch(java.util.Calendar start, java.util.Calendar selected, Recurrence r) {
        boolean isMatch = isSameDay(start, selected);
        if (r != null && r.getFrequency() != null) {
            if (!selected.before(start)) {
                if ("WEEKLY".equalsIgnoreCase(r.getFrequency()))
                    return start.get(java.util.Calendar.DAY_OF_WEEK) == selected.get(java.util.Calendar.DAY_OF_WEEK);
                if ("MONTHLY".equalsIgnoreCase(r.getFrequency()))
                    return start.get(java.util.Calendar.DAY_OF_MONTH) == selected.get(java.util.Calendar.DAY_OF_MONTH);
            }
        }
        return isMatch;
    }

    private void setupWeekView() {
        weekViewContainer.removeAllViews();
        java.util.Calendar cal = (java.util.Calendar) currentSelectedDate.clone();
        java.util.Calendar today = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY);

        for (int i = 0; i < 7; i++) {
            View dayItem = LayoutInflater.from(this).inflate(R.layout.item_calendar_week_day, weekViewContainer, false);
            TextView tvName = dayItem.findViewById(R.id.tvDayName);
            TextView tvNumber = dayItem.findViewById(R.id.tvDayNumber);
            ImageView imgDot = dayItem.findViewById(R.id.imgDayDot);

            SimpleDateFormat sdfDay = new SimpleDateFormat("d", Locale.US);
            SimpleDateFormat sdfName = new SimpleDateFormat("EE", Locale.US);

            tvName.setText(sdfName.format(cal.getTime()));
            tvNumber.setText(sdfDay.format(cal.getTime()));

            boolean isSelected = isSameDay(cal, currentSelectedDate);
            boolean isToday = isSameDay(cal, today);

            if (isSelected) {
                tvNumber.setBackgroundResource(R.drawable.bg_calendar_selector);
                tvNumber.setTextColor(Color.parseColor("#B792D9"));
            } else if (isToday) {
                tvNumber.setBackgroundResource(0);
                tvNumber.setTextColor(Color.parseColor("#E91E63"));
            } else {
                tvNumber.setBackgroundResource(0);
                tvNumber.setTextColor(Color.BLACK);
            }

            int dotRes = getIconForDate(cal);
            if (dotRes != 0) {
                imgDot.setImageResource(dotRes);
                imgDot.setVisibility(View.VISIBLE);
            } else {
                imgDot.setVisibility(View.INVISIBLE);
            }

            java.util.Calendar clickedDay = (java.util.Calendar) cal.clone();
            dayItem.setOnClickListener(v -> {
                currentSelectedDate = clickedDay;
                setupWeekView();
                displayTasksForDate(currentSelectedDate);
                updateDateHeader();
                try { calendarView.setDate(clickedDay); } catch (Exception e) {}
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            dayItem.setLayoutParams(params);
            weekViewContainer.addView(dayItem);
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }
    }

    private int getIconForDate(java.util.Calendar targetDate) {
        boolean hasChore = false, hasEvent = false, hasReminder = false;

        java.util.Calendar today = java.util.Calendar.getInstance();
        // Normalize today to the start of the day for accurate comparisons
        today.set(java.util.Calendar.HOUR_OF_DAY, 0);
        today.set(java.util.Calendar.MINUTE, 0);
        today.set(java.util.Calendar.SECOND, 0);
        today.set(java.util.Calendar.MILLISECOND, 0);

        long nowMillis = System.currentTimeMillis();

        // Iterate filteredEvents so dots match filter
        for (Calendar c : filteredEvents) {

            if ("COMPLETED".equalsIgnoreCase(c.getStatus())) continue;
            if (!isTypeMatch(c.getType(), activeFilter)) continue;
            if (c.getStartDate() == null) continue;

            String type = c.getType() != null ? c.getType() : "";
            java.util.Calendar eventStart = java.util.Calendar.getInstance();
            eventStart.setTime(c.getStartDate().toDate());

            // --- 1. EVENT EXPIRY LOGIC ---
            // If it's an event and it has already ended, don't show the dot at all
            if (type.equalsIgnoreCase("EVENT")) {
                if (c.getEndDate() != null && c.getEndDate().toDate().getTime() < nowMillis) {
                    continue;
                }
            }

            // --- 2. MATCHING LOGIC ---
            boolean isMatch = false;

            if (type.contains("REMINDER") || type.contains("BILL") || type.contains("MAINTENANCE")) {
                // STICKY DOT LOGIC — overdue reminders only show on Today, not past dates
                boolean isReminderOverdue = eventStart.before(today);
                if (isReminderOverdue) {
                    // Only show dot on Today
                    if (isSameDay(targetDate, today)) isMatch = true;
                } else {
                    // Show on its scheduled day
                    if (isSameDay(eventStart, targetDate)) isMatch = true;
                }
            } else {
                // CHORE / ACTIVE EVENT LOGIC (Standard + Recurrence)
                isMatch = isSameDay(eventStart, targetDate);

                Recurrence r = c.getRecurrence();
                if (r != null && r.getFrequency() != null) {
                    if (!targetDate.before(eventStart)) {
                        if ("WEEKLY".equalsIgnoreCase(r.getFrequency())) {
                            if (eventStart.get(java.util.Calendar.DAY_OF_WEEK) == targetDate.get(java.util.Calendar.DAY_OF_WEEK))
                                isMatch = true;
                        } else if ("MONTHLY".equalsIgnoreCase(r.getFrequency())) {
                            if (eventStart.get(java.util.Calendar.DAY_OF_MONTH) == targetDate.get(java.util.Calendar.DAY_OF_MONTH))
                                isMatch = true;
                        }
                    }
                }
            }

            if (isMatch) {
                if (isTypeMatch(type, "CHORE")) hasChore = true;
                else if (isTypeMatch(type, "EVENT")) hasEvent = true;
                else if (isTypeMatch(type, "REMINDER")) hasReminder = true;
            }
        }

        // Handle External Bills (overdue bills only show dot on Today)
        if (activeFilter.equals("ALL") || activeFilter.equals("REMINDER")) {
            for (BillsRequest b : allBills) {
                java.util.Calendar billDue = parseIsoToCalendar(b.getDueDate());
                if (billDue != null) {
                    boolean isBillOverdue = billDue.before(today);
                    if (isBillOverdue) {
                        if (isSameDay(targetDate, today)) hasReminder = true;
                    } else {
                        if (isSameDay(billDue, targetDate)) hasReminder = true;
                    }
                }
            }
        }

        // Return the correct icon based on what was found for this specific date
        if (activeFilter.equals("CHORE") && hasChore) return R.drawable.ic_dot_green;
        if (activeFilter.equals("EVENT") && hasEvent) return R.drawable.ic_dot_pink;
        if (activeFilter.equals("REMINDER") && hasReminder) return R.drawable.ic_dot_orange;

        if (hasChore && hasEvent && hasReminder) return R.drawable.ic_dots_all_three;
        if (hasChore && hasEvent) return R.drawable.ic_dots_green_pink;
        if (hasChore && hasReminder) return R.drawable.ic_dots_green_orange;
        if (hasEvent && hasReminder) return R.drawable.ic_dots_pink_orange;
        if (hasChore) return R.drawable.ic_dot_green;
        if (hasEvent) return R.drawable.ic_dot_pink;
        if (hasReminder) return R.drawable.ic_dot_orange;

        return 0;
    }

    private void toggleCalendarMode() {
        if (isCalendarExpanded) {
            calendarView.setVisibility(View.GONE);
            weekViewContainer.setVisibility(View.VISIBLE);
            imgToggleArrow.setImageResource(android.R.drawable.arrow_down_float);
        } else {
            calendarView.setVisibility(View.VISIBLE);
            weekViewContainer.setVisibility(View.GONE);
            imgToggleArrow.setImageResource(android.R.drawable.arrow_up_float);
            calendarView.post(() -> highlightMonthViewDate(currentSelectedDate));
        }
        isCalendarExpanded = !isCalendarExpanded;
    }

    private void highlightMonthViewDate(java.util.Calendar date) {
        try {
            List<java.util.Calendar> selectedDates = new ArrayList<>();
            selectedDates.add(date);
            calendarView.setDate(date);
            calendarView.setSelectedDates(selectedDates);
        } catch (Exception e) {}
    }

    private void setupFilters() {
        View.OnClickListener listener = v -> {
            filterAll.setBackgroundResource(R.drawable.bg_pill_unselected);
            filterChores.setBackgroundResource(R.drawable.bg_pill_unselected);
            filterEvents.setBackgroundResource(R.drawable.bg_pill_unselected);
            filterReminders.setBackgroundResource(R.drawable.bg_pill_unselected);

            filterAll.setTextColor(Color.BLACK);
            filterChores.setTextColor(Color.BLACK);
            filterEvents.setTextColor(Color.BLACK);
            filterReminders.setTextColor(Color.BLACK);

            if (v == filterChores) {
                activeFilter = "CHORE";
                filterChores.setBackgroundResource(R.drawable.bg_pill_green);
                filterChores.setTextColor(Color.WHITE);
            } else if (v == filterEvents) {
                activeFilter = "EVENT";
                filterEvents.setBackgroundResource(R.drawable.bg_pill_pink);
                filterEvents.setTextColor(Color.WHITE);
            } else if (v == filterReminders) {
                activeFilter = "REMINDER";
                filterReminders.setBackgroundResource(R.drawable.bg_pill_orange);
                filterReminders.setTextColor(Color.WHITE);
            } else {
                activeFilter = "ALL";
                filterAll.setBackgroundResource(R.drawable.bg_pill_purple);
                filterAll.setTextColor(Color.WHITE);
            }
            updateCalendarDots();
            setupWeekView();
            displayTasksForDate(currentSelectedDate);
        };

        filterAll.setOnClickListener(listener);
        filterChores.setOnClickListener(listener);
        filterEvents.setOnClickListener(listener);
        filterReminders.setOnClickListener(listener);
    }

    private boolean isTypeMatch(String eventType, String filter) {
        if (filter.equals("ALL")) return true;
        if (eventType == null) return false;
        if (filter.equals("CHORE")) return eventType.contains("CHORE");
        if (filter.equals("EVENT")) return eventType.contains("EVENT") || eventType.contains("MOVE") || eventType.contains("OTHER") || eventType.contains("CUSTOM");
        if (filter.equals("REMINDER")) return eventType.contains("REMINDER") || eventType.contains("BILL") || eventType.contains("MAINTENANCE");
        return false;
    }

    private void makeHeaderBold(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View child = vg.getChildAt(i);
                if (child instanceof TextView) ((TextView) child).setTypeface(null, Typeface.BOLD);
                else if (child instanceof ViewGroup) makeHeaderBold(child);
            }
        }
    }

    private boolean isSameDay(java.util.Calendar cal1, java.util.Calendar cal2) {
        return cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR) &&
                cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR);
    }
}