package com.example.uninest.ui.auth;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.applandeo.materialcalendarview.CalendarDay;
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
import com.example.uninest.notifications.LocalNotificationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TenantCalendarActivity extends AppCompatActivity {

    // UI Components
    private CalendarView calendarView;
    private LinearLayout weekViewContainer;
    private View btnToggleCalendar;
    private ImageView imgToggleArrow;
    private TextView tvCurrentDateHeader;
    private TextView tvInsightDate, tvInsightVisibleCount, tvInsightReminderCount, tvInsightOverdueCount;
    private LinearLayout emptyStateCard;
    private TextView tvEmptyTitle, tvEmptyBody;

    // Task Sections
    private LinearLayout containerChores, containerEvents, containerReminders;
    private LinearLayout sectionChores, sectionEvents, sectionReminders;
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

    private String highlightEventId;
    private String highlightMessage;

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

        highlightEventId = getIntent().getStringExtra("highlight_event_id");
        highlightMessage = getIntent().getStringExtra(LocalNotificationHelper.EXTRA_HIGHLIGHT_MESSAGE);
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
        updateFilterAccessibilityState();
        updateCalendarToggleAccessibility();
    }

    private void initViews() {
        calendarView = findViewById(R.id.calendarView);
        try {
            calendarView.setCalendarDayLayout(R.layout.item_custom_calendar_day);
            calendarView.setSelectionBackground(R.drawable.bg_calendar_selector);
            applyMonthViewTodayHighlight();
        } catch (Exception e) {
            e.printStackTrace();
        }
        makeHeaderBold(calendarView);

        weekViewContainer = findViewById(R.id.weekViewContainer);
        btnToggleCalendar = findViewById(R.id.btnToggleCalendar);
        imgToggleArrow = findViewById(R.id.imgToggleArrow);
        tvCurrentDateHeader = findViewById(R.id.tvCurrentDateHeader);
        tvInsightDate = findViewById(R.id.tvInsightDate);
        tvInsightVisibleCount = findViewById(R.id.tvInsightVisibleCount);
        tvInsightReminderCount = findViewById(R.id.tvInsightReminderCount);
        tvInsightOverdueCount = findViewById(R.id.tvInsightOverdueCount);
        emptyStateCard = findViewById(R.id.emptyStateCard);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptyBody = findViewById(R.id.tvEmptyBody);

        containerChores = findViewById(R.id.containerChores);
        containerEvents = findViewById(R.id.containerEvents);
        containerReminders = findViewById(R.id.containerReminders);
        sectionChores = findViewById(R.id.sectionChores);
        sectionEvents = findViewById(R.id.sectionEvents);
        sectionReminders = findViewById(R.id.sectionReminders);

        headerChores = findViewById(R.id.tvHeaderChores);
        headerEvents = findViewById(R.id.tvHeaderEvents);
        headerReminders = findViewById(R.id.tvHeaderReminders);

        filterAll = findViewById(R.id.filterAll);
        filterChores = findViewById(R.id.filterChores);
        filterEvents = findViewById(R.id.filterEvents);
        filterReminders = findViewById(R.id.filterReminders);

        updateFilterAccessibilityState();
        updateCalendarToggleAccessibility();
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
                } else {
                    allBills = new ArrayList<>();
                }
                refreshCalendarViews();
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
                    refreshCalendarViews();
                    if (highlightEventId != null && !highlightEventId.isBlank()) {
                        for (Calendar event : allEvents) {
                            if (highlightEventId.equals(event.getId()) && event.getStartDate() != null) {
                                java.util.Calendar selectedDate = java.util.Calendar.getInstance();
                                selectedDate.setTime(event.getStartDate().toDate());
                                currentSelectedDate = selectedDate;
                                updateDateHeader();
                                displayTasksForDate(currentSelectedDate);
                                setupWeekView();
                                highlightMonthViewDate(currentSelectedDate);
                                Toast.makeText(
                                        TenantCalendarActivity.this,
                                        highlightMessage != null && !highlightMessage.trim().isEmpty()
                                                ? highlightMessage
                                                : "View events here",
                                        Toast.LENGTH_SHORT
                                ).show();
                                highlightEventId = null;
                                highlightMessage = null;
                                break;
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Calendar>> call, Throwable t) {
                com.example.uninest.utils.NetworkErrorDialog.show(
                        TenantCalendarActivity.this,
                        TenantCalendarActivity.this::fetchRoommatesAndEvents
                );
            }
        });
    }

    private void refreshCalendarViews() {
        updateCalendarDots();
        setupWeekView();
        displayTasksForDate(currentSelectedDate);
    }

    private void applyUserFilter() {
        filteredEvents.clear();
        for (Calendar c : allEvents) {
            boolean include = false;
            String type = c.getType() != null ? c.getType() : "";

            // Shared reminder-like items are visible to the whole house.
            if (isTypeMatch(type, "REMINDER")) {
                include = true;
            } else if (isTypeMatch(type, "CHORE")) {
                if (isUserMatch(c.getAssignedTo())) {
                    include = true;
                }
            } else {
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
        int visibleItemCount = 0;
        int reminderItemCount = 0;
        int overdueItemCount = 0;

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
            if (isTypeMatch(type, "EVENT")) {
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
                boolean isOverdueChore = isTypeMatch(c.getType(), "CHORE")
                        && selectedDate.before(today);
                if (isTypeMatch(c.getType(), "CHORE")) {
                    addCard(c, containerChores, isOverdueChore);
                    hasChores = true;
                    visibleItemCount++;
                    if (isOverdueChore) overdueItemCount++;
                } else if (isTypeMatch(c.getType(), "EVENT")) {
                    addCard(c, containerEvents, false);
                    hasEvents = true;
                    visibleItemCount++;
                } else {
                    addCard(c, containerReminders, isOverdue);
                    hasReminders = true;
                    visibleItemCount++;
                    reminderItemCount++;
                    if (isOverdue) overdueItemCount++;
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
                            visibleItemCount++;
                            reminderItemCount++;
                            overdueItemCount++;
                        }
                    } else if (isSameDay(billDue, selectedDate)) {
                        // Future/today bill: show on its due date
                        addBillCard(b, containerReminders);
                        hasReminders = true;
                        visibleItemCount++;
                        reminderItemCount++;
                    }
                }
            }
        }

        sectionChores.setVisibility(hasChores ? View.VISIBLE : View.GONE);
        sectionEvents.setVisibility(hasEvents ? View.VISIBLE : View.GONE);
        sectionReminders.setVisibility(hasReminders ? View.VISIBLE : View.GONE);
        headerChores.setVisibility(hasChores ? View.VISIBLE : View.GONE);
        headerEvents.setVisibility(hasEvents ? View.VISIBLE : View.GONE);
        headerReminders.setVisibility(hasReminders ? View.VISIBLE : View.GONE);

        boolean hasAnyItems = hasChores || hasEvents || hasReminders;
        emptyStateCard.setVisibility(hasAnyItems ? View.GONE : View.VISIBLE);
        if (!hasAnyItems) {
            tvEmptyTitle.setText(activeFilter.equals("ALL") ? "Nothing scheduled here" : "No " + activeFilter.toLowerCase(Locale.US) + " for this day");
            tvEmptyBody.setText(getEmptyStateMessage(selectedDate));
        }

        updateOverviewPanel(selectedDate, visibleItemCount, reminderItemCount, overdueItemCount);
    }

    private void updateOverviewPanel(java.util.Calendar selectedDate, int visibleItemCount, int reminderItemCount, int overdueItemCount) {
        java.util.Calendar today = java.util.Calendar.getInstance();
        SimpleDateFormat overviewFmt = new SimpleDateFormat("EEEE, MMM d", Locale.US);
        String header = overviewFmt.format(selectedDate.getTime());
        if (isSameDay(selectedDate, today)) {
            header = "Today, " + new SimpleDateFormat("MMM d", Locale.US).format(selectedDate.getTime());
        }
        tvInsightDate.setText(header);
        tvInsightVisibleCount.setText(String.valueOf(visibleItemCount));
        tvInsightReminderCount.setText(String.valueOf(reminderItemCount));
        tvInsightOverdueCount.setText(String.valueOf(overdueItemCount));
    }

    private String getEmptyStateMessage(java.util.Calendar selectedDate) {
        SimpleDateFormat dateFmt = new SimpleDateFormat("MMMM d", Locale.US);
        String formattedDate = dateFmt.format(selectedDate.getTime());

        if ("CHORE".equals(activeFilter)) {
            return "No chores are assigned for " + formattedDate + ". Try another day or switch back to All.";
        }
        if ("EVENT".equals(activeFilter)) {
            return "No events are scheduled for " + formattedDate + ".";
        }
        if ("REMINDER".equals(activeFilter)) {
            return "No reminders or bills need attention for " + formattedDate + ".";
        }
        return "No chores, events, or reminders are scheduled for " + formattedDate + ". Try another day.";
    }

    private void addBillCard(BillsRequest b, ViewGroup parent) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_calendar_task_card, parent, false);
        View container = view.findViewById(R.id.cardContainer);
        TextView title = view.findViewById(R.id.tvTitle);
        TextView desc = view.findViewById(R.id.tvDesc);
        TextView extraInfo = view.findViewById(R.id.tvExtraInfo);
        TextView statusBadge = view.findViewById(R.id.popStatusBadge);
        com.google.android.material.button.MaterialButton btnMore = view.findViewById(R.id.btnViewMore);
        applyCalendarActionPill(btnMore);

        container.setBackgroundResource(R.drawable.bg_tenant_calendar_card_bill);

        title.setText(b.getTitle());
        title.setTypeface(null, Typeface.BOLD);


        statusBadge.setVisibility(View.VISIBLE);

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
        desc.setTypeface(null, Typeface.BOLD);
        desc.setText(String.format(Locale.getDefault(), "You owe EUR %.2f", myShare));
        desc.setTextSize(12f);


        java.util.Calendar dueCalendar = parseIsoToCalendar(b.getDueDate());
        String dueText = dueCalendar != null
                ? new SimpleDateFormat("MMM d", Locale.US).format(dueCalendar.getTime())
                : "Soon";

        java.util.Calendar today = java.util.Calendar.getInstance();
        today.set(java.util.Calendar.HOUR_OF_DAY, 0);
        today.set(java.util.Calendar.MINUTE, 0);
        today.set(java.util.Calendar.SECOND, 0);
        today.set(java.util.Calendar.MILLISECOND, 0);

        boolean isOverdue = false;
        boolean isDueToday = false;
        if (dueCalendar != null) {
            java.util.Calendar normalizedDue = (java.util.Calendar) dueCalendar.clone();
            normalizedDue.set(java.util.Calendar.HOUR_OF_DAY, 0);
            normalizedDue.set(java.util.Calendar.MINUTE, 0);
            normalizedDue.set(java.util.Calendar.SECOND, 0);
            normalizedDue.set(java.util.Calendar.MILLISECOND, 0);
            isOverdue = normalizedDue.before(today);
            isDueToday = isSameDay(normalizedDue, today);
        }

        if (isOverdue) {
            container.setBackgroundResource(R.drawable.bg_tenant_calendar_card_overdue);
            statusBadge.setText("OVERDUE");
            applyCalendarStatusBadge(
                    statusBadge,
                    R.drawable.bg_tenant_calendar_status_overdue_badge,
                    R.color.calendar_overdue_badge_bg,
                    R.color.calendar_overdue_badge_text
            );
        } else if (isDueToday) {
            statusBadge.setText("DUE TODAY");
            applyCalendarStatusBadge(
                    statusBadge,
                    R.drawable.bg_tenant_bill_status_due,
                    R.color.bill_due_badge_bg,
                    R.color.bill_due_badge_text
            );
        } else {
            statusBadge.setText("BILL");
            applyCalendarStatusBadge(
                    statusBadge,
                    R.drawable.bg_tenant_calendar_status_bill,
                    R.color.calendar_bill_badge_bg,
                    R.color.app_text_primary
            );
        }
        statusBadge.setTypeface(null, Typeface.BOLD);

        String creator = roommateNamesMap.get(b.getCreatorId());
        if (currentUserId.equals(b.getCreatorId())) creator = "Me";

        extraInfo.setVisibility(View.VISIBLE);
        extraInfo.setText(String.format(Locale.getDefault(),
                "Total bill: €%.2f\nCreated by: %s",
                b.getTotalAmount(),
                (creator != null ? creator : "Unknown")));
        if (isOverdue) {
            extraInfo.setText(String.format(Locale.getDefault(),
                    "Was due %s  |  Total EUR %.2f  |  %s",
                    dueText,
                    b.getTotalAmount(),
                    (creator != null ? creator : "Unknown")));
        } else if (isDueToday) {
            extraInfo.setText(String.format(Locale.getDefault(),
                    "Due today  |  Total EUR %.2f  |  %s",
                    b.getTotalAmount(),
                    (creator != null ? creator : "Unknown")));
        } else {
            extraInfo.setText(String.format(Locale.getDefault(),
                    "Due %s  |  Total EUR %.2f  |  %s",
                    dueText,
                    b.getTotalAmount(),
                    (creator != null ? creator : "Unknown")));
        }
        applyCalendarCardTextColors(title, desc, extraInfo);


        btnMore.setText("View");
        btnMore.setContentDescription("Open bill details");
        tintCalendarActionButton(btnMore, R.color.calendar_primary_dark, R.color.white);

        btnMore.setOnClickListener(v -> {
            startActivity(new Intent(this, TenantBillsActivity.class));
        });

        container.setContentDescription(buildCardContentDescription(
                "Bill reminder",
                title.getText(),
                extraInfo.getText(),
                desc.getVisibility() == View.VISIBLE ? desc.getText() : null,
                "Double tap to open bills."
        ));

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
        applyCalendarActionPill(btnMore);

        title.setText(c.getTitle());
        String dVal = c.getDescription() != null ? c.getDescription() : "";
        desc.setText(dVal);
        boolean hasDescription = !dVal.trim().isEmpty();
        desc.setVisibility(hasDescription ? View.VISIBLE : View.GONE);
        applyCalendarCardTextColors(title, desc, extraInfo);

        String type = c.getType() != null ? c.getType() : "";
        boolean isBillItem = isBillCategory(type);

        if (isTypeMatch(type, "CHORE")) {
            // --- CHORE LOGIC ---
            container.setBackgroundResource(R.drawable.bg_tenant_calendar_card_chore);
            desc.setVisibility(View.GONE);
            String creatorName = roommateNamesMap.get(c.getCreatedBy());
            if (isUserMatch(c.getCreatedBy())) creatorName = "Me";
            if (creatorName == null) creatorName = "Unknown";

            String assigneeName = roommateNamesMap.get(c.getAssignedTo());
            if (isUserMatch(c.getAssignedTo())) assigneeName = "Me";
            if (assigneeName == null) assigneeName = "Unassigned";

            extraInfo.setText("By " + creatorName + "  |  Assigned " + assigneeName);
            extraInfo.setVisibility(View.VISIBLE);

            String status = c.getStatus() != null ? c.getStatus() : "NOT_STARTED";
            tvStatus.setVisibility(View.VISIBLE);

            if (isOverdue) {
                tvStatus.setText("OVERDUE");
                applyCalendarStatusBadge(
                        tvStatus,
                        R.drawable.bg_tenant_calendar_status_overdue_badge,
                        R.color.calendar_overdue_badge_bg,
                        R.color.calendar_overdue_badge_text
                );
            } else if (status.equals("COMPLETED")) {
                tvStatus.setText(formatStatus(status));
                applyCalendarStatusBadge(
                        tvStatus,
                        R.drawable.bg_tenant_calendar_status_chore_completed,
                        R.color.calendar_chore_status_completed_bg,
                        R.color.calendar_chore_status_completed_text
                );
            } else if (status.equals("IN_PROGRESS")) {
                tvStatus.setText(formatStatus(status));
                applyCalendarStatusBadge(
                        tvStatus,
                        R.drawable.bg_tenant_calendar_status_chore_progress,
                        R.color.calendar_chore_status_progress_bg,
                        R.color.calendar_chore_status_progress_text
                );
            } else {
                tvStatus.setText(formatStatus(status));
                applyCalendarStatusBadge(
                        tvStatus,
                        R.drawable.bg_tenant_calendar_status_chore_pending,
                        R.color.calendar_chore_status_pending_bg,
                        R.color.calendar_chore_status_pending_text
                );
            }

            final String finalCreator = creatorName;
            final String finalAssignee = assigneeName;
            btnMore.setVisibility(View.GONE);

            container.setOnClickListener(v -> {
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
            container.setBackgroundResource(R.drawable.bg_tenant_calendar_card_event);
            desc.setVisibility(View.GONE);
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText("EVENT");
            applyCalendarStatusBadge(
                    tvStatus,
                    R.drawable.bg_tenant_calendar_status_event,
                    R.color.calendar_event_badge_bg,
                    R.color.calendar_text_inverse
            );
            btnMore.setVisibility(View.GONE);

            if (c.getStartDate() != null) {
                SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm a", Locale.US);
                extraInfo.setText(timeFmt.format(c.getStartDate().toDate()));
                extraInfo.setVisibility(View.VISIBLE);
            }

            // Click the card to view details
            container.setOnClickListener(v -> showEventDetailsMiniModal(c));
        } else {
            // --- REMINDER / STICKY LOGIC ---
            container.setBackgroundResource(isBillItem
                    ? R.drawable.bg_tenant_calendar_card_bill
                    : R.drawable.bg_tenant_calendar_card_reminder);
            extraInfo.setTypeface(extraInfo.getTypeface(), Typeface.BOLD);
            extraInfo.setTextSize(12f);
            desc.setVisibility(View.GONE);

            // Show OVERDUE badge if the reminder is from a past date
            if (isOverdue && c.getStartDate() != null) {
                container.setBackgroundResource(R.drawable.bg_tenant_calendar_card_overdue);
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText("OVERDUE");
                applyCalendarStatusBadge(
                        tvStatus,
                        R.drawable.bg_tenant_calendar_status_overdue_badge,
                        R.color.calendar_overdue_badge_bg,
                        R.color.calendar_overdue_badge_text
                );
                tvStatus.setTypeface(null, Typeface.BOLD);
                tvStatus.setTextSize(11f);
                int padH = (int) (10 * getResources().getDisplayMetrics().density);
                int padV = (int) (3 * getResources().getDisplayMetrics().density);
                tvStatus.setPadding(padH, padV, padH, padV);
            } else {
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText(isBillItem ? "BILL" : "REMINDER");
                applyCalendarStatusBadge(
                        tvStatus,
                        isBillItem ? R.drawable.bg_tenant_calendar_status_bill : R.drawable.bg_tenant_calendar_status_reminder,
                        isBillItem ? R.color.calendar_bill_badge_bg : R.color.calendar_reminder_badge_bg,
                        R.color.app_text_primary
                );
            }

            // Transform the button into a "Done" action
            btnMore.setVisibility(View.VISIBLE);
            btnMore.setText("Done");
            btnMore.setContentDescription("Mark reminder as done");
            tintCalendarActionButton(
                    btnMore,
                    R.color.calendar_primary_dark,
                    R.color.white
            );
            btnMore.setStrokeWidth(0);

            if (c.getAmount() != null && c.getAmount() > 0) {
                extraInfo.setText(String.format(Locale.US, "Amount: €%.2f", c.getAmount()));
                extraInfo.setVisibility(View.VISIBLE);
            } else {
                extraInfo.setVisibility(View.GONE);
            }

            SimpleDateFormat reminderFmt = new SimpleDateFormat("MMM d", Locale.US);
            String dueLabel = c.getStartDate() != null
                    ? ((isOverdue ? "Was due " : "Due ") + reminderFmt.format(c.getStartDate().toDate()))
                    : (isOverdue ? "Overdue" : "Upcoming");
            if (c.getAmount() != null && c.getAmount() > 0) {
                extraInfo.setText(String.format(Locale.US, "%s  |  EUR %.2f", dueLabel, c.getAmount()));
            } else {
                extraInfo.setText(dueLabel);
            }
            extraInfo.setVisibility(View.VISIBLE);

            // Click logic to mark the reminder as completed
            btnMore.setOnClickListener(v -> markReminderAsDone(c));

            // Click the card to view details
            container.setOnClickListener(v -> showEventDetailsMiniModal(c));
        }

        String actionHint = isTypeMatch(type, "CHORE")
                ? "Double tap to view chore details."
                : isTypeMatch(type, "EVENT")
                ? "Double tap to view event details."
                : isBillItem
                ? "Double tap to view bill details."
                : "Double tap to view reminder details.";
        CharSequence badgeText = tvStatus.getVisibility() == View.VISIBLE ? tvStatus.getText() : null;
        container.setContentDescription(buildCardContentDescription(
                badgeText != null ? badgeText.toString() : type,
                title.getText(),
                extraInfo.getVisibility() == View.VISIBLE ? extraInfo.getText() : null,
                desc.getVisibility() == View.VISIBLE ? desc.getText() : null,
                actionHint
        ));

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
                com.example.uninest.utils.NetworkErrorDialog.show(
                        TenantCalendarActivity.this,
                        () -> markReminderAsDone(c)
                );
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
        TextView tvAmountLabel = dialog.findViewById(R.id.popAmountLabel);
        TextView tvAmount = dialog.findViewById(R.id.popAmount);
        Button btnClose = dialog.findViewById(R.id.btnPopClose);

        tvTitle.setText(c.getTitle());

        // Type Badge
        String type = c.getType() != null ? c.getType() : "EVENT";
        tvTypeBadge.setText(type.toUpperCase());
        if (isBillCategory(type)) {
            applyCalendarStatusBadge(
                    tvTypeBadge,
                    R.drawable.bg_tenant_calendar_status_bill,
                    R.color.calendar_bill_badge_bg,
                    R.color.app_text_primary
            );
        } else if (type.contains("REMINDER") || type.contains("MAINTENANCE")) {
            applyCalendarStatusBadge(
                    tvTypeBadge,
                    R.drawable.bg_tenant_calendar_status_reminder,
                    R.color.calendar_reminder_badge_bg,
                    R.color.app_text_primary
            );
        } else {
            applyCalendarStatusBadge(
                    tvTypeBadge,
                    R.drawable.bg_tenant_calendar_status_event,
                    R.color.calendar_event_badge_bg,
                    R.color.calendar_text_inverse
            );
        }

        // Description
        if (c.getDescription() != null && !c.getDescription().isEmpty()) {
            tvDesc.setText(c.getDescription());
            if (isTypeMatch(type, "REMINDER")) {
                tvDesc.setTextSize(16f);
                tvDesc.setTypeface(null, Typeface.BOLD);
            } else {
                tvDesc.setTextSize(14f);
                tvDesc.setTypeface(null, Typeface.NORMAL);
            }
        } else {
            tvDesc.setText("No description provided.");
            tvDesc.setTypeface(null, Typeface.ITALIC);
            if (isTypeMatch(type, "REMINDER")) {
                tvDesc.setTextSize(16f);
            } else {
                tvDesc.setTextSize(14f);
            }
        }

        if (tvAmountLabel != null) {
            int amountColorRes = isBillCategory(type)
                    ? R.color.calendar_bill_text
                    : R.color.calendar_reminder_text;
            tvAmountLabel.setTextColor(ContextCompat.getColor(this, amountColorRes));
            tvAmount.setTextColor(ContextCompat.getColor(this, amountColorRes));
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

    private void applyCalendarActionPill(com.google.android.material.button.MaterialButton button) {
        if (button == null) {
            return;
        }

        android.view.ViewGroup.LayoutParams layoutParams = button.getLayoutParams();
        if (layoutParams != null) {
            layoutParams.width = Math.round(dpToPx(84));
            layoutParams.height = Math.round(dpToPx(28));
            button.setLayoutParams(layoutParams);
        }

        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setCornerRadius(Math.round(dpToPx(14)));
        button.setTypeface(button.getTypeface(), Typeface.BOLD);
        button.setTextSize(12f);
    }

    private void applyCalendarCardTextColors(TextView... textViews) {
        int textColor = ContextCompat.getColor(this, R.color.app_text_primary);
        for (TextView textView : textViews) {
            if (textView != null) {
                textView.setTextColor(textColor);
            }
        }
    }

    private void applyCalendarStatusBadge(TextView badge,
                                          int backgroundRes,
                                          int darkModeBadgeColorRes,
                                          int lightModeTextColorRes) {
        if (badge == null) {
            return;
        }

        badge.setBackgroundResource(backgroundRes);
        boolean darkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;

        if (darkMode) {
            int badgeColor = ContextCompat.getColor(this, darkModeBadgeColorRes);
            int darkText = ContextCompat.getColor(this, R.color.calendar_text_inverse);
            int lightText = ContextCompat.getColor(this, R.color.white);
            badge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    badgeColor
            ));
            badge.setTextColor(ColorUtils.calculateContrast(darkText, badgeColor) >= 4.5
                    ? darkText
                    : lightText);
        } else {
            badge.setBackgroundTintList(null);
            badge.setTextColor(ContextCompat.getColor(this, lightModeTextColorRes));
        }
    }

    private void tintCalendarActionButton(com.google.android.material.button.MaterialButton button,
                                          int backgroundColorRes,
                                          int textColorRes) {
        if (button == null) {
            return;
        }

        int backgroundColor = ContextCompat.getColor(this, backgroundColorRes);
        int preferredTextColor = ContextCompat.getColor(this, textColorRes);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(backgroundColor));
        button.setTextColor(getReadableCalendarForeground(backgroundColor, preferredTextColor));
    }

    private int getReadableCalendarForeground(int backgroundColor, int preferredTextColor) {
        if (ColorUtils.calculateContrast(preferredTextColor, backgroundColor) >= 4.5) {
            return preferredTextColor;
        }

        int lightText = ContextCompat.getColor(this, R.color.white);
        int darkText = ContextCompat.getColor(this, R.color.black);
        return ColorUtils.calculateContrast(darkText, backgroundColor)
                >= ColorUtils.calculateContrast(lightText, backgroundColor)
                ? darkText
                : lightText;
    }

    private void updateDateHeader() {
        java.util.Calendar today = java.util.Calendar.getInstance();
        SimpleDateFormat selectedFmt = new SimpleDateFormat("EEE, MMM d", Locale.US);
        SimpleDateFormat todayFmt = new SimpleDateFormat("MMM d", Locale.US);

        if (isSameDay(currentSelectedDate, today)) {
            tvCurrentDateHeader.setText("Today, " + todayFmt.format(currentSelectedDate.getTime()));
            updateCalendarLabel("Viewing today");
        } else {
            tvCurrentDateHeader.setText(selectedFmt.format(currentSelectedDate.getTime()));
            updateCalendarLabel("Viewing");
        }

        updateCalendarToggleAccessibility();
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

            String simplifiedType = simplifyDotType(rawType);

            if (simplifiedType.isEmpty()) continue;

            java.util.Calendar eventDate = java.util.Calendar.getInstance();
            eventDate.setTime(c.getStartDate().toDate());

            boolean isWeekly = false;
            boolean isMonthly = false;
            if (c.getRecurrence() != null && c.getRecurrence().getFrequency() != null) {
                if ("WEEKLY".equalsIgnoreCase(c.getRecurrence().getFrequency())) isWeekly = true;
                else if ("MONTHLY".equalsIgnoreCase(c.getRecurrence().getFrequency())) isMonthly = true;
            }

            // Sticky reminder- and bill-style items move to today's dot once overdue.
            boolean isStickyOverdue = false;
            if (!isTypeMatch(rawType, "CHORE") && !isTypeMatch(rawType, "EVENT")) {
                java.util.Calendar normalizedStart = java.util.Calendar.getInstance();
                normalizedStart.setTime(c.getStartDate().toDate());
                normalizedStart.set(java.util.Calendar.HOUR_OF_DAY, 0);
                normalizedStart.set(java.util.Calendar.MINUTE, 0);
                normalizedStart.set(java.util.Calendar.SECOND, 0);
                normalizedStart.set(java.util.Calendar.MILLISECOND, 0);
                isStickyOverdue = normalizedStart.before(today);
            }

            if (!isStickyOverdue) {
                do {
                    String dateKey = sdf.format(eventDate.getTime());
                    if (!dailyEventsMap.containsKey(dateKey)) {
                        dailyEventsMap.put(dateKey, new LinkedHashSet<>());
                        dateObjectMap.put(dateKey, (java.util.Calendar) eventDate.clone());
                    }
                    addDotType(dailyEventsMap.get(dateKey), simplifiedType);

                    if (isWeekly) eventDate.add(java.util.Calendar.DAY_OF_MONTH, 7);
                    else if (isMonthly) eventDate.add(java.util.Calendar.MONTH, 1);
                } while ((isWeekly || isMonthly) && eventDate.before(limitDate));
            }

            if (isStickyOverdue) {
                String todayKey = sdf.format(today.getTime());
                if (!dailyEventsMap.containsKey(todayKey)) {
                    dailyEventsMap.put(todayKey, new LinkedHashSet<>());
                    dateObjectMap.put(todayKey, (java.util.Calendar) today.clone());
                }
                addDotType(dailyEventsMap.get(todayKey), simplifiedType);
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
                    dailyEventsMap.put(dateKey, new LinkedHashSet<>());
                    dateObjectMap.put(dateKey, dotDate);
                }
                addDotType(dailyEventsMap.get(dateKey), "BILL");
            }
        }

        for (String dateKey : dailyEventsMap.keySet()) {
            java.util.Set<String> types = dailyEventsMap.get(dateKey);
            java.util.Calendar cal = dateObjectMap.get(dateKey);
            Drawable dotDrawable = buildCalendarDotDrawable(types);
            if (dotDrawable != null) {
                mapEvents.add(new EventDay(cal, dotDrawable));
            }
        }
        calendarView.setEvents(mapEvents);
        applyMonthViewTodayHighlight();
        highlightMonthViewDate(currentSelectedDate);
    }

    private void applyMonthViewTodayHighlight() {
        java.util.Calendar today = java.util.Calendar.getInstance();
        today.set(java.util.Calendar.HOUR_OF_DAY, 0);
        today.set(java.util.Calendar.MINUTE, 0);
        today.set(java.util.Calendar.SECOND, 0);
        today.set(java.util.Calendar.MILLISECOND, 0);

        CalendarDay todayDay = new CalendarDay(today);
        todayDay.setBackgroundResource(R.drawable.bg_calendar_ring);
        calendarView.setCalendarDays(java.util.Collections.singletonList(todayDay));
    }

    private void addDotType(Set<String> types, String dotType) {
        if (types == null || dotType == null || dotType.isEmpty()) {
            return;
        }
        types.add(dotType);
    }

    private String simplifyDotType(String rawType) {
        if (rawType == null) {
            return "";
        }
        if (isTypeMatch(rawType, "CHORE")) {
            return "CHORE";
        }
        if (isTypeMatch(rawType, "EVENT")) {
            return "EVENT";
        }
        if (isBillCategory(rawType)) {
            return "BILL";
        }
        if (isTypeMatch(rawType, "REMINDER")) {
            return "REMINDER";
        }
        return "";
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

            boolean isSelected = isSameDay(cal, currentSelectedDate);
            boolean isToday = isSameDay(cal, today);

            tvName.setText(isToday ? "Today" : sdfName.format(cal.getTime()));
            tvNumber.setText(sdfDay.format(cal.getTime()));

            if (isSelected) {
                tvNumber.setBackgroundResource(R.drawable.bg_calendar_selector);
                tvNumber.setTextColor(ContextCompat.getColor(this, R.color.calendar_text_inverse));
                tvName.setTextColor(ContextCompat.getColor(this, R.color.calendar_text_primary));
                tvName.setTypeface(null, Typeface.BOLD);
            } else if (isToday) {
                tvNumber.setBackgroundResource(R.drawable.bg_calendar_ring);
                tvNumber.setTextColor(ContextCompat.getColor(this, R.color.calendar_text_primary));
                tvName.setTextColor(ContextCompat.getColor(this, R.color.calendar_text_primary));
                tvName.setTypeface(null, Typeface.BOLD);
            } else {
                tvNumber.setBackgroundResource(R.drawable.bg_tenant_calendar_day_outline);
                tvNumber.setTextColor(ContextCompat.getColor(this, R.color.calendar_text_primary));
                tvName.setTextColor(ContextCompat.getColor(this, R.color.calendar_text_secondary));
                tvName.setTypeface(null, Typeface.NORMAL);
            }

            Set<String> dotTypes = getDotTypesForDate(cal);
            Drawable dotDrawable = buildCalendarDotDrawable(dotTypes);
            if (dotDrawable != null) {
                imgDot.setImageDrawable(dotDrawable);
                imgDot.setVisibility(View.VISIBLE);
            } else {
                imgDot.setVisibility(View.INVISIBLE);
            }

            java.util.Calendar clickedDay = (java.util.Calendar) cal.clone();
            dayItem.setFocusable(true);
            dayItem.setContentDescription(buildWeekDayContentDescription(
                    clickedDay,
                    isSelected,
                    isToday,
                    !dotTypes.isEmpty()
            ));
            dayItem.setOnClickListener(v -> {
                currentSelectedDate = clickedDay;
                setupWeekView();
                displayTasksForDate(currentSelectedDate);
                updateDateHeader();
                highlightMonthViewDate(clickedDay);
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            dayItem.setLayoutParams(params);
            weekViewContainer.addView(dayItem);
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }
    }

    private Set<String> getDotTypesForDate(java.util.Calendar targetDate) {
        Set<String> dotTypes = new LinkedHashSet<>();

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
            if (isTypeMatch(type, "EVENT")) {
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
                addDotType(dotTypes, simplifyDotType(type));
            }
        }

        // Handle external bills with their own bill marker.
        if (activeFilter.equals("ALL") || activeFilter.equals("REMINDER")) {
            for (BillsRequest b : allBills) {
                java.util.Calendar billDue = parseIsoToCalendar(b.getDueDate());
                if (billDue != null) {
                    boolean isBillOverdue = billDue.before(today);
                    if (isBillOverdue) {
                        if (isSameDay(targetDate, today)) addDotType(dotTypes, "BILL");
                    } else {
                        if (isSameDay(billDue, targetDate)) addDotType(dotTypes, "BILL");
                    }
                }
            }
        }

        return dotTypes;
    }

    private Drawable buildCalendarDotDrawable(Set<String> dotTypes) {
        if (dotTypes == null || dotTypes.isEmpty()) {
            return null;
        }

        List<Integer> colors = new ArrayList<>();
        if (dotTypes.contains("CHORE")) {
            colors.add(ContextCompat.getColor(this, R.color.calendar_dot_chore));
        }
        if (dotTypes.contains("EVENT")) {
            colors.add(ContextCompat.getColor(this, R.color.calendar_dot_event));
        }
        if (dotTypes.contains("REMINDER") || dotTypes.contains("BILL")) {
            colors.add(ContextCompat.getColor(this, R.color.calendar_dot_reminder));
        }

        if (colors.isEmpty()) {
            return null;
        }

        int dotSize = Math.round(dpToPx(8));
        int gap = Math.round(dpToPx(4));
        int width = dotSize + ((colors.size() - 1) * (dotSize + gap));

        Bitmap bitmap = Bitmap.createBitmap(width, dotSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float radius = dotSize / 2f;

        for (int i = 0; i < colors.size(); i++) {
            paint.setColor(colors.get(i));
            float centerX = radius + (i * (dotSize + gap));
            canvas.drawCircle(centerX, radius, radius, paint);
        }

        BitmapDrawable drawable = new BitmapDrawable(getResources(), bitmap);
        drawable.setBounds(0, 0, width, dotSize);
        return drawable;
    }

    private void toggleCalendarMode() {
        if (isCalendarExpanded) {
            collapseMonthCalendar();
        } else {
            expandMonthCalendar();
        }
        isCalendarExpanded = !isCalendarExpanded;
        updateCalendarToggleAccessibility();
    }

    private void expandMonthCalendar() {
        weekViewContainer.animate().cancel();
        calendarView.animate().cancel();
        highlightMonthViewDate(currentSelectedDate);

        calendarView.setVisibility(View.VISIBLE);
        calendarView.setAlpha(0f);
        calendarView.setTranslationY(-dpToPx(8));
        calendarView.setScaleY(0.985f);

        imgToggleArrow.animate()
                .rotation(180f)
                .setDuration(160)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        calendarView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (calendarView.getViewTreeObserver().isAlive()) {
                    calendarView.getViewTreeObserver().removeOnPreDrawListener(this);
                }

                weekViewContainer.animate()
                        .alpha(0f)
                        .translationY(dpToPx(6))
                        .setDuration(90)
                        .setInterpolator(new DecelerateInterpolator())
                        .withEndAction(() -> {
                            weekViewContainer.setVisibility(View.GONE);
                            weekViewContainer.setAlpha(1f);
                            weekViewContainer.setTranslationY(0f);
                        })
                        .start();

                calendarView.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .scaleY(1f)
                        .setDuration(140)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
                return true;
            }
        });
    }

    private void collapseMonthCalendar() {
        weekViewContainer.animate().cancel();
        calendarView.animate().cancel();

        if (calendarView.getVisibility() != View.VISIBLE) {
            calendarView.setVisibility(View.GONE);
            weekViewContainer.setVisibility(View.VISIBLE);
            imgToggleArrow.setRotation(0f);
            return;
        }

        calendarView.animate()
                .alpha(0f)
                .translationY(-dpToPx(8))
                .scaleY(0.96f)
                .setDuration(140)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    calendarView.setVisibility(View.GONE);
                    calendarView.setScaleY(1f);
                    calendarView.setAlpha(1f);
                    calendarView.setTranslationY(0f);
                    weekViewContainer.setAlpha(0f);
                    weekViewContainer.setTranslationY(dpToPx(6));
                    weekViewContainer.setVisibility(View.VISIBLE);
                    weekViewContainer.animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(140)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                })
                .start();

        imgToggleArrow.animate()
                .rotation(0f)
                .setDuration(140)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void highlightMonthViewDate(java.util.Calendar date) {
        try {
            java.util.Calendar currentPage = calendarView.getCurrentPageDate();
            boolean samePage = currentPage.get(java.util.Calendar.YEAR) == date.get(java.util.Calendar.YEAR)
                    && currentPage.get(java.util.Calendar.MONTH) == date.get(java.util.Calendar.MONTH);

            boolean sameSelected = false;
            try {
                java.util.Calendar selected = calendarView.getFirstSelectedDate();
                sameSelected = isSameDay(selected, date);
            } catch (Exception ignored) {}

            if (samePage && sameSelected) {
                return;
            }

            calendarView.setDate((java.util.Calendar) date.clone());
        } catch (Exception e) {}
    }

    private void setupFilters() {
        View.OnClickListener listener = v -> {
            int defaultColor = ContextCompat.getColor(this, R.color.calendar_text_secondary);
            filterAll.setBackgroundResource(R.drawable.bg_tenant_calendar_filter_neutral);
            filterChores.setBackgroundResource(R.drawable.bg_tenant_calendar_filter_neutral);
            filterEvents.setBackgroundResource(R.drawable.bg_tenant_calendar_filter_neutral);
            filterReminders.setBackgroundResource(R.drawable.bg_tenant_calendar_filter_neutral);

            filterAll.setTextColor(defaultColor);
            filterChores.setTextColor(defaultColor);
            filterEvents.setTextColor(defaultColor);
            filterReminders.setTextColor(defaultColor);

            if (v == filterChores) {
                activeFilter = "CHORE";
                filterChores.setBackgroundResource(R.drawable.bg_tenant_calendar_filter_chore);
                filterChores.setTextColor(ContextCompat.getColor(this, R.color.calendar_text_inverse));
            } else if (v == filterEvents) {
                activeFilter = "EVENT";
                filterEvents.setBackgroundResource(R.drawable.bg_tenant_calendar_filter_event);
                filterEvents.setTextColor(ContextCompat.getColor(this, R.color.calendar_text_inverse));
            } else if (v == filterReminders) {
                activeFilter = "REMINDER";
                filterReminders.setBackgroundResource(R.drawable.bg_tenant_calendar_filter_reminder);
                filterReminders.setTextColor(ContextCompat.getColor(this, R.color.calendar_text_inverse));
            } else {
                activeFilter = "ALL";
                filterAll.setBackgroundResource(R.drawable.bg_tenant_calendar_filter_all);
                filterAll.setTextColor(ContextCompat.getColor(this, R.color.calendar_text_inverse));
            }
            updateFilterAccessibilityState();
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

    private boolean isBillCategory(String type) {
        return type != null && type.toUpperCase(Locale.getDefault()).contains("BILL");
    }

    private void updateCalendarLabel(String value) {
        TextView label = findViewById(R.id.tvCalendarLabel);
        if (label != null) {
            label.setText(value);
        }
    }

    private void updateFilterAccessibilityState() {
        updateFilterChip(filterAll, "All", "ALL".equals(activeFilter));
        updateFilterChip(filterChores, "Chores", "CHORE".equals(activeFilter));
        updateFilterChip(filterEvents, "Events", "EVENT".equals(activeFilter));
        updateFilterChip(filterReminders, "Reminders", "REMINDER".equals(activeFilter));
    }

    private void updateFilterChip(TextView chip, String label, boolean selected) {
        if (chip == null) {
            return;
        }

        chip.setSelected(selected);
        chip.setContentDescription(label + " filter" + (selected ? ", selected" : ""));
    }

    private void updateCalendarToggleAccessibility() {
        if (btnToggleCalendar == null) {
            return;
        }

        String dateLabel = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(currentSelectedDate.getTime());
        String action = isCalendarExpanded ? "Collapse month calendar" : "Expand month calendar";
        btnToggleCalendar.setContentDescription(action + ". Selected date " + dateLabel + ".");
    }

    private String buildCardContentDescription(String category, CharSequence title, CharSequence detail,
                                               CharSequence description, String actionHint) {
        StringBuilder builder = new StringBuilder();
        if (category != null && !category.trim().isEmpty()) {
            builder.append(category).append(". ");
        }
        if (title != null && title.length() > 0) {
            builder.append(title).append(". ");
        }
        if (detail != null && detail.length() > 0) {
            builder.append(detail).append(". ");
        }
        if (description != null && description.length() > 0) {
            builder.append(description).append(". ");
        }
        if (actionHint != null && !actionHint.isEmpty()) {
            builder.append(actionHint);
        }
        return builder.toString().trim();
    }

    private String buildWeekDayContentDescription(java.util.Calendar day,
                                                  boolean isSelected,
                                                  boolean isToday,
                                                  boolean hasScheduledItems) {
        StringBuilder builder = new StringBuilder();
        builder.append(new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(day.getTime()));
        if (isToday) {
            builder.append(", today");
        }
        if (isSelected) {
            builder.append(", selected");
        }
        if (hasScheduledItems) {
            builder.append(", has scheduled items");
        }
        return builder.toString();
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

    private float dpToPx(int dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
