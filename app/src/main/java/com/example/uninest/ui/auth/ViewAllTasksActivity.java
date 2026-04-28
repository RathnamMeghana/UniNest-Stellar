package com.example.uninest.ui.auth;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.Calendar;
import com.example.uninest.model.User;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewAllTasksActivity extends AppCompatActivity {

    private static final String FILTER_ALL_USERS = "ALL";
    private static final String SECTION_ALL = "ALL";
    private static final String SECTION_ONCE = "ONCE";
    private static final String SECTION_WEEKLY = "WEEKLY";
    private static final String SECTION_MONTHLY = "MONTHLY";
    private static final String SECTION_HISTORY = "HISTORY";

    private RecyclerView rvRoommateFilter;
    private LinearLayout containerOnce;
    private LinearLayout containerWeekly;
    private LinearLayout containerMonthly;
    private LinearLayout containerHistory;
    private LinearLayout layoutHistoryHeader;
    private LinearLayout emptyStateCard;
    private TextView tvHeaderOnce;
    private TextView tvHeaderWeekly;
    private TextView tvHeaderMonthly;
    private TextView tvHeaderHistory;
    private TextView tvEmptyTitle;
    private TextView tvEmptyBody;
    private TextView filterSectionAll;
    private TextView filterSectionOnce;
    private TextView filterSectionWeekly;
    private TextView filterSectionMonthly;
    private TextView filterSectionHistory;
    private String highlightTaskId;
    private SessionManager sessionManager;
    private String houseCode;
    private String currentUserId;
    private List<Calendar> allTasks = new ArrayList<>();
    private final List<User> filterList = new ArrayList<>();
    private final Map<String, String> roommateNameMap = new HashMap<>();
    private final Map<String, String> roommateImageMap = new HashMap<>();
    private String selectedUserId = FILTER_ALL_USERS;
    private String selectedSection = SECTION_ALL;
    private FilterAdapter filterAdapter;
    private MaterialButton btnToggleHistory;
    private boolean isHistoryExpanded = false;
    private boolean tasksUnavailable = false;

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
        layoutHistoryHeader = findViewById(R.id.layoutHistoryHeader);
        emptyStateCard = findViewById(R.id.emptyStateCard);
        tvHeaderOnce = findViewById(R.id.tvHeaderOnce);
        tvHeaderWeekly = findViewById(R.id.tvHeaderWeekly);
        tvHeaderMonthly = findViewById(R.id.tvHeaderMonthly);
        tvHeaderHistory = findViewById(R.id.tvHeaderHistory);
        btnToggleHistory = findViewById(R.id.btnToggleHistory);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptyBody = findViewById(R.id.tvEmptyBody);
        filterSectionAll = findViewById(R.id.filterSectionAll);
        filterSectionOnce = findViewById(R.id.filterSectionOnce);
        filterSectionWeekly = findViewById(R.id.filterSectionWeekly);
        filterSectionMonthly = findViewById(R.id.filterSectionMonthly);
        filterSectionHistory = findViewById(R.id.filterSectionHistory);
        highlightTaskId = getIntent().getStringExtra("highlight_task_id");

        rvRoommateFilter.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        filterAdapter = new FilterAdapter();
        rvRoommateFilter.setAdapter(filterAdapter);
        btnToggleHistory.setOnClickListener(v -> {
            isHistoryExpanded = !isHistoryExpanded;
            updateUi();
        });

        setupSectionFilters();
        updateSectionFilterUi();
        updateEmptyState(0);
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
                roommateNameMap.clear();
                roommateImageMap.clear();
                filterList.clear();

                User all = new User();
                all.setId(FILTER_ALL_USERS);
                all.setFirstName("All");
                filterList.add(all);

                User you = new User();
                you.setId(currentUserId);
                you.setFirstName("You");
                you.setProfileImageUrl(sessionManager.getUserImage());
                filterList.add(you);

                if (response.isSuccessful() && response.body() != null) {
                    for (User user : response.body()) {
                        roommateNameMap.put(user.getId(), user.getFullName());
                        roommateImageMap.put(user.getId(), user.getProfileImageUrl());
                        if (!currentUserId.equals(user.getId())) {
                            filterList.add(user);
                        }
                    }
                }

                roommateImageMap.put(currentUserId, sessionManager.getUserImage());
                filterAdapter.notifyDataSetChanged();
                loadTasks();
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                filterList.clear();

                User all = new User();
                all.setId(FILTER_ALL_USERS);
                all.setFirstName("All");
                filterList.add(all);

                User you = new User();
                you.setId(currentUserId);
                you.setFirstName("You");
                you.setProfileImageUrl(sessionManager.getUserImage());
                filterList.add(you);

                roommateImageMap.put(currentUserId, sessionManager.getUserImage());
                filterAdapter.notifyDataSetChanged();
                loadTasks();
            }
        });
    }

    private void loadTasks() {
        ApiClient.getCalendarApi().getByApartment(houseCode).enqueue(new Callback<List<Calendar>>() {
            @Override
            public void onResponse(Call<List<Calendar>> call, Response<List<Calendar>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tasksUnavailable = false;
                    allTasks = new ArrayList<>(response.body());
                    filterAdapter.notifyDataSetChanged();
                    updateUi();
                } else {
                    tasksUnavailable = true;
                    allTasks = new ArrayList<>();
                    filterAdapter.notifyDataSetChanged();
                    updateUi();
                }
            }

            @Override
            public void onFailure(Call<List<Calendar>> call, Throwable t) {
                tasksUnavailable = true;
                allTasks = new ArrayList<>();
                filterAdapter.notifyDataSetChanged();
                updateUi();
            }
        });
    }

    private void setupSectionFilters() {
        bindSectionFilter(filterSectionAll, SECTION_ALL);
        bindSectionFilter(filterSectionOnce, SECTION_ONCE);
        bindSectionFilter(filterSectionWeekly, SECTION_WEEKLY);
        bindSectionFilter(filterSectionMonthly, SECTION_MONTHLY);
        bindSectionFilter(filterSectionHistory, SECTION_HISTORY);
    }

    private void bindSectionFilter(TextView view, String section) {
        view.setOnClickListener(v -> {
            if (!section.equals(selectedSection)) {
                selectedSection = section;
                isHistoryExpanded = SECTION_HISTORY.equals(section);
                updateSectionFilterUi();
                updateUi();
            }
        });
    }

    private void updateSectionFilterUi() {
        applySectionChipState(filterSectionAll, SECTION_ALL.equals(selectedSection));
        applySectionChipState(filterSectionOnce, SECTION_ONCE.equals(selectedSection));
        applySectionChipState(filterSectionWeekly, SECTION_WEEKLY.equals(selectedSection));
        applySectionChipState(filterSectionMonthly, SECTION_MONTHLY.equals(selectedSection));
        applySectionChipState(filterSectionHistory, SECTION_HISTORY.equals(selectedSection));
    }

    private void applySectionChipState(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected
                ? R.drawable.bg_tenant_calendar_filter_all
                : R.drawable.bg_tenant_calendar_filter_neutral);
        chip.setTextColor(ContextCompat.getColor(this, selected
                ? R.color.calendar_text_inverse
                : R.color.calendar_text_secondary));
    }

    private void updateUi() {
        clearSection(containerOnce, tvHeaderOnce);
        clearSection(containerWeekly, tvHeaderWeekly);
        clearSection(containerMonthly, tvHeaderMonthly);
        clearSection(containerHistory, tvHeaderHistory);
        layoutHistoryHeader.setVisibility(View.GONE);
        btnToggleHistory.setVisibility(View.GONE);
        containerHistory.setVisibility(View.GONE);

        List<Calendar> onceTasks = new ArrayList<>();
        List<Calendar> weeklyTasks = new ArrayList<>();
        List<Calendar> monthlyTasks = new ArrayList<>();
        List<Calendar> historyTasks = new ArrayList<>();
        boolean matchedHighlightedTask = false;

        for (Calendar task : allTasks) {
            if (!"CHORE".equalsIgnoreCase(safeText(task.getType()))) {
                continue;
            }
            if (!matchesSelectedUser(task)) {
                continue;
            }

            String section = resolveSection(task);
            if (!SECTION_ALL.equals(selectedSection) && !selectedSection.equals(section)) {
                continue;
            }

            if (!matchedHighlightedTask
                    && highlightTaskId != null
                    && highlightTaskId.equals(getHighlightKey(task))) {
                Toast.makeText(this, "Opened related task", Toast.LENGTH_SHORT).show();
                matchedHighlightedTask = true;
            }

            if (SECTION_HISTORY.equals(section)) {
                historyTasks.add(task);
            } else if (SECTION_WEEKLY.equals(section)) {
                weeklyTasks.add(task);
            } else if (SECTION_MONTHLY.equals(section)) {
                monthlyTasks.add(task);
            } else {
                onceTasks.add(task);
            }
        }

        sortTasks(onceTasks, false);
        sortTasks(weeklyTasks, false);
        sortTasks(monthlyTasks, false);
        sortTasks(historyTasks, true);

        populateSection(containerOnce, tvHeaderOnce, onceTasks);
        populateSection(containerWeekly, tvHeaderWeekly, weeklyTasks);
        populateSection(containerMonthly, tvHeaderMonthly, monthlyTasks);
        populateHistorySection(historyTasks);

        int visibleTaskCount = onceTasks.size() + weeklyTasks.size() + monthlyTasks.size() + historyTasks.size();
        updateEmptyState(visibleTaskCount);
    }

    private void clearSection(LinearLayout container, TextView header) {
        container.removeAllViews();
        header.setVisibility(View.GONE);
    }

    private void populateSection(LinearLayout container, TextView header, List<Calendar> tasks) {
        if (tasks.isEmpty()) {
            return;
        }

        header.setVisibility(View.VISIBLE);
        for (Calendar task : tasks) {
            container.addView(createCard(task, container));
        }
    }

    private void populateHistorySection(List<Calendar> tasks) {
        if (tasks.isEmpty()) {
            return;
        }

        layoutHistoryHeader.setVisibility(View.VISIBLE);
        tvHeaderHistory.setVisibility(View.VISIBLE);

        boolean forceExpanded = SECTION_HISTORY.equals(selectedSection);
        boolean showCards = forceExpanded || isHistoryExpanded;
        if (forceExpanded) {
            isHistoryExpanded = true;
            btnToggleHistory.setVisibility(View.GONE);
        } else {
            btnToggleHistory.setVisibility(View.VISIBLE);
            btnToggleHistory.setText(showCards
                    ? "Hide completed (" + tasks.size() + ")"
                    : "Show completed (" + tasks.size() + ")");
        }

        containerHistory.setVisibility(showCards ? View.VISIBLE : View.GONE);
        if (!showCards) {
            return;
        }

        for (Calendar task : tasks) {
            containerHistory.addView(createCard(task, containerHistory));
        }
    }

    private void updateEmptyState(int visibleTaskCount) {
        if (tasksUnavailable) {
            tvEmptyTitle.setText("You're offline");
            tvEmptyBody.setText("Reconnect to load chores and completed history for this planner view.");
            emptyStateCard.setVisibility(View.VISIBLE);
            return;
        }

        if (visibleTaskCount > 0) {
            emptyStateCard.setVisibility(View.GONE);
            return;
        }

        String roommateLabel = FILTER_ALL_USERS.equals(selectedUserId)
                ? "everyone"
                : getRoommateLabel(selectedUserId);

        String title;
        if (SECTION_HISTORY.equals(selectedSection)) {
            title = "No completed tasks yet";
        } else if (SECTION_WEEKLY.equals(selectedSection)) {
            title = "No weekly tasks right now";
        } else if (SECTION_MONTHLY.equals(selectedSection)) {
            title = "No monthly tasks right now";
        } else if (SECTION_ONCE.equals(selectedSection)) {
            title = "No one-time tasks right now";
        } else {
            title = "No tasks to show";
        }

        String body = FILTER_ALL_USERS.equals(selectedUserId)
                ? "Try another section or check back when new chores are assigned."
                : "There is nothing in this view for " + roommateLabel + " yet. Try another roommate or section.";

        tvEmptyTitle.setText(title);
        tvEmptyBody.setText(body);
        emptyStateCard.setVisibility(View.VISIBLE);
    }

    private boolean matchesSelectedUser(Calendar task) {
        if (FILTER_ALL_USERS.equals(selectedUserId)) {
            return true;
        }
        return selectedUserId.equals(task.getAssignedTo());
    }

    private String resolveSection(Calendar task) {
        String status = normalizeStatus(task.getStatus());
        if ("COMPLETED".equals(status)) {
            return SECTION_HISTORY;
        }

        if (task.getRecurrence() != null && task.getRecurrence().getFrequency() != null) {
            String frequency = task.getRecurrence().getFrequency().trim().toUpperCase(Locale.US);
            if (SECTION_WEEKLY.equals(frequency)) {
                return SECTION_WEEKLY;
            }
            if (SECTION_MONTHLY.equals(frequency)) {
                return SECTION_MONTHLY;
            }
        }
        return SECTION_ONCE;
    }

    private void sortTasks(List<Calendar> tasks, boolean historySection) {
        Comparator<Calendar> comparator = Comparator.comparingLong(task -> getSortTime(task, historySection));
        if (historySection) {
            comparator = comparator.reversed();
        }
        tasks.sort(comparator);
    }

    private long getSortTime(Calendar task, boolean historySection) {
        if (historySection) {
            if (task.getEndDate() != null) {
                return task.getEndDate().toDate().getTime();
            }
            if (task.getStartDate() != null) {
                return task.getStartDate().toDate().getTime();
            }
            return Long.MIN_VALUE;
        }

        if (task.getStartDate() != null) {
            return task.getStartDate().toDate().getTime();
        }
        return Long.MAX_VALUE;
    }

    private View createCard(Calendar task, ViewGroup parent) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_task_status_card, parent, false);

        View card = view.findViewById(R.id.cardContainer);
        TextView title = view.findViewById(R.id.tvTitle);
        TextView statusBadge = view.findViewById(R.id.tvStatusBadge);
        TextView extraInfo = view.findViewById(R.id.tvExtraInfo);
        TextView tvCreatedBy = view.findViewById(R.id.tvCreatedBy);
        TextView tvDateInfo = view.findViewById(R.id.tvDateInfo);
        TextView tvPointsEarned = view.findViewById(R.id.tvPointsEarned);
        View avatarShell = view.findViewById(R.id.avatarShell);
        ImageView imgAssignee = view.findViewById(R.id.imgAssigneeProfile);

        String status = normalizeStatus(task.getStatus());
        boolean isOverdue = isTaskOverdue(task);
        String displayStatus = isOverdue ? "OVERDUE" : status;
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);

        title.setText(getDisplayText(task.getTitle(), "Untitled task"));
        statusBadge.setText(formatStatus(displayStatus));
        applyTaskStatusBadge(statusBadge, displayStatus);

        String assigneeName = getPersonLabel(task.getAssignedTo());
        String creatorName = getPersonLabel(task.getCreatedBy());
        String location = cleanText(task.getLocation());

        extraInfo.setText("Assigned to " + assigneeName);
        tvCreatedBy.setText(location != null
                ? "Created by " + creatorName + "  |  " + location
                : "Created by " + creatorName);

        loadTaskProfileImage(imgAssignee, roommateImageMap.get(task.getAssignedTo()));

        if ("COMPLETED".equals(status)) {
            if (task.getEndDate() != null) {
                tvDateInfo.setText("Completed " + sdf.format(task.getEndDate().toDate()));
            } else {
                tvDateInfo.setText("Completed recently");
            }
            tvDateInfo.setVisibility(View.VISIBLE);
            avatarShell.setVisibility(View.VISIBLE);
            tvPointsEarned.setText("+" + calculateTaskPoints(task) + " pts earned");
            tvPointsEarned.setVisibility(View.VISIBLE);
        } else {
            if (task.getStartDate() != null) {
                tvDateInfo.setText((isOverdue ? "Was due " : "Due ") + sdf.format(task.getStartDate().toDate()));
                tvDateInfo.setVisibility(View.VISIBLE);
            } else {
                tvDateInfo.setVisibility(View.GONE);
            }
            avatarShell.setVisibility(View.VISIBLE);
            tvPointsEarned.setVisibility(View.GONE);
        }

        applyTaskCardStyle(card, displayStatus);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> {
            if ("COMPLETED".equals(status)) {
                showTaskPopup(task);
            } else if (task.getAssignedTo() != null && task.getAssignedTo().equals(currentUserId)) {
                Intent intent = new Intent(this, ChoreDetailActivity.class);
                intent.putExtra("CHORE_ID", task.getRelatedChoreId());
                intent.putExtra("HOUSE_CODE", houseCode);
                intent.putExtra("TITLE", task.getTitle());
                intent.putExtra("CURRENT_STATUS", status);
                intent.putExtra("DESCRIPTION", task.getDescription());
                intent.putExtra("LOCATION", task.getLocation());
                intent.putExtra("EST_DURATION", task.getEstDuration());
                intent.putExtra("ASSIGNED_TO_NAME", assigneeName);
                intent.putExtra("CREATED_BY_NAME", creatorName);

                if (task.getStartDate() != null) {
                    intent.putExtra("DUE_DATE", sdf.format(task.getStartDate().toDate()));
                }
                startActivity(intent);
            } else {
                showTaskPopup(task);
            }
        });

        return view;
    }

    private void applyTaskCardStyle(View card, String status) {
        int cardBackgroundRes;

        if ("OVERDUE".equals(status)) {
            cardBackgroundRes = R.drawable.bg_task_card_overdue;
        } else if ("COMPLETED".equals(status)) {
            cardBackgroundRes = R.drawable.bg_task_card_completed;
        } else if ("IN_PROGRESS".equals(status)) {
            cardBackgroundRes = R.drawable.bg_task_card_progress;
        } else {
            cardBackgroundRes = R.drawable.bg_task_card_pending;
        }

        card.setBackgroundResource(cardBackgroundRes);
    }

    private void applyTaskStatusBadge(TextView badge, String status) {
        int badgeBackgroundColorRes;
        if ("OVERDUE".equals(status)) {
            badgeBackgroundColorRes = R.color.task_badge_overdue_bg;
        } else if ("COMPLETED".equals(status)) {
            badgeBackgroundColorRes = R.color.task_badge_completed_bg;
        } else if ("IN_PROGRESS".equals(status)) {
            badgeBackgroundColorRes = R.color.task_badge_progress_bg;
        } else {
            badgeBackgroundColorRes = R.color.task_badge_pending_bg;
        }

        badge.setBackgroundResource(R.drawable.bg_task_status_badge);
        badge.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, badgeBackgroundColorRes)
        ));
        badge.setTextColor(ContextCompat.getColor(this, isDarkMode() ? R.color.black : R.color.app_text_primary));
    }

    private boolean isDarkMode() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    private String getPersonLabel(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return "Unassigned";
        }
        if (userId.equals(currentUserId)) {
            return "Me";
        }
        String name = roommateNameMap.get(userId);
        return name != null && !name.trim().isEmpty() ? name : "Roommate";
    }

    private String getRoommateLabel(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return "this roommate";
        }
        if (userId.equals(currentUserId)) {
            return "you";
        }
        String name = roommateNameMap.get(userId);
        return name != null && !name.trim().isEmpty() ? name : "this roommate";
    }

    private String getDisplayText(String value, String fallback) {
        String cleaned = cleanText(value);
        return cleaned != null ? cleaned : fallback;
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safeText(String value) {
        return value != null ? value : "";
    }

    private String normalizeStatus(String status) {
        return safeText(status).trim().toUpperCase(Locale.US);
    }

    private boolean isTaskOverdue(Calendar task) {
        if (task == null || task.getStartDate() == null) {
            return false;
        }

        String status = normalizeStatus(task.getStatus());
        if ("COMPLETED".equals(status)) {
            return false;
        }

        java.util.Calendar dueDate = java.util.Calendar.getInstance();
        dueDate.setTime(task.getStartDate().toDate());
        dueDate.set(java.util.Calendar.HOUR_OF_DAY, 0);
        dueDate.set(java.util.Calendar.MINUTE, 0);
        dueDate.set(java.util.Calendar.SECOND, 0);
        dueDate.set(java.util.Calendar.MILLISECOND, 0);

        java.util.Calendar today = java.util.Calendar.getInstance();
        today.set(java.util.Calendar.HOUR_OF_DAY, 0);
        today.set(java.util.Calendar.MINUTE, 0);
        today.set(java.util.Calendar.SECOND, 0);
        today.set(java.util.Calendar.MILLISECOND, 0);
        return dueDate.before(today);
    }

    private String getHighlightKey(Calendar task) {
        return task.getRelatedChoreId() != null ? task.getRelatedChoreId() : task.getId();
    }

    private void loadTaskProfileImage(ImageView imageView, String imageSource) {
        String cleaned = cleanText(imageSource);
        if (cleaned == null || cleaned.length() < 10) {
            imageView.setImageResource(R.drawable.ic_profile_tenant);
            if (isDarkMode()) {
                imageView.setColorFilter(ContextCompat.getColor(this, R.color.white));
            } else {
                imageView.clearColorFilter();
            }
            return;
        }

        imageView.clearColorFilter();
        com.example.uninest.utils.ImageUtils.loadProfileImage(imageView, cleaned);
    }

    class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_roommate_filter, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            User user = filterList.get(position);
            holder.name.setText(getDisplayText(user.getFirstName(), "Roommate"));

            int padding = (int) (1 * getResources().getDisplayMetrics().density);
            holder.profile.setPadding(padding, padding, padding, padding);

            if (FILTER_ALL_USERS.equals(user.getId())) {
                if (!FILTER_ALL_USERS.equals(holder.lastLoadedKey)) {
                    holder.profile.clearColorFilter();
                    holder.profile.setImageResource(R.drawable.ic_all_users);
                    holder.profile.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    holder.lastLoadedKey = FILTER_ALL_USERS;
                }
            } else {
                String imageUrl = cleanText(user.getProfileImageUrl());
                String key = imageUrl != null ? imageUrl : "default_" + user.getId();
                if (!key.equals(holder.lastLoadedKey)) {
                    holder.profile.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    loadTaskProfileImage(holder.profile, imageUrl);
                    holder.lastLoadedKey = key;
                }
            }

            if (FILTER_ALL_USERS.equals(user.getId())) {
                holder.points.setVisibility(View.GONE);
            } else {
                int points = calculateUserPoints(user.getId());
                if (points > 0) {
                    holder.points.setText(points + " pts");
                    holder.points.setVisibility(View.VISIBLE);
                } else {
                    holder.points.setVisibility(View.GONE);
                }
            }

            if (user.getId().equals(selectedUserId)) {
                holder.frame.setBackgroundResource(R.drawable.bg_tasks_filter_circle_selected);
                holder.name.setTextColor(ContextCompat.getColor(ViewAllTasksActivity.this, R.color.calendar_primary_dark));
            } else {
                holder.frame.setBackgroundResource(R.drawable.bg_tasks_filter_circle);
                holder.name.setTextColor(ContextCompat.getColor(ViewAllTasksActivity.this, R.color.calendar_text_primary));
            }

            holder.itemView.setOnClickListener(v -> {
                selectedUserId = user.getId();
                notifyDataSetChanged();
                updateUi();
            });
        }

        @Override
        public int getItemCount() {
            return filterList.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView points;
            final View frame;
            final ImageView profile;
            String lastLoadedKey;

            VH(View view) {
                super(view);
                name = view.findViewById(R.id.tvRoommateName);
                points = view.findViewById(R.id.tvPoints);
                frame = view.findViewById(R.id.frameCircle);
                profile = view.findViewById(R.id.imgProfile);
            }
        }
    }

    private void showTaskPopup(Calendar task) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_task_details_mini);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialog.findViewById(R.id.popTitle);
        TextView tvStatusBadge = dialog.findViewById(R.id.popStatusBadge);
        TextView tvDesc = dialog.findViewById(R.id.popDesc);
        TextView tvLocation = dialog.findViewById(R.id.popLocation);
        TextView tvAssignee = dialog.findViewById(R.id.popAssignee);
        TextView tvEst = dialog.findViewById(R.id.popEstTime);
        TextView tvActual = dialog.findViewById(R.id.popActualTime);
        TextView tvDueDate = dialog.findViewById(R.id.popDueDate);
        View layoutActual = dialog.findViewById(R.id.layoutActualTime);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        String status = normalizeStatus(task.getStatus());
        String displayStatus = isTaskOverdue(task) ? "OVERDUE" : status;

        tvTitle.setText(getDisplayText(task.getTitle(), "Untitled task"));
        tvStatusBadge.setText(formatStatus(displayStatus));
        applyTaskStatusBadge(tvStatusBadge, displayStatus);
        tvDesc.setText(getDisplayText(task.getDescription(), "No additional notes yet."));
        tvLocation.setText(getDisplayText(task.getLocation(), "General"));
        tvAssignee.setText(getPersonLabel(task.getAssignedTo()));
        tvEst.setText(task.getEstDuration() > 0 ? task.getEstDuration() + " mins" : "Not set");
        tvDueDate.setText(task.getStartDate() != null
                ? sdf.format(task.getStartDate().toDate())
                : "No due date");

        if ("COMPLETED".equals(status)) {
            layoutActual.setVisibility(View.VISIBLE);
            tvActual.setText(task.getActualDuration() > 0 ? task.getActualDuration() + " mins" : "Not logged");
        } else {
            layoutActual.setVisibility(View.GONE);
        }

        dialog.findViewById(R.id.btnPopClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String formatStatus(String status) {
        if ("OVERDUE".equals(status)) {
            return "Overdue";
        }
        if ("NOT_STARTED".equals(status)) {
            return "Not Started";
        }
        if ("IN_PROGRESS".equals(status)) {
            return "In Progress";
        }
        if ("COMPLETED".equals(status)) {
            return "Completed";
        }
        return "Pending";
    }

    private int calculateTaskPoints(Calendar task) {
        int difficulty = Math.max(task.getDifficultyScore(), 1);
        int estimatedTime = Math.max(task.getEstDuration(), 10);
        return difficulty * estimatedTime;
    }

    private int calculateUserPoints(String userId) {
        int total = 0;
        for (Calendar task : allTasks) {
            if ("CHORE".equalsIgnoreCase(task.getType())
                    && "COMPLETED".equalsIgnoreCase(task.getStatus())
                    && userId.equals(task.getAssignedTo())) {
                total += calculateTaskPoints(task);
            }
        }
        return total;
    }
}
