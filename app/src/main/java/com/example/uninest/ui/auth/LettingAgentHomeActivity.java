package com.example.uninest.ui.auth;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.BuildingApi;
import com.example.uninest.data.api.TicketApi;
import com.example.uninest.model.Building;
import com.example.uninest.model.Ticket;
import com.example.uninest.utils.AgentBottomNavHelper;
import com.example.uninest.utils.NetworkErrorDialog;

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

public class LettingAgentHomeActivity extends AppCompatActivity {

    private final List<Building> buildings = new ArrayList<>();
    private final List<Ticket> tickets = new ArrayList<>();

    private SessionManager sessionManager;
    private BuildingApi buildingApi;
    private TicketApi ticketApi;

    private TextView tvAgentGreeting;
    private TextView tvAgentCompany;
    private TextView tvAgentSummary;
    private View heroCard;
    private View heroOrbLarge;
    private View heroOrbSmall;
    private View layoutHeroQuickActions;
    private TextView tvMetricBuildings;
    private TextView tvMetricApartments;
    private TextView tvMetricActiveTickets;
    private TextView tvMetricScheduledVisits;
    private LinearLayout layoutPriorityTickets;
    private LinearLayout layoutBuildingWatchlist;
    private View layoutPriorityEmpty;
    private View layoutWatchlistEmpty;
    private TextView tvPriorityEmptyText;
    private TextView tvWatchlistEmptyText;

    private int dashboardLoadToken = 0;
    private int pendingDashboardLoads = 0;
    private boolean dashboardErrorDialogVisible = false;
    private AnimatorSet heroOrbLargeAnimator;
    private AnimatorSet heroOrbSmallAnimator;

    private final ActivityResultLauncher<Intent> addBuildingLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadDashboard();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letting_agent_home);

        sessionManager = new SessionManager(this);
        buildingApi = ApiClient.getBuildingApi();
        ticketApi = ApiClient.getTicketApi();

        bindViews();
        setupQuickActions();
        setupHomeAnimations();
        AgentBottomNavHelper.setup(this, R.id.nav_home);
        loadDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AgentBottomNavHelper.syncSelected(this, R.id.nav_home);
        loadDashboard();
    }

    private void bindViews() {
        heroCard = findViewById(R.id.heroCard);
        heroOrbLarge = findViewById(R.id.heroOrbLarge);
        heroOrbSmall = findViewById(R.id.heroOrbSmall);
        layoutHeroQuickActions = findViewById(R.id.layoutHeroQuickActions);
        tvAgentGreeting = findViewById(R.id.tvAgentGreeting);
        tvAgentCompany = findViewById(R.id.tvAgentCompany);
        tvAgentSummary = findViewById(R.id.tvAgentSummary);
        tvMetricBuildings = findViewById(R.id.tvMetricBuildings);
        tvMetricApartments = findViewById(R.id.tvMetricApartments);
        tvMetricActiveTickets = findViewById(R.id.tvMetricActiveTickets);
        tvMetricScheduledVisits = findViewById(R.id.tvMetricScheduledVisits);
        layoutPriorityTickets = findViewById(R.id.layoutPriorityTickets);
        layoutBuildingWatchlist = findViewById(R.id.layoutBuildingWatchlist);
        layoutPriorityEmpty = findViewById(R.id.layoutPriorityEmpty);
        layoutWatchlistEmpty = findViewById(R.id.layoutWatchlistEmpty);
        tvPriorityEmptyText = findViewById(R.id.tvPriorityEmptyText);
        tvWatchlistEmptyText = findViewById(R.id.tvWatchlistEmptyText);
    }

    private void setupHomeAnimations() {
        if (heroCard != null) {
            heroCard.setAlpha(0f);
            heroCard.setTranslationY(dp(18));
            heroCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(520)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        if (layoutHeroQuickActions != null) {
            layoutHeroQuickActions.setAlpha(0f);
            layoutHeroQuickActions.setTranslationY(dp(14));
            layoutHeroQuickActions.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(120)
                    .setDuration(420)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        heroOrbLargeAnimator = startFloatingAnimation(heroOrbLarge, 18f, 4800L, 0L);
        heroOrbSmallAnimator = startFloatingAnimation(heroOrbSmall, -14f, 4200L, 240L);
    }

    private AnimatorSet startFloatingAnimation(View target, float travel, long duration, long startDelay) {
        if (target == null) {
            return null;
        }

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
        return animatorSet;
    }

    private void setupQuickActions() {
        findViewById(R.id.btnHeroNewBuilding).setOnClickListener(v ->
                addBuildingLauncher.launch(new Intent(this, AddBuildingActivity.class)));
        findViewById(R.id.btnHeroTickets).setOnClickListener(v ->
                startActivity(new Intent(this, LettingAgentTicketsActivity.class)));
        findViewById(R.id.btnHeroNotify).setOnClickListener(v ->
                startActivity(new Intent(this, LettingAgentNotificationsActivity.class)));
        findViewById(R.id.btnViewAllTickets).setOnClickListener(v ->
                startActivity(new Intent(this, LettingAgentTicketsActivity.class)));
        findViewById(R.id.btnViewPortfolio).setOnClickListener(v ->
                startActivity(new Intent(this, LettingAgentBuildingsActivity.class)));
    }

    private void loadDashboard() {
        String landlordId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (landlordId == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        int loadToken = ++dashboardLoadToken;
        pendingDashboardLoads = 2;
        tvAgentSummary.setText("Loading your portfolio snapshot...");

        requestBuildings(loadToken, landlordId);
        requestTickets(loadToken, landlordId);
    }

    private void requestBuildings(int loadToken, String landlordId) {
        buildingApi.getBuildingsByLandlord(landlordId).enqueue(new Callback<List<Building>>() {
            @Override
            public void onResponse(Call<List<Building>> call, Response<List<Building>> response) {
                if (loadToken != dashboardLoadToken) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    buildings.clear();
                    buildings.addAll(response.body());
                } else {
                    showDashboardLoadError();
                }

                onDashboardSourceLoaded(loadToken);
            }

            @Override
            public void onFailure(Call<List<Building>> call, Throwable t) {
                if (loadToken != dashboardLoadToken) {
                    return;
                }

                showDashboardLoadError();
                onDashboardSourceLoaded(loadToken);
            }
        });
    }

    private void requestTickets(int loadToken, String landlordId) {
        ticketApi.getTicketsByLandlord(landlordId, false).enqueue(new Callback<List<Ticket>>() {
            @Override
            public void onResponse(Call<List<Ticket>> call, Response<List<Ticket>> response) {
                if (loadToken != dashboardLoadToken) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    tickets.clear();
                    tickets.addAll(response.body());
                } else {
                    showDashboardLoadError();
                }

                onDashboardSourceLoaded(loadToken);
            }

            @Override
            public void onFailure(Call<List<Ticket>> call, Throwable t) {
                if (loadToken != dashboardLoadToken) {
                    return;
                }

                showDashboardLoadError();
                onDashboardSourceLoaded(loadToken);
            }
        });
    }

    private void showDashboardLoadError() {
        if (dashboardErrorDialogVisible || isFinishing() || isDestroyed()) {
            return;
        }

        dashboardErrorDialogVisible = true;
        NetworkErrorDialog.show(
                this,
                "Couldn't refresh your dashboard",
                "Check your connection and try again.",
                () -> {
                    dashboardErrorDialogVisible = false;
                    loadDashboard();
                }
        );
    }

    private void onDashboardSourceLoaded(int loadToken) {
        if (loadToken != dashboardLoadToken) {
            return;
        }

        pendingDashboardLoads = Math.max(0, pendingDashboardLoads - 1);
        if (pendingDashboardLoads > 0) {
            return;
        }

        renderDashboard();
    }

    private void renderDashboard() {
        List<Ticket> activeTickets = getActiveTickets();
        int buildingCount = buildings.size();
        int apartmentCount = getApartmentCount();
        int scheduledVisits = countScheduledVisits(activeTickets);
        int urgentCount = countUrgentTickets(activeTickets);

        bindHeader(buildingCount, apartmentCount, activeTickets.size(), scheduledVisits, urgentCount);
        tvMetricBuildings.setText(String.valueOf(buildingCount));
        tvMetricApartments.setText(String.valueOf(apartmentCount));
        tvMetricActiveTickets.setText(String.valueOf(activeTickets.size()));
        tvMetricScheduledVisits.setText(String.valueOf(scheduledVisits));

        bindPriorityQueue(activeTickets);
        bindBuildingWatchlist(activeTickets);
    }

    private void bindHeader(int buildingCount,
                            int apartmentCount,
                            int activeTicketCount,
                            int scheduledVisits,
                            int urgentCount) {
        String fullName = sessionManager.getUserFullName();
        String company = sessionManager.getCompany();
        String greetingTarget = extractGreetingName(fullName, company);
        String timeGreeting = buildTimeGreeting();

        tvAgentGreeting.setText(timeGreeting + ",\n" + greetingTarget + "!");
        tvAgentCompany.setText(!TextUtils.isEmpty(company) ? company : "Letting portfolio");

        if (buildingCount == 0) {
            tvAgentSummary.setText("Start by adding your first building. Once your portfolio is live, this dashboard will surface ticket pressure and scheduled visits here.");
            return;
        }

        if (activeTicketCount == 0) {
            tvAgentSummary.setText("Portfolio looks calm right now with " + apartmentCount + " apartments across "
                    + buildingCount + " buildings and no active maintenance tickets.");
            return;
        }

        if (urgentCount > 0) {
            if (scheduledVisits > 0) {
                tvAgentSummary.setText(urgentCount + " urgent tickets and " + scheduledVisits
                        + " scheduled visits need attention across " + apartmentCount + " apartments.");
            } else {
                tvAgentSummary.setText(urgentCount + " urgent tickets need attention across "
                        + apartmentCount + " apartments.");
            }
            return;
        }

        tvAgentSummary.setText(activeTicketCount + " active tickets and " + scheduledVisits
                + " scheduled visits are in flight across " + apartmentCount + " apartments.");
    }

    private void bindPriorityQueue(List<Ticket> activeTickets) {
        layoutPriorityTickets.removeAllViews();

        List<Ticket> queue = new ArrayList<>(activeTickets);
        queue.sort(this::comparePriorityTickets);

        if (queue.isEmpty()) {
            layoutPriorityEmpty.setVisibility(View.VISIBLE);
            tvPriorityEmptyText.setText("No active tickets right now. When something needs attention, your next best actions will show up here.");
            return;
        }

        layoutPriorityEmpty.setVisibility(View.GONE);

        int limit = Math.min(queue.size(), 3);
        for (int i = 0; i < limit; i++) {
            Ticket ticket = queue.get(i);
            View row = getLayoutInflater().inflate(R.layout.item_agent_dashboard_ticket, layoutPriorityTickets, false);
            bindPriorityTicketRow(row, ticket);
            layoutPriorityTickets.addView(row);
        }
    }

    private void bindPriorityTicketRow(View row, Ticket ticket) {
        View card = row.findViewById(R.id.cardTicketRow);
        TextView tvStatus = row.findViewById(R.id.tvTicketStateBadge);
        TextView tvPriority = row.findViewById(R.id.tvTicketPriorityChip);
        TextView tvTitle = row.findViewById(R.id.tvTicketTitle);
        TextView tvMeta = row.findViewById(R.id.tvTicketMeta);
        TextView tvSummary = row.findViewById(R.id.tvTicketSummary);
        TextView tvSchedule = row.findViewById(R.id.tvTicketSchedule);

        String canonicalStatus = canonicalStatus(ticket.getStatus());
        if ("progress".equals(canonicalStatus)) {
            card.setBackgroundResource(R.drawable.bg_tenant_ticket_card_progress);
        } else if ("solved".equals(canonicalStatus)) {
            card.setBackgroundResource(R.drawable.bg_tenant_ticket_card_solved);
        } else {
            card.setBackgroundResource(R.drawable.bg_tenant_ticket_card_raised);
        }

        bindStatusBadge(tvStatus, ticket.getStatus());
        bindPriorityChip(tvPriority, ticket.getPriority());

        tvTitle.setText(buildTicketTitle(ticket));
        tvMeta.setText(buildTicketLocation(ticket));
        tvSummary.setText(buildTicketSummary(ticket));
        tvSchedule.setText(buildScheduleLabel(ticket));

        row.setOnClickListener(v -> {
            Intent intent = new Intent(this, LettingAgentTicketDetailsActivity.class);
            intent.putExtra("TICKET_OBJ", ticket);
            startActivity(intent);
        });
    }

    private void bindBuildingWatchlist(List<Ticket> activeTickets) {
        layoutBuildingWatchlist.removeAllViews();

        if (buildings.isEmpty()) {
            layoutWatchlistEmpty.setVisibility(View.VISIBLE);
            tvWatchlistEmptyText.setText("No buildings yet. Add one to start tracking your portfolio here.");
            return;
        }

        layoutWatchlistEmpty.setVisibility(View.GONE);

        List<BuildingWatchStat> watchStats = buildBuildingWatchStats(activeTickets);
        int limit = Math.min(watchStats.size(), 3);
        for (int i = 0; i < limit; i++) {
            BuildingWatchStat stat = watchStats.get(i);
            View row = getLayoutInflater().inflate(R.layout.item_agent_dashboard_building, layoutBuildingWatchlist, false);
            bindBuildingWatchRow(row, stat);
            layoutBuildingWatchlist.addView(row);
        }
    }

    private void bindBuildingWatchRow(View row, BuildingWatchStat stat) {
        TextView tvName = row.findViewById(R.id.tvWatchBuildingName);
        TextView tvSummary = row.findViewById(R.id.tvWatchBuildingSummary);
        TextView tvApartments = row.findViewById(R.id.tvWatchApartmentsChip);
        TextView tvTickets = row.findViewById(R.id.tvWatchTicketsChip);
        TextView tvUrgent = row.findViewById(R.id.tvWatchUrgentChip);
        TextView tvState = row.findViewById(R.id.tvWatchStateChip);

        tvName.setText(!TextUtils.isEmpty(stat.building.getName()) ? stat.building.getName() : "Building");
        tvApartments.setText(stat.building.getApartmentCount() + " apartments");
        tvTickets.setText(stat.activeTickets > 0 ? stat.activeTickets + " active" : "No active tickets");

        if (stat.urgentTickets > 0) {
            tvUrgent.setVisibility(View.VISIBLE);
            tvUrgent.setText(stat.urgentTickets + " urgent");
            tvUrgent.setBackgroundResource(R.drawable.bg_tenant_ticket_status_raised);
            tvUrgent.setTextColor(getColor(R.color.ticket_raised_text));
            if (stat.scheduledVisits > 0) {
                tvState.setText(stat.scheduledVisits + " scheduled");
                tvState.setBackgroundResource(R.drawable.bg_tenant_ticket_status_progress);
                tvState.setTextColor(getColor(R.color.ticket_progress_text));
            } else {
                tvState.setText("Needs booking");
                tvState.setBackgroundResource(R.drawable.bg_soft_badge);
                tvState.setTextColor(getColor(R.color.app_text_primary));
            }
        } else if (stat.scheduledVisits > 0) {
            tvUrgent.setVisibility(View.VISIBLE);
            tvUrgent.setText(stat.scheduledVisits + " scheduled");
            tvUrgent.setBackgroundResource(R.drawable.bg_tenant_ticket_status_progress);
            tvUrgent.setTextColor(getColor(R.color.ticket_progress_text));
            tvState.setText("Steady");
            tvState.setBackgroundResource(R.drawable.bg_tenant_calendar_today_chip);
            tvState.setTextColor(getColor(R.color.calendar_today_chip_text));
        } else {
            tvUrgent.setVisibility(View.GONE);
            tvState.setText(stat.activeTickets > 0 ? "Needs review" : "Quiet");
            if (stat.activeTickets > 0) {
                tvState.setBackgroundResource(R.drawable.bg_tenant_ticket_status_raised);
                tvState.setTextColor(getColor(R.color.ticket_raised_text));
            } else {
                tvState.setBackgroundResource(R.drawable.bg_tenant_calendar_today_chip);
                tvState.setTextColor(getColor(R.color.calendar_today_chip_text));
            }
        }

        tvSummary.setText(buildWatchSummary(stat));

        row.setOnClickListener(v -> openBuilding(stat.building));
    }

    private void openBuilding(Building building) {
        Intent intent = new Intent(this, LettingAgentApartmentsActivity.class);
        intent.putExtra("EXTRA_BUILDING_NAME", building.getName());
        intent.putExtra("EXTRA_BUILDING_ID", building.getId());
        startActivity(intent);
    }

    private List<BuildingWatchStat> buildBuildingWatchStats(List<Ticket> activeTickets) {
        Map<String, BuildingWatchStat> statsByName = new HashMap<>();
        for (Building building : buildings) {
            statsByName.put(normalizeBuildingKey(building.getName()), new BuildingWatchStat(building));
        }

        for (Ticket ticket : activeTickets) {
            BuildingWatchStat stat = statsByName.get(normalizeBuildingKey(ticket.getBuilding()));
            if (stat == null) {
                continue;
            }

            stat.activeTickets++;
            if (isUrgentTicket(ticket)) {
                stat.urgentTickets++;
            }
            if (hasScheduledVisit(ticket)) {
                stat.scheduledVisits++;
            }
        }

        List<BuildingWatchStat> stats = new ArrayList<>(statsByName.values());
        stats.sort((first, second) -> {
            int byActive = Integer.compare(second.activeTickets, first.activeTickets);
            if (byActive != 0) {
                return byActive;
            }

            int byUrgent = Integer.compare(second.urgentTickets, first.urgentTickets);
            if (byUrgent != 0) {
                return byUrgent;
            }

            int byApartments = Integer.compare(second.building.getApartmentCount(), first.building.getApartmentCount());
            if (byApartments != 0) {
                return byApartments;
            }

            String firstName = first.building.getName() != null ? first.building.getName() : "";
            String secondName = second.building.getName() != null ? second.building.getName() : "";
            return firstName.compareToIgnoreCase(secondName);
        });
        return stats;
    }

    private List<Ticket> getActiveTickets() {
        List<Ticket> active = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if (ticket != null && isActiveTicket(ticket)) {
                active.add(ticket);
            }
        }
        return active;
    }

    private int getApartmentCount() {
        int count = 0;
        for (Building building : buildings) {
            count += Math.max(0, building.getApartmentCount());
        }
        return count;
    }

    private int countScheduledVisits(List<Ticket> activeTickets) {
        int count = 0;
        for (Ticket ticket : activeTickets) {
            if (hasScheduledVisit(ticket)) {
                count++;
            }
        }
        return count;
    }

    private int countUrgentTickets(List<Ticket> activeTickets) {
        int count = 0;
        for (Ticket ticket : activeTickets) {
            if (isUrgentTicket(ticket)) {
                count++;
            }
        }
        return count;
    }

    private boolean isUrgentTicket(Ticket ticket) {
        return isActiveTicket(ticket) && priorityWeight(ticket.getPriority()) >= 3;
    }

    private boolean isActiveTicket(Ticket ticket) {
        return !"solved".equals(canonicalStatus(ticket.getStatus()));
    }

    private boolean hasScheduledVisit(Ticket ticket) {
        return ticket != null
                && ticket.getArrivalDate() != null
                && !ticket.getArrivalDate().trim().isEmpty();
    }

    private int comparePriorityTickets(Ticket first, Ticket second) {
        int scoreComparison = Integer.compare(ticketUrgencyScore(second), ticketUrgencyScore(first));
        if (scoreComparison != 0) {
            return scoreComparison;
        }

        boolean firstHasVisit = hasScheduledVisit(first);
        boolean secondHasVisit = hasScheduledVisit(second);
        if (firstHasVisit && secondHasVisit) {
            long firstVisit = parseDisplayDate(first.getArrivalDate());
            long secondVisit = parseDisplayDate(second.getArrivalDate());
            if (firstVisit != secondVisit) {
                return Long.compare(firstVisit, secondVisit);
            }
        } else if (firstHasVisit != secondHasVisit) {
            return firstHasVisit ? 1 : -1;
        }

        return Long.compare(createdAtMillis(second), createdAtMillis(first));
    }

    private int ticketUrgencyScore(Ticket ticket) {
        int priority = priorityWeight(ticket.getPriority());
        int status = "raised".equals(canonicalStatus(ticket.getStatus())) ? 2 : 1;
        int scheduled = hasScheduledVisit(ticket) ? 0 : 1;
        return (priority * 10) + (status * 3) + scheduled;
    }

    private int priorityWeight(String priority) {
        if (priority == null) {
            return 0;
        }
        if ("high".equalsIgnoreCase(priority.trim())) {
            return 3;
        }
        if ("medium".equalsIgnoreCase(priority.trim())) {
            return 2;
        }
        if ("low".equalsIgnoreCase(priority.trim())) {
            return 1;
        }
        return 0;
    }

    private String buildTicketTitle(Ticket ticket) {
        String category = !TextUtils.isEmpty(ticket.getCategory()) ? ticket.getCategory().trim() : "Maintenance issue";
        String room = !TextUtils.isEmpty(ticket.getRoom()) ? ticket.getRoom().trim() : null;
        return room != null ? room + ": " + category : category;
    }

    private String buildTicketLocation(Ticket ticket) {
        List<String> parts = new ArrayList<>();
        if (!TextUtils.isEmpty(ticket.getBuilding())) {
            parts.add(ticket.getBuilding().trim());
        }
        if (!TextUtils.isEmpty(ticket.getApartmentName())) {
            parts.add(ticket.getApartmentName().trim());
        }
        if (!TextUtils.isEmpty(ticket.getUserName())) {
            parts.add("Raised by " + ticket.getUserName().trim());
        }
        return parts.isEmpty() ? "Location not available" : TextUtils.join(" • ", parts);
    }

    private String buildTicketSummary(Ticket ticket) {
        if (hasScheduledVisit(ticket)) {
            return "Scheduled visit: " + ticket.getArrivalDate().trim();
        }
        if (!TextUtils.isEmpty(ticket.getAgentResponse())) {
            return ticket.getAgentResponse().trim();
        }
        if ("progress".equals(canonicalStatus(ticket.getStatus()))) {
            return "In progress and waiting for next update.";
        }
        return "Waiting for review and schedule confirmation.";
    }

    private String buildScheduleLabel(Ticket ticket) {
        if (hasScheduledVisit(ticket)) {
            return "Visit booked";
        }
        if ("progress".equals(canonicalStatus(ticket.getStatus()))) {
            return "No visit booked yet";
        }
        return "Needs first response";
    }

    private String buildWatchSummary(BuildingWatchStat stat) {
        if (stat.activeTickets == 0) {
            return "Quiet right now.";
        }
        if (stat.urgentTickets > 0) {
            return stat.urgentTickets + (stat.urgentTickets == 1 ? " urgent issue." : " urgent issues.");
        }
        if (stat.scheduledVisits > 0) {
            return stat.scheduledVisits + (stat.scheduledVisits == 1 ? " visit booked." : " visits booked.");
        }
        return stat.activeTickets + (stat.activeTickets == 1 ? " open issue." : " open issues.");
    }

    private void bindStatusBadge(TextView badge, String status) {
        String canonical = canonicalStatus(status);
        if ("progress".equals(canonical)) {
            badge.setText("In progress");
            badge.setBackgroundResource(R.drawable.bg_tenant_ticket_status_progress);
            badge.setTextColor(getColor(R.color.ticket_progress_text));
        } else if ("solved".equals(canonical)) {
            badge.setText("Solved");
            badge.setBackgroundResource(R.drawable.bg_tenant_ticket_status_solved);
            badge.setTextColor(getColor(R.color.ticket_solved_text));
        } else {
            badge.setText("Raised");
            badge.setBackgroundResource(R.drawable.bg_tenant_ticket_status_raised);
            badge.setTextColor(getColor(R.color.ticket_raised_text));
        }
    }

    private void bindPriorityChip(TextView chip, String priority) {
        chip.setBackgroundResource(R.drawable.bg_priority_chip);

        if ("high".equalsIgnoreCase(priority)) {
            chip.setText("High");
            chip.setTextColor(getColor(R.color.chip_high_text));
        } else if ("medium".equalsIgnoreCase(priority)) {
            chip.setText("Medium");
            chip.setTextColor(getColor(R.color.chip_medium_text));
        } else if ("low".equalsIgnoreCase(priority)) {
            chip.setText("Low");
            chip.setTextColor(getColor(R.color.chip_low_text));
        } else {
            chip.setText("Unrated");
            chip.setTextColor(getColor(R.color.calendar_text_secondary));
        }
    }

    private String canonicalStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            return "raised";
        }

        String normalized = rawStatus.trim()
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ");

        if ("in progress".equals(normalized) || "in process".equals(normalized)) {
            return "progress";
        }
        if ("resolved".equals(normalized) || "closed".equals(normalized) || "solved".equals(normalized)) {
            return "solved";
        }
        return "raised";
    }

    private long createdAtMillis(Ticket ticket) {
        Object raw = ticket != null ? ticket.getCreatedAt() : null;
        if (raw instanceof Date) {
            return ((Date) raw).getTime();
        }
        if (raw instanceof Long) {
            return (Long) raw;
        }
        if (raw instanceof Double) {
            return ((Double) raw).longValue();
        }
        if (raw instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) raw).toDate().getTime();
        }
        if (raw instanceof Map) {
            Object seconds = ((Map<?, ?>) raw).get("seconds");
            if (seconds instanceof Long) {
                return ((Long) seconds) * 1000L;
            }
            if (seconds instanceof Double) {
                return ((Double) seconds).longValue() * 1000L;
            }
        }
        return 0L;
    }

    private long parseDisplayDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Long.MAX_VALUE;
        }

        String[] patterns = {"d/M/yyyy", "dd/MM/yyyy", "d-M-yyyy", "dd-MM-yyyy"};
        for (String pattern : patterns) {
            try {
                Date parsed = new SimpleDateFormat(pattern, Locale.UK).parse(value.trim());
                if (parsed != null) {
                    return parsed.getTime();
                }
            } catch (Exception ignored) {
            }
        }
        return Long.MAX_VALUE;
    }

    private String normalizeBuildingKey(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private String buildTimeGreeting() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            return "Good morning";
        }
        if (hour < 17) {
            return "Good afternoon";
        }
        return "Good evening";
    }

    private String extractGreetingName(String fullName, String company) {
        if (!TextUtils.isEmpty(fullName)) {
            String trimmed = fullName.trim();
            if (trimmed.contains(" ")) {
                return trimmed.substring(0, trimmed.indexOf(' '));
            }
            return trimmed;
        }
        if (!TextUtils.isEmpty(company)) {
            return company.trim();
        }
        return "Agent";
    }

    @Override
    protected void onDestroy() {
        if (heroOrbLargeAnimator != null) {
            heroOrbLargeAnimator.cancel();
        }
        if (heroOrbSmallAnimator != null) {
            heroOrbSmallAnimator.cancel();
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    private static final class BuildingWatchStat {
        final Building building;
        int activeTickets;
        int urgentTickets;
        int scheduledVisits;

        BuildingWatchStat(Building building) {
            this.building = building;
        }
    }
}
