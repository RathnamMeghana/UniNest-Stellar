package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.BuildingApi;
import com.example.uninest.model.Apartment;
import com.example.uninest.model.Building;
import com.example.uninest.model.SendNotificationRequest;
import com.example.uninest.model.User;
import com.example.uninest.utils.AgentBottomNavHelper;
import com.example.uninest.utils.NetworkErrorDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LettingAgentNotificationsActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    private Chip chipEveryone;
    private Chip chipPickTenants;
    private Spinner spinnerBuildings;
    private Spinner spinnerApartments;

    // Multi-select components
    private RecyclerView rvTenantsList;
    private TenantSelectionAdapter tenantAdapter;

    private EditText etTitle;
    private EditText etBody;
    private MaterialButton btnSend;
    private TextView tvSelectedBuilding;
    private TextView tvSelectedApartment;
    private TextView tvMessageCount;

    private final List<Building> buildingList = new ArrayList<>();
    private final List<String> buildingNames = new ArrayList<>();
    private final List<Apartment> apartmentList = new ArrayList<>();
    private final List<String> apartmentNames = new ArrayList<>();

    private String apartmentName;
    private String houseCode;
    private String selectedBuildingId;
    private String selectedBuildingName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letting_agent_notifications);

        sessionManager = new SessionManager(this);

        // UI Binding
        chipEveryone = findViewById(R.id.chipEveryone);
        chipPickTenants = findViewById(R.id.chipPickTenants);
        spinnerBuildings = findViewById(R.id.spinnerBuildings);
        spinnerApartments = findViewById(R.id.spinnerApartments);

        // Initialize Multi-select RecyclerView
        rvTenantsList = findViewById(R.id.rvTenantsList);
        tenantAdapter = new TenantSelectionAdapter();
        rvTenantsList.setLayoutManager(new LinearLayoutManager(this));
        rvTenantsList.setAdapter(tenantAdapter);

        etTitle = findViewById(R.id.etNotificationTitle);
        etBody = findViewById(R.id.etNotificationBody);
        btnSend = findViewById(R.id.btnSendNotification);
        tvMessageCount = findViewById(R.id.tvMessageCount);
        tvSelectedBuilding = findViewById(R.id.tvSelectedBuilding);
        tvSelectedApartment = findViewById(R.id.tvSelectedApartment);

        // Initial UI State
        chipEveryone.setChecked(true);
        rvTenantsList.setVisibility(View.GONE);

        // Chip Logic for Audience Selection
        chipEveryone.setOnClickListener(v -> {
            chipEveryone.setChecked(true);
            chipPickTenants.setChecked(false);
            rvTenantsList.setVisibility(View.GONE);
        });

        chipPickTenants.setOnClickListener(v -> {
            chipPickTenants.setChecked(true);
            chipEveryone.setChecked(false);
            rvTenantsList.setVisibility(View.VISIBLE);
            loadTenants(); // Fetch roommates for the currently selected apartment
        });

        // Building Selection Listener
        spinnerBuildings.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < buildingList.size()) {
                    Building selectedBuilding = buildingList.get(position);
                    selectedBuildingId = selectedBuilding.getId();
                    selectedBuildingName = selectedBuilding.getName();
                    updateBuildingLabel();
                    loadApartmentsForBuilding(selectedBuildingId);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Apartment Selection Listener
        spinnerApartments.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < apartmentList.size()) {
                    Apartment selectedApartment = apartmentList.get(position);
                    houseCode = selectedApartment.getCode();
                    apartmentName = selectedApartment.getName();
                    updateApartmentLabel();

                    // If we are in "Pick Tenants" mode, refresh the list for the new apartment
                    if (chipPickTenants.isChecked()) {
                        loadTenants();
                    }
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Character Counter Logic
        etBody.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvMessageCount.setText(s.length() + "/200");
                tvMessageCount.setTextColor(s.length() >= 180 ?
                        getColor(android.R.color.holo_red_dark) : getColor(android.R.color.darker_gray));
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        loadBuildings();
        AgentBottomNavHelper.setup(this, R.id.nav_notifications);
        btnSend.setOnClickListener(v -> sendNotification());
    }

    private void updateApartmentLabel() {
        if (apartmentName == null || apartmentName.isEmpty()) {
            tvSelectedApartment.setText("Apartment: Not selected");
        } else {
            tvSelectedApartment.setText("Apartment: " + apartmentName + " (" + houseCode + ")");
        }
    }

    private void updateBuildingLabel() {
        tvSelectedBuilding.setText(selectedBuildingName == null ? "Building: Not selected" : "Building: " + selectedBuildingName);
    }

    private void loadBuildings() {
        String landlordId = FirebaseAuth.getInstance().getUid();
        if (landlordId == null) return;
        ApiClient.getBuildingApi().getBuildingsByLandlord(landlordId).enqueue(new Callback<List<Building>>() {
            @Override
            public void onResponse(Call<List<Building>> call, Response<List<Building>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bindBuildings(response.body());
                }
            }
            @Override public void onFailure(Call<List<Building>> call, Throwable t) {
                Toast.makeText(LettingAgentNotificationsActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindBuildings(List<Building> buildings) {
        buildingList.clear();
        buildingNames.clear();
        buildingList.addAll(buildings);
        for (Building b : buildings) buildingNames.add(b.getName());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_calendar_spinner_selected, buildingNames);
        adapter.setDropDownViewResource(R.layout.item_calendar_spinner_dropdown);
        spinnerBuildings.setAdapter(adapter);
    }

    private void loadApartmentsForBuilding(String buildingId) {
        ApiClient.getApartmentApi().getApartmentsByBuilding(buildingId).enqueue(new Callback<List<Apartment>>() {
            @Override
            public void onResponse(Call<List<Apartment>> call, Response<List<Apartment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bindApartments(response.body());
                }
            }
            @Override public void onFailure(Call<List<Apartment>> call, Throwable t) { }
        });
    }

    private void bindApartments(List<Apartment> apartments) {
        apartmentList.clear();
        apartmentNames.clear();
        apartmentList.addAll(apartments);
        for (Apartment a : apartments) apartmentNames.add(a.getName() + " (" + a.getCode() + ")");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_calendar_spinner_selected, apartmentNames);
        adapter.setDropDownViewResource(R.layout.item_calendar_spinner_dropdown);
        spinnerApartments.setAdapter(adapter);
    }

    private void loadTenants() {
        if (houseCode == null || houseCode.isEmpty()) return;

        ApiClient.getUserApi().getRoommates(houseCode).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<User> filtered = new ArrayList<>();
                    for (User u : response.body()) {
                        if ("2".equals(u.getRole()) || "TENANT".equalsIgnoreCase(u.getRole())) {
                            filtered.add(u);
                        }
                    }
                    tenantAdapter.setTenants(filtered);
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) { }
        });
    }

    private void sendNotification() {
        String title = etTitle.getText().toString().trim();
        String body = etBody.getText().toString().trim();

        if (houseCode == null || houseCode.isEmpty()) {
            Toast.makeText(this, "Select an apartment", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> targetIds = null; // Default to null for Everyone

        if (chipPickTenants.isChecked()) {
            targetIds = tenantAdapter.getSelectedUserIds();
            if (targetIds.isEmpty()) {
                Toast.makeText(this, "Please select at least one tenant", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        btnSend.setEnabled(false);
        btnSend.setText("Sending...");

        SendNotificationRequest request = new SendNotificationRequest(houseCode, targetIds, title, body);

        ApiClient.getNotificationApi().sendNotification(request).enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(Call<Integer> call, Response<Integer> response) {
                btnSend.setEnabled(true);
                btnSend.setText("Send Message");

                if (response.isSuccessful()) {
                    // Clear the form for the next message
                    etTitle.setText("");
                    etBody.setText("");
                    tvMessageCount.setText("0/200");

                    // Professional UI Feedback: Green Success Snackbar
                    Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                            "Notification Dispatched Successfully", Snackbar.LENGTH_LONG);

                    // Setting a success green background
                    snackbar.setBackgroundTint(getColor(android.R.color.holo_green_dark));
                    snackbar.setTextColor(getColor(android.R.color.white));

                    // Add a Dismiss button
                    snackbar.setAction("OK", v -> snackbar.dismiss());
                    snackbar.setActionTextColor(getColor(android.R.color.white));

                    snackbar.show();

                } else {
                    Toast.makeText(LettingAgentNotificationsActivity.this,
                            "Failed to send message: Server error",
                            Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<Integer> call, Throwable t) {
                btnSend.setEnabled(true);
                btnSend.setText("Send Message");
            }
        });
    }
}