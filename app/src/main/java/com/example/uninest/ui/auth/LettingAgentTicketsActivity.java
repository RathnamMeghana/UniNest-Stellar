package com.example.uninest.ui.auth;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.BuildingApi;
import com.example.uninest.data.api.TicketApi;
import com.example.uninest.model.Building;
import com.example.uninest.model.Ticket;
import com.example.uninest.utils.AgentBottomNavHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LettingAgentTicketsActivity extends AppCompatActivity {

    private TicketCardAdapter adapter;
    private TicketApi ticketApi;
    private EditText etSearch;
    private ImageButton btnAiToggle;

    private String selectedPriority = "";
    private String selectedState = "";
    private String selectedSort = "";
    private boolean aiOnly = false;
    private boolean showDeleted = false;


    private List<Ticket> allTickets = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letting_agent_tickets);

        ticketApi = ApiClient.getTicketApi();

        etSearch = findViewById(R.id.etSearch);
        RecyclerView rv = findViewById(R.id.rvTickets);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TicketCardAdapter(this, new ArrayList<>(), item -> {
            Intent intent = new Intent(LettingAgentTicketsActivity.this, LettingAgentTicketDetailsActivity.class);
            intent.putExtra("TICKET_OBJ", item); // Pass the whole object
            startActivity(intent);
        });

        rv.setAdapter(adapter);

        btnAiToggle = findViewById(R.id.btnAiToggle);
        updateAiToggleUi();
        btnAiToggle.setOnClickListener(v -> {
            aiOnly = !aiOnly;
            updateAiToggleUi();
            if (aiOnly) {
                Toast.makeText(this, "AI Prioritized Only", Toast.LENGTH_SHORT).show();
            }
            applyTicketFilters();
        });

        loadTicketsByLandlordId();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Navigation
        findViewById(R.id.btnFilter).setOnClickListener(v -> showFilterDialog());
        AgentBottomNavHelper.setup(this, R.id.nav_tickets);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AgentBottomNavHelper.syncSelected(this, R.id.nav_tickets);
        loadTicketsByLandlordId();
    }

    private void loadTicketsByLandlordId() {
        String myAgentId = FirebaseAuth.getInstance().getUid();

        Log.d("DEBUG_TICKETS", "Agent ID: " + myAgentId);

        if (myAgentId == null) return;

        ticketApi.getTicketsByLandlord(myAgentId, showDeleted).enqueue(new Callback<List<Ticket>>() {
            @Override
            public void onResponse(Call<List<Ticket>> call, Response<List<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allTickets = response.body();

                    Log.d("DEBUG_TICKETS", "Tickets Found: " + allTickets.size());

                    adapter.setData(allTickets);

                    if (allTickets.isEmpty()) {
                        Toast.makeText(LettingAgentTicketsActivity.this, "No tickets found", Toast.LENGTH_SHORT).show();
                    }
                }
                    else {
                        Log.e("DEBUG_TICKETS", "API Fail: " + response.code());
                    }
                }

            @Override
            public void onFailure(Call<List<Ticket>> call, Throwable t) {
                Log.e("DEBUG_TICKETS", "Network Error", t);
                com.example.uninest.utils.NetworkErrorDialog.show(
                        LettingAgentTicketsActivity.this,
                        LettingAgentTicketsActivity.this::loadTicketsByLandlordId
                );
            }
        });
    }

    private void showFilterDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_filter_tickets, null);

        RadioGroup rgSort = v.findViewById(R.id.rgSort);
        TextView tvClear = v.findViewById(R.id.tvClearAll);

        androidx.appcompat.widget.SwitchCompat swDeleted = v.findViewById(R.id.swDeleted);


        TextView[] pChips = { v.findViewById(R.id.chipHigh), v.findViewById(R.id.chipMedium), v.findViewById(R.id.chipLow) };
        TextView[] sChips = { v.findViewById(R.id.stateRaised), v.findViewById(R.id.stateProgress), v.findViewById(R.id.stateSolved) };
        final String[] dialogPriority = {selectedPriority};
        final String[] dialogState = {selectedState};

        swDeleted.setChecked(showDeleted);

        if (selectedSort.equals("Building")) {
            rgSort.check(R.id.rbBuilding);
        } else if (selectedSort.equals("Priority")) {
            rgSort.check(R.id.rbPriority);
        } else if (selectedSort.equals("Date")) {
            rgSort.check(R.id.rbDate);
        } else {
            rgSort.clearCheck();
        }

        // Initial UI update for chips
        updateChipSelectionUI(pChips, dialogPriority[0]);
        updateChipSelectionUI(sChips, dialogState[0]);

        for (TextView chip : pChips) {
            chip.setOnClickListener(view -> {
                String value = ((TextView) view).getText().toString();
                dialogPriority[0] = value.equalsIgnoreCase(dialogPriority[0]) ? "" : value;
                updateChipSelectionUI(pChips, dialogPriority[0]);
            });
        }

        for (TextView chip : sChips) {
            chip.setOnClickListener(view -> {
                String mappedState = mapDialogState(((TextView) view).getText().toString());
                dialogState[0] = mappedState.equalsIgnoreCase(dialogState[0]) ? "" : mappedState;
                updateChipSelectionUI(sChips, dialogState[0]);
            });
        }

        tvClear.setOnClickListener(view -> {
            dialogPriority[0] = "";
            dialogState[0] = "";
            swDeleted.setChecked(false);
            rgSort.clearCheck();
            updateChipSelectionUI(pChips, "");
            updateChipSelectionUI(sChips, "");

            boolean previousShowDeleted = showDeleted;
            selectedPriority = "";
            selectedState = "";
            selectedSort = "";
            showDeleted = false;

            if (showDeleted != previousShowDeleted) {
                loadTicketsByLandlordId();
            } else {
                applyTicketFilters();
            }
        });

        builder.setView(v);
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        v.findViewById(R.id.btnSubmitFilter).setOnClickListener(view -> {

            int checkedId = rgSort.getCheckedRadioButtonId();
            String dialogSort;
            if (checkedId == R.id.rbBuilding) dialogSort = "Building";
            else if (checkedId == R.id.rbPriority) dialogSort = "Priority";
            else if (checkedId == R.id.rbDate) dialogSort = "Date";
            else dialogSort = "";

            boolean previousShowDeleted = showDeleted;
            selectedPriority = dialogPriority[0];
            selectedState = dialogState[0];
            selectedSort = dialogSort;
            showDeleted = swDeleted.isChecked();

            if (showDeleted != previousShowDeleted) {
                loadTicketsByLandlordId();
            } else {
                applyTicketFilters();
            }
            dialog.dismiss();
        });
        v.findViewById(R.id.btnClose).setOnClickListener(view -> dialog.dismiss());
    }


    private void updateChipSelectionUI(TextView[] group, String selectedValue) {
        for (TextView chip : group) {
            String chipVal = chip.getText().toString();

            if (chipVal.equals("In Progress") && selectedValue.equals("In_Process")) chipVal = "In_Process";
            if (chipVal.equals("Solved") && selectedValue.equals("Resolved")) chipVal = "Resolved";

            boolean isSelected = chipVal.equalsIgnoreCase(selectedValue);
            applyFilterChipStyle(chip, isSelected);
        }
    }

    private void applyTicketFilters() {
        adapter.applyAdvancedFilter(selectedPriority, selectedState, selectedSort, aiOnly);
    }

    private void updateAiToggleUi() {
        if (btnAiToggle == null) {
            return;
        }

        btnAiToggle.setBackgroundResource(R.drawable.bg_tenant_calendar_action_primary);
        btnAiToggle.setAlpha(1.0f);

        if (aiOnly) {
            btnAiToggle.setColorFilter(Color.parseColor("#FFD700"));
        } else {
            btnAiToggle.setColorFilter(Color.WHITE);
        }
    }

    private String mapDialogState(String text) {
        if ("In Progress".equals(text)) {
            return "In_Process";
        }
        if ("Solved".equals(text)) {
            return "Resolved";
        }
        return "Raised";
    }

    private void applyFilterChipStyle(TextView chip, boolean isSelected) {
        int fillColor;
        int strokeColor;
        int textColor;

        int chipId = chip.getId();
        if (chipId == R.id.chipHigh) {
            fillColor = getColor(isSelected ? R.color.chip_high_text : R.color.chip_high_bg);
            strokeColor = getColor(R.color.chip_high_text);
            textColor = isSelected ? Color.WHITE : getColor(R.color.chip_high_text);
        } else if (chipId == R.id.chipMedium) {
            fillColor = getColor(isSelected ? R.color.chip_medium_text : R.color.chip_medium_bg);
            strokeColor = getColor(R.color.chip_medium_text);
            textColor = isSelected ? Color.WHITE : getColor(R.color.chip_medium_text);
        } else if (chipId == R.id.chipLow) {
            fillColor = getColor(isSelected ? R.color.chip_low_text : R.color.chip_low_bg);
            strokeColor = getColor(R.color.chip_low_text);
            textColor = isSelected ? Color.WHITE : getColor(R.color.chip_low_text);
        } else if (chipId == R.id.stateProgress) {
            fillColor = getColor(isSelected ? R.color.ticket_progress_text : R.color.ticket_progress_badge_bg);
            strokeColor = getColor(R.color.ticket_progress_text);
            textColor = isSelected ? Color.WHITE : getColor(R.color.ticket_progress_text);
        } else if (chipId == R.id.stateSolved) {
            fillColor = getColor(isSelected ? R.color.ticket_solved_text : R.color.ticket_solved_badge_bg);
            strokeColor = getColor(R.color.ticket_solved_text);
            textColor = isSelected ? Color.WHITE : getColor(R.color.ticket_solved_text);
        } else {
            fillColor = getColor(isSelected ? R.color.ticket_raised_text : R.color.ticket_raised_badge_bg);
            strokeColor = getColor(R.color.ticket_raised_text);
            textColor = isSelected ? Color.WHITE : getColor(R.color.ticket_raised_text);
        }

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(dp(18));
        background.setColor(fillColor);
        background.setStroke(dp(isSelected ? 2 : 1), strokeColor);

        chip.setBackground(background);
        chip.setTextColor(textColor);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setAlpha(isSelected ? 1f : 0.96f);
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

}
