package com.example.uninest.ui.auth;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.TicketApi;
import com.example.uninest.model.Ticket;
import com.example.uninest.notifications.LocalNotificationHelper;
import com.example.uninest.utils.DestructiveConfirmationDialog;
import com.example.uninest.utils.ImageUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TenantTicketsActivity extends AppCompatActivity {

    private static final String TAG = "TenantTicketsActivity";

    private LinearLayout containerRaised;
    private LinearLayout containerInProgress;
    private LinearLayout containerSolved;
    private View headerRaised;
    private View headerInProgress;
    private View headerSolved;
    private ImageView ivToggleRaised;
    private ImageView ivToggleInProgress;
    private ImageView ivToggleSolved;
    private TextView tvCountRaised;
    private TextView tvCountInProgress;
    private TextView tvCountSolved;
    private TextView tvNoTickets;
    private String highlightTicketId;
    private String highlightMessage;

    private TicketApi ticketApi;
    private SessionManager sessionManager;
    private String currentHouseCode;

    private boolean raisedExpanded = true;
    private boolean inProgressExpanded = true;
    private boolean solvedExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_tickets);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_tickets);
        highlightTicketId = getIntent().getStringExtra("highlight_ticket_id");
        highlightMessage = getIntent().getStringExtra(LocalNotificationHelper.EXTRA_HIGHLIGHT_MESSAGE);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_tickets) return true;

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, TenantHomeActivity.class));
            } else if (itemId == R.id.nav_calendar) {
                startActivity(new Intent(this, TenantCalendarActivity.class));
            } else if (itemId == R.id.nav_bills) {
                startActivity(new Intent(this, TenantBillsActivity.class));
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, TenantProfileActivity.class));
            }
            overridePendingTransition(0, 0);
            finish();
            return true;
        });

        ticketApi = ApiClient.getTicketApi();
        sessionManager = new SessionManager(this);
        currentHouseCode = sessionManager.fetchHouseCode();
        if (currentHouseCode == null || currentHouseCode.isEmpty()) {
            Toast.makeText(this, "House code missing. Please re-login.", Toast.LENGTH_LONG).show();
            return;
        }

        containerRaised = findViewById(R.id.containerRaised);
        containerInProgress = findViewById(R.id.containerInProgress);
        containerSolved = findViewById(R.id.containerSolved);
        headerRaised = findViewById(R.id.headerRaised);
        headerInProgress = findViewById(R.id.headerInProgress);
        headerSolved = findViewById(R.id.headerSolved);
        ivToggleRaised = findViewById(R.id.ivToggleRaised);
        ivToggleInProgress = findViewById(R.id.ivToggleInProgress);
        ivToggleSolved = findViewById(R.id.ivToggleSolved);
        tvCountRaised = findViewById(R.id.tvCountRaised);
        tvCountInProgress = findViewById(R.id.tvCountInProgress);
        tvCountSolved = findViewById(R.id.tvCountSolved);
        tvNoTickets = findViewById(R.id.tvNoTickets);

        setupSectionToggles();

        findViewById(R.id.btnRaiseTicket).setOnClickListener(v -> {
            Intent intent = new Intent(TenantTicketsActivity.this, RaiseTicketActivity.class);
            intent.putExtra("EXTRA_HOUSE_CODE", currentHouseCode);
            startActivity(intent);
        });

        loadTickets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentHouseCode != null) {
            loadTickets();
        }
    }

    private void setupSectionToggles() {
        headerInProgress.setOnClickListener(v -> {
            inProgressExpanded = !inProgressExpanded;
            applySectionState(containerInProgress, ivToggleInProgress, inProgressExpanded);
        });
        headerRaised.setOnClickListener(v -> {
            raisedExpanded = !raisedExpanded;
            applySectionState(containerRaised, ivToggleRaised, raisedExpanded);
        });
        headerSolved.setOnClickListener(v -> {
            solvedExpanded = !solvedExpanded;
            applySectionState(containerSolved, ivToggleSolved, solvedExpanded);
        });

        applySectionState(containerInProgress, ivToggleInProgress, inProgressExpanded);
        applySectionState(containerRaised, ivToggleRaised, raisedExpanded);
        applySectionState(containerSolved, ivToggleSolved, solvedExpanded);
    }

    private void applySectionState(View container, ImageView toggle, boolean expanded) {
        container.setVisibility(expanded ? View.VISIBLE : View.GONE);
        toggle.animate().rotation(expanded ? 0f : -90f).setDuration(160).start();
    }

    private void updateSectionCounts() {
        tvCountInProgress.setText(String.valueOf(containerInProgress.getChildCount()));
        tvCountRaised.setText(String.valueOf(containerRaised.getChildCount()));
        tvCountSolved.setText(String.valueOf(containerSolved.getChildCount()));

        boolean empty = containerInProgress.getChildCount() == 0
                && containerRaised.getChildCount() == 0
                && containerSolved.getChildCount() == 0;
        tvNoTickets.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void loadTickets() {
        Log.d(TAG, "Fetching tickets for Apartment: " + currentHouseCode);

        ticketApi.getTicketsByApartment(currentHouseCode).enqueue(new Callback<List<Ticket>>() {
            @Override
            public void onResponse(Call<List<Ticket>> call, Response<List<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    populateLists(response.body());
                } else {
                    Log.e(TAG, "Failed to load tickets: " + response.code());
                    Toast.makeText(TenantTicketsActivity.this, "Could not load tickets", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Ticket>> call, Throwable t) {
                Log.e(TAG, "Network Error", t);
                com.example.uninest.utils.NetworkErrorDialog.show(
                        TenantTicketsActivity.this,
                        TenantTicketsActivity.this::loadTickets
                );
            }
        });
    }

    private void populateLists(List<Ticket> tickets) {
        containerRaised.removeAllViews();
        containerInProgress.removeAllViews();
        containerSolved.removeAllViews();

        Ticket highlightedTicket = null;

        for (Ticket ticket : tickets) {
            if (ticket.isDeletedByTenant()) {
                continue;
            }

            if (highlightedTicket == null
                    && highlightTicketId != null
                    && highlightTicketId.equals(ticket.getId())) {
                highlightedTicket = ticket;
            }

            String status = canonicalizeStatus(ticket.getStatus());
            if ("In Progress".equalsIgnoreCase(status)) {
                addTicketView(containerInProgress, ticket, 2);
            } else if ("Solved".equalsIgnoreCase(status)) {
                addTicketView(containerSolved, ticket, 3);
            } else {
                addTicketView(containerRaised, ticket, 1);
            }
        }

        updateSectionCounts();
        applySectionState(containerRaised, ivToggleRaised, raisedExpanded);
        applySectionState(containerInProgress, ivToggleInProgress, inProgressExpanded);
        applySectionState(containerSolved, ivToggleSolved, solvedExpanded);

        if (highlightedTicket != null) {
            if (highlightMessage != null && !highlightMessage.trim().isEmpty()) {
                Toast.makeText(this, highlightMessage, Toast.LENGTH_SHORT).show();
            }
            showTicketDetailsPopup(highlightedTicket);
            highlightTicketId = null;
            highlightMessage = null;
        }
    }

    private void addTicketView(LinearLayout container, Ticket ticket, int type) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_tenant_ticket_row, container, false);

        View cardContainer = view.findViewById(R.id.cardContainer);
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvRaisedBy = view.findViewById(R.id.tvRaisedBy);
        TextView tvRaised = view.findViewById(R.id.tvRaisedDate);
        TextView tvStateChip = view.findViewById(R.id.tvStateChip);
        TextView tvStatusMsg = view.findViewById(R.id.tvStatusMessage);
        TextView tvSolved = view.findViewById(R.id.tvSolvedDate);
        String status = canonicalizeStatus(ticket.getStatus());

        String room = ticket.getRoom() != null ? ticket.getRoom() : "General";
        String category = ticket.getCategory() != null ? ticket.getCategory() : "Issue";
        tvTitle.setText(room + ": " + category);
        tvStateChip.setText(status);
        applyStatusBadge(tvStateChip, status);

        String currentUserId = sessionManager.getUserId();
        if (ticket.getUserId() != null && ticket.getUserId().equals(currentUserId)) {
            tvRaisedBy.setText("Raised by you");
        } else {
            String name = ticket.getUserName() != null ? ticket.getUserName() : "Roommate";
            tvRaisedBy.setText("Raised by " + name);
        }

        tvRaised.setText("Raised: " + formatTimestamp(ticket.getCreatedAt(), true));

        if (type == 1) {
            cardContainer.setBackgroundResource(R.drawable.bg_tenant_ticket_card_raised);
            tvStatusMsg.setText("Awaiting letting agent review");
            tvSolved.setVisibility(View.GONE);
        } else if (type == 2) {
            cardContainer.setBackgroundResource(R.drawable.bg_tenant_ticket_card_progress);
            if (ticket.getArrivalDate() != null && !ticket.getArrivalDate().isEmpty()) {
                tvStatusMsg.setText("Agent arrival: " + ticket.getArrivalDate());
            } else {
                tvStatusMsg.setText("Agent is actively reviewing this issue");
            }
            tvSolved.setVisibility(View.GONE);
        } else {
            cardContainer.setBackgroundResource(R.drawable.bg_tenant_ticket_card_solved);
            if (ticket.getArrivalDate() != null && !ticket.getArrivalDate().isEmpty()) {
                tvStatusMsg.setText("Agent visit: " + ticket.getArrivalDate());
                tvStatusMsg.setVisibility(View.VISIBLE);
            } else {
                tvStatusMsg.setVisibility(View.VISIBLE);
                tvStatusMsg.setText("This ticket has been marked as solved");
            }
            tvSolved.setVisibility(View.VISIBLE);
            Object solvedAt = ticket.getUpdatedAt() != null ? ticket.getUpdatedAt() : ticket.getCreatedAt();
            tvSolved.setText("Solved: " + formatTimestamp(solvedAt, true));
        }
        cardContainer.setOnClickListener(v -> showTicketDetailsPopup(ticket));
        container.addView(view);
    }

    private String canonicalizeStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            return "Raised";
        }

        if ("Raised".equalsIgnoreCase(rawStatus) || "Open".equalsIgnoreCase(rawStatus)) {
            return "Raised";
        }

        if ("In_Process".equalsIgnoreCase(rawStatus)
                || "In Progress".equalsIgnoreCase(rawStatus)
                || "Medium".equalsIgnoreCase(rawStatus)) {
            return "In Progress";
        }

        if ("Resolved".equalsIgnoreCase(rawStatus)
                || "Closed".equalsIgnoreCase(rawStatus)
                || "Solved".equalsIgnoreCase(rawStatus)) {
            return "Solved";
        }

        return rawStatus.replace("_", " ").trim();
    }

    private String buildTicketLocation(Ticket ticket) {
        String room = ticket.getRoom() != null && !ticket.getRoom().trim().isEmpty()
                ? ticket.getRoom().trim()
                : "General";
        String apartment = ticket.getApartmentName() != null && !ticket.getApartmentName().trim().isEmpty()
                ? ticket.getApartmentName().trim()
                : currentHouseCode;
        String building = ticket.getBuilding() != null && !ticket.getBuilding().trim().isEmpty()
                ? ticket.getBuilding().trim()
                : null;

        if (building != null && apartment != null && !apartment.isEmpty()) {
            return room + " / " + apartment + " / " + building;
        }

        if (apartment != null && !apartment.isEmpty()) {
            return room + " / " + apartment;
        }

        return room;
    }

    private String formatTimestamp(Object obj, boolean includeTime) {
        Date parsed = parseDateObject(obj);
        if (parsed == null) {
            return "-";
        }

        String pattern = includeTime ? "dd MMM yyyy, h:mm a" : "dd MMM yyyy";
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(parsed);
    }

    private Date parseDateObject(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            if (obj instanceof Date) {
                return (Date) obj;
            } else if (obj instanceof Long) {
                return new Date((Long) obj);
            } else if (obj instanceof Double) {
                return new Date(((Double) obj).longValue());
            } else if (obj instanceof com.google.firebase.Timestamp) {
                return ((com.google.firebase.Timestamp) obj).toDate();
            } else if (obj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) obj;
                if (map.containsKey("seconds")) {
                    Object secondsObj = map.get("seconds");
                    long seconds = 0L;
                    if (secondsObj instanceof Double) {
                        seconds = ((Double) secondsObj).longValue();
                    } else if (secondsObj instanceof Long) {
                        seconds = (Long) secondsObj;
                    }
                    return new Date(seconds * 1000L);
                }
            } else if (obj instanceof String) {
                String value = (String) obj;
                String[] patterns = {
                        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                        "yyyy-MM-dd'T'HH:mm:ssX",
                        "yyyy-MM-dd"
                };
                for (String pattern : patterns) {
                    try {
                        return new SimpleDateFormat(pattern, Locale.US).parse(value);
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private void showTicketDetailsPopup(Ticket ticket) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_ticket_details, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        TextView tvTitle = view.findViewById(R.id.popTitle);
        TextView tvStatus = view.findViewById(R.id.popStatusBadge);
        TextView tvDesc = view.findViewById(R.id.popDesc);
        TextView tvLocation = view.findViewById(R.id.popLocation);
        TextView tvRaisedBy = view.findViewById(R.id.popRaisedBy);
        TextView tvRaisedAt = view.findViewById(R.id.popRaisedAt);
        TextView tvArrival = view.findViewById(R.id.popArrival);
        TextView tvAgentMsg = view.findViewById(R.id.popAgentMessage);
        View layoutAgentResponse = view.findViewById(R.id.layoutAgentResponse);
        ImageView ivPopImage = view.findViewById(R.id.popTicketImage);
        Button btnClose = view.findViewById(R.id.btnPopClose);
        Button btnDelete = view.findViewById(R.id.btnPopDelete);

        String room = ticket.getRoom() != null ? ticket.getRoom() : "General";
        String category = ticket.getCategory() != null ? ticket.getCategory() : "Issue";
        tvTitle.setText(room + ": " + category);
        tvDesc.setText(ticket.getDescription() == null || ticket.getDescription().trim().isEmpty()
                ? "No additional description provided."
                : ticket.getDescription().trim());
        tvLocation.setText(buildTicketLocation(ticket));
        tvRaisedAt.setText(formatTimestamp(ticket.getCreatedAt(), true));
        tvArrival.setText(ticket.getArrivalDate() != null && !ticket.getArrivalDate().isEmpty()
                ? ticket.getArrivalDate()
                : "Not scheduled");

        if (ticket.getUserId() != null && ticket.getUserId().equals(sessionManager.getUserId())) {
            tvRaisedBy.setText("You");
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            tvRaisedBy.setText(ticket.getUserName() != null ? ticket.getUserName() : "Roommate");
            btnDelete.setVisibility(View.GONE);
        }

        String status = canonicalizeStatus(ticket.getStatus());
        tvStatus.setText(status);
        applyStatusBadge(tvStatus, status);

        if (ticket.getAgentResponse() != null && !ticket.getAgentResponse().trim().isEmpty()) {
            layoutAgentResponse.setVisibility(View.VISIBLE);
            tvAgentMsg.setText(ticket.getAgentResponse().trim());
        } else {
            layoutAgentResponse.setVisibility(View.GONE);
        }

        btnDelete.setOnClickListener(v -> {
            String ticketLabel = room + ": " + category;

            DestructiveConfirmationDialog.show(
                    this,
                    "Remove ticket",
                    "Remove " + ticketLabel + "?",
                    "This ticket will disappear from your active list.",
                    "You can't undo this from the app.",
                    "Remove ticket",
                    () -> {
                        deleteTicket(ticket.getId());
                        dialog.dismiss();
                    }
            );
        });

        if (ticket.getImageUrl() != null && !ticket.getImageUrl().isEmpty()) {
            ivPopImage.setVisibility(View.VISIBLE);
            ImageUtils.loadTicketImage(ivPopImage, ticket.getImageUrl());
        } else {
            ivPopImage.setVisibility(View.GONE);
        }

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92f),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        btnClose.setOnClickListener(v -> dialog.dismiss());
    }

    private void applyStatusBadge(TextView badge, String status) {
        if ("Raised".equalsIgnoreCase(status) || "Open".equalsIgnoreCase(status)) {
            badge.setBackgroundResource(R.drawable.bg_tenant_ticket_status_raised);
            badge.setTextColor(getColor(R.color.ticket_raised_text));
        } else if ("In_Process".equalsIgnoreCase(status)
                || "In Progress".equalsIgnoreCase(status)
                || "Medium".equalsIgnoreCase(status)) {
            badge.setBackgroundResource(R.drawable.bg_tenant_ticket_status_progress);
            badge.setTextColor(getColor(R.color.ticket_progress_text));
        } else {
            badge.setBackgroundResource(R.drawable.bg_tenant_ticket_status_solved);
            badge.setTextColor(getColor(R.color.ticket_solved_text));
        }
    }

    private void deleteTicket(String ticketId) {
        ticketApi.softDeleteTicket(ticketId).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TenantTicketsActivity.this, "Ticket deleted", Toast.LENGTH_SHORT).show();
                    loadTickets();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(TenantTicketsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
