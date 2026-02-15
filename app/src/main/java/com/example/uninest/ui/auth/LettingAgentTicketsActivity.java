package com.example.uninest.ui.auth;

import android.content.Intent;
import android.graphics.Color;
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

    private String selectedPriority = "";
    private String selectedState = "";
    private String selectedSort = "";
    private boolean aiOnly = false;


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

        ImageButton btnAiToggle = findViewById(R.id.btnAiToggle);
        btnAiToggle.setOnClickListener(v -> {
            // 1. Toggle the state
            aiOnly = !aiOnly;


            if (aiOnly) {
                btnAiToggle.setColorFilter(Color.parseColor("#FFD700"));
                btnAiToggle.setAlpha(1.0f);
                Toast.makeText(this, "AI Prioritized Only", Toast.LENGTH_SHORT).show();

            } else {
                btnAiToggle.setColorFilter(Color.WHITE);
                btnAiToggle.setAlpha(0.7f);
            }

            // 3. Trigger the filter
            adapter.applyAdvancedFilter(selectedPriority, selectedState, selectedSort, aiOnly);
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
        setupBottomNav(R.id.nav_tickets);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTicketsByLandlordId();
    }

    private void loadTicketsByLandlordId() {
        String myAgentId = FirebaseAuth.getInstance().getUid();

        Log.d("DEBUG_TICKETS", "Agent ID: " + myAgentId);

        if (myAgentId == null) return;

        ticketApi.getTicketsByLandlord(myAgentId).enqueue(new Callback<List<Ticket>>() {
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
                Toast.makeText(LettingAgentTicketsActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFilterDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_filter_tickets, null);

        RadioGroup rgSort = v.findViewById(R.id.rgSort);
        TextView tvClear = v.findViewById(R.id.tvClearAll);

        TextView[] pChips = { v.findViewById(R.id.chipHigh), v.findViewById(R.id.chipMedium), v.findViewById(R.id.chipLow) };
        TextView[] sChips = { v.findViewById(R.id.stateRaised), v.findViewById(R.id.stateProgress), v.findViewById(R.id.stateSolved) };


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
        updateChipSelectionUI(pChips, selectedPriority);
        updateChipSelectionUI(sChips, selectedState);

        // --- 2. Click Listeners ---
        for (TextView chip : pChips) {
            chip.setOnClickListener(view -> {
                selectedPriority = ((TextView)view).getText().toString();
                updateChipSelectionUI(pChips, selectedPriority);
            });
        }

        for (TextView chip : sChips) {
            chip.setOnClickListener(view -> {
                String text = ((TextView)view).getText().toString();
                // Map UI names to backend names
                if (text.equals("In Progress")) selectedState = "In_Process";
                else if (text.equals("Solved")) selectedState = "Resolved";
                else selectedState = "Raised";

                updateChipSelectionUI(sChips, selectedState);
            });
        }

        // --- 3. CLEAR ALL Logic ---
        tvClear.setOnClickListener(view -> {
            selectedPriority = "";
            selectedState = "";
            selectedSort = "";


            rgSort.clearCheck();


            if (etSearch != null) etSearch.setText("");

            updateChipSelectionUI(pChips, "");
            updateChipSelectionUI(sChips, "");
        });

        builder.setView(v);
        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        v.findViewById(R.id.btnSubmitFilter).setOnClickListener(view -> {

            // Capture Sort selection
            int checkedId = rgSort.getCheckedRadioButtonId();
            if (checkedId == R.id.rbBuilding) selectedSort = "Building";
            else if (checkedId == R.id.rbPriority) selectedSort = "Priority";
            else if (checkedId == R.id.rbDate) selectedSort = "Date";
            else selectedSort = ""; // No sort selected


            adapter.applyAdvancedFilter(selectedPriority, selectedState, selectedSort, aiOnly);
            dialog.dismiss();
        });
        v.findViewById(R.id.btnClose).setOnClickListener(view -> dialog.dismiss());
    }


    private void updateChipSelectionUI(TextView[] group, String selectedValue) {
        for (TextView chip : group) {
            String chipVal = chip.getText().toString();

            if (chipVal.equals("In Progress") && selectedValue.equals("In_Process")) chipVal = "In_Process";
            if (chipVal.equals("Solved") && selectedValue.equals("Resolved")) chipVal = "Resolved";

            if (chipVal.equalsIgnoreCase(selectedValue)) {

                chip.setBackgroundResource(R.drawable.bg_chip_selected);
                chip.setTypeface(null, Typeface.BOLD);
            } else {

                chip.setBackgroundResource(R.drawable.bg_chip_unselected);
                chip.setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    private void setupBottomNav(int selectedId) {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(selectedId);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // Prevent reloading the same activity
            if (itemId == selectedId) return true;

            if (itemId == R.id.nav_tickets) {
                startActivity(new Intent(this, LettingAgentTicketsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_buildings) {
                startActivity(new Intent(this, LettingAgentBuildingsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                // startActivity(new Intent(this, LettingAgentProfileActivity.class));
                // overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

}