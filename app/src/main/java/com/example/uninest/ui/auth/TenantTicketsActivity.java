package com.example.uninest.ui.auth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.TicketApi;
import com.example.uninest.model.Ticket;
import com.example.uninest.ui.auth.RaiseTicketActivity;
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

    // UI Containers
    private LinearLayout containerRaised, containerInProgress, containerSolved;
    private TextView tvNoTickets;
    private String highlightTicketId;
    // Data
    private TicketApi ticketApi;
    private SessionManager sessionManager;
    private String currentHouseCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_tickets);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_tickets); // Highlight Tickets

        highlightTicketId = getIntent().getStringExtra("highlight_ticket_id");

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

        // 1. Initialize API & Session
        ticketApi = ApiClient.getTicketApi();
        sessionManager = new SessionManager(this);

        // 2. Get House Code (Apartment ID)
        currentHouseCode = sessionManager.fetchHouseCode();
        if (currentHouseCode == null || currentHouseCode.isEmpty()) {
            Toast.makeText(this, "House code missing. Please re-login.", Toast.LENGTH_LONG).show();
            // Ideally, redirect to login here
            return;
        }

        // 3. Init UI
        containerRaised = findViewById(R.id.containerRaised);
        containerInProgress = findViewById(R.id.containerInProgress);
        containerSolved = findViewById(R.id.containerSolved);

        // 4. Raise Ticket Button Logic
        findViewById(R.id.btnRaiseTicket).setOnClickListener(v -> {
            Intent intent = new Intent(TenantTicketsActivity.this, RaiseTicketActivity.class);
            // Pass the house code to RaiseTicketActivity so it doesn't fail
            intent.putExtra("EXTRA_HOUSE_CODE", currentHouseCode);
            startActivity(intent);
        });

        // 5. Load Data
        loadTickets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload tickets when coming back from "Raise Ticket" screen
        if (currentHouseCode != null) {
            loadTickets();
        }
    }

    private void loadTickets() {
        Log.d(TAG, "Fetching tickets for Apartment: " + currentHouseCode);

        ticketApi.getTicketsByApartment(currentHouseCode).enqueue(new Callback<List<Ticket>>() {
            @Override
            public void onResponse(Call<List<Ticket>> call, Response<List<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Ticket> tickets = response.body();
                    Log.d(TAG, "Tickets found: " + tickets.size());
                    populateLists(tickets);
                } else {
                    Log.e(TAG, "Failed to load tickets: " + response.code());
                    Toast.makeText(TenantTicketsActivity.this, "Could not load tickets", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Ticket>> call, Throwable t) {
                Log.e(TAG, "Network Error", t);
                Toast.makeText(TenantTicketsActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateLists(List<Ticket> tickets) {
        // Clear previous views to avoid duplicates
        containerRaised.removeAllViews();
        containerInProgress.removeAllViews();
        containerSolved.removeAllViews();
        boolean matchedHighlightedTicket = false;

        if (tickets.isEmpty()) {
            return;
        }

        for (Ticket t : tickets) {
            if (!matchedHighlightedTicket
                    && highlightTicketId != null
                    && highlightTicketId.equals(t.getId())) {
                Toast.makeText(this, "Opened related ticket", Toast.LENGTH_SHORT).show();
                matchedHighlightedTicket = true;
            }

            String status = t.getStatus() != null ? t.getStatus() : "Raised";

            // LOGIC: Group tickets into 3 categories
            if ("Raised".equalsIgnoreCase(status) || "Open".equalsIgnoreCase(status)) {
                addTicketView(containerRaised, t, 1); // 1 = Raised (Red)
            } else if ("In_Process".equalsIgnoreCase(status) || "In Progress".equalsIgnoreCase(status) || "Medium".equalsIgnoreCase(status)) {
                addTicketView(containerInProgress, t, 2); // 2 = In Progress (Orange)
            } else if ("Resolved".equalsIgnoreCase(status) || "Closed".equalsIgnoreCase(status) || "Solved".equalsIgnoreCase(status)) {
                addTicketView(containerSolved, t, 3); // 3 = Solved (Green)
            }
        }
    }

    /**
     * @param type 1=Raised (Red), 2=Progress (Orange), 3=Solved (Green)
     */
    private void addTicketView(LinearLayout container, Ticket t, int type) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_tenant_ticket_row, container, false);

        View cardContainer = view.findViewById(R.id.cardContainer);
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvRaisedBy = view.findViewById(R.id.tvRaisedBy);
        TextView tvRaised = view.findViewById(R.id.tvRaisedDate);
        TextView tvStatusMsg = view.findViewById(R.id.tvStatusMessage);
        TextView tvSolved = view.findViewById(R.id.tvSolvedDate);

        // 1. Set Title (Room: Category)
        String room = t.getRoom() != null ? t.getRoom() : "General";
        String cat = t.getCategory() != null ? t.getCategory() : "Issue";
        tvTitle.setText(room + ": " + cat);

        // 2. LOGIC: Set "Raised by me" or "Raised by [Name]"
        String currentUserId = sessionManager.getUserId();
        if (t.getUserId() != null && t.getUserId().equals(currentUserId)) {
            tvRaisedBy.setText("Raised by: Me");
            tvRaisedBy.setTypeface(null, android.graphics.Typeface.BOLD); // Optional: make 'me' bold
        } else {
            String name = (t.getUserName() != null) ? t.getUserName() : "Roommate";
            tvRaisedBy.setText("Raised by: " + name);
        }

        // 2. Set Raised Date
        tvRaised.setText("Raised on: " + parseDate(t.getCreatedAt()));

        // 3. Style based on Type
        if (type == 1) {
            // RAISED
            cardContainer.setBackgroundResource(R.drawable.bg_card_border_raised);
            tvStatusMsg.setText("Waiting for Letting Agent");
            tvSolved.setVisibility(View.GONE);
        } else if (type == 2) {
            // IN PROGRESS
            cardContainer.setBackgroundResource(R.drawable.bg_card_border_progress);

            // Show Arrival Date if exists
            if (t.getArrivalDate() != null && !t.getArrivalDate().isEmpty()) {
                tvStatusMsg.setText("Agent arrival: " + t.getArrivalDate());
                tvStatusMsg.setTextColor(getColor(R.color.black));
            } else {
                tvStatusMsg.setText("Agent is reviewing...");
            }
            tvSolved.setVisibility(View.GONE);
        } else if (type == 3) {
            // SOLVED
            cardContainer.setBackgroundResource(R.drawable.bg_card_border_solved);

            if (t.getArrivalDate() != null && !t.getArrivalDate().isEmpty()) {
                tvStatusMsg.setText("Agent visit: " + t.getArrivalDate());
            } else {
                tvStatusMsg.setVisibility(View.GONE);
            }

            tvSolved.setVisibility(View.VISIBLE);
            Object dateObj = t.getUpdatedAt() != null ? t.getUpdatedAt() : t.getCreatedAt();
            tvSolved.setText("Solved: " + parseDate(dateObj));
        }
        cardContainer.setOnClickListener(v -> showTicketDetailsPopup(t));
        // Add to the specific container
        container.addView(view);
    }

    // Helper to parse Dates from Backend (Map/Timestamp/String)
    private String parseDate(Object obj) {
        if (obj == null) return "-";
        try {
            if (obj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) obj;
                if (map.containsKey("seconds")) {
                    Object secObj = map.get("seconds");
                    long seconds = 0;
                    if (secObj instanceof Double) seconds = ((Double) secObj).longValue();
                    else if (secObj instanceof Long) seconds = (Long) secObj;
                    return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(seconds * 1000));
                }
            } else if (obj instanceof String) {
                String s = (String) obj;
                if (s.length() >= 10) return s.substring(0, 10);
                return s;
            }
        } catch (Exception e) {
            return "-";
        }
        return "-";
    }

    private void showTicketDetailsPopup(Ticket t) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_ticket_details, null);

        // References
        TextView tvTitle = view.findViewById(R.id.popTitle);
        TextView tvStatus = view.findViewById(R.id.popStatusBadge);
        TextView tvDesc = view.findViewById(R.id.popDesc);
        TextView tvLocation = view.findViewById(R.id.popLocation);
        TextView tvRaisedBy = view.findViewById(R.id.popRaisedBy);
        TextView tvArrival = view.findViewById(R.id.popArrival);
        TextView tvAgentMsg = view.findViewById(R.id.popAgentMessage);
        View layoutAgentResponse = view.findViewById(R.id.layoutAgentResponse);
        Button btnClose = view.findViewById(R.id.btnPopClose);

        // Data population
        String room = t.getRoom() != null ? t.getRoom() : "General";
        String cat = t.getCategory() != null ? t.getCategory() : "Issue";
        tvTitle.setText(room + ": " + cat);

        tvDesc.setText(t.getDescription());
        tvLocation.setText(room);

        // Raised By Logic
        if (t.getUserId() != null && t.getUserId().equals(sessionManager.getUserId())) {
            tvRaisedBy.setText("Me");
        } else {
            tvRaisedBy.setText(t.getUserName() != null ? t.getUserName() : "Roommate");
        }

        // Status Pill Styling
        String status = t.getStatus() != null ? t.getStatus() : "Raised";
        tvStatus.setText(status.toUpperCase().replace("_", " "));

        if (status.equalsIgnoreCase("Raised") || status.equalsIgnoreCase("Open")) {
            tvStatus.setTextColor(Color.parseColor("#C62828")); // Dark Red
            tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
        } else if (status.toLowerCase().contains("process")) {
            tvStatus.setTextColor(Color.parseColor("#EF6C00")); // Dark Orange
            tvStatus.setBackgroundResource(R.drawable.bg_status_progress);
        } else {
            tvStatus.setTextColor(Color.parseColor("#2E7D32")); // Dark Green
            tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
        }

        // Agent Arrival & Message
        tvArrival.setText(t.getArrivalDate() != null && !t.getArrivalDate().isEmpty() ? t.getArrivalDate() : "Not scheduled");

        if (t.getAgentResponse() != null && !t.getAgentResponse().trim().isEmpty()) {
            layoutAgentResponse.setVisibility(View.VISIBLE);
            tvAgentMsg.setText(t.getAgentResponse());
        } else {
            layoutAgentResponse.setVisibility(View.GONE);
        }

        // Show Dialog
        builder.setView(view);
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}