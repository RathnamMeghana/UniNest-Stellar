package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.example.uninest.data.api.BuildingApi;
import com.example.uninest.model.Building;
import com.google.firebase.auth.FirebaseAuth;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.ApartmentApi;
import com.example.uninest.model.Apartment;
import com.example.uninest.model.SendNotificationRequest;
import com.example.uninest.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LettingAgentNotificationsActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    private Chip chipEveryone;
    private Chip chipPickTenants;
    private Spinner spinnerApartments;
    private Spinner spinnerTenants;
    private EditText etTitle;
    private EditText etBody;
    private MaterialButton btnSend;
    private TextView tvSelectedApartment;
    private TextView tvMessageCount;

    private final List<Apartment> apartmentList = new ArrayList<>();
    private final List<String> apartmentNames = new ArrayList<>();

    private final List<User> tenantUsers = new ArrayList<>();
    private final List<String> tenantNames = new ArrayList<>();

    private String apartmentName;
    private String houseCode;
    private String selectedBuildingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letting_agent_notifications);

        sessionManager = new SessionManager(this);

        chipEveryone = findViewById(R.id.chipEveryone);
        chipPickTenants = findViewById(R.id.chipPickTenants);
        spinnerApartments = findViewById(R.id.spinnerApartments);
        spinnerTenants = findViewById(R.id.spinnerTenants);
        etTitle = findViewById(R.id.etNotificationTitle);
        etBody = findViewById(R.id.etNotificationBody);
        btnSend = findViewById(R.id.btnSendNotification);
        tvMessageCount = findViewById(R.id.tvMessageCount);
        tvSelectedApartment = findViewById(R.id.tvSelectedApartment);

        houseCode = getIntent().getStringExtra("EXTRA_HOUSE_CODE");
        apartmentName = getIntent().getStringExtra("EXTRA_APARTMENT_NAME");
        selectedBuildingId = getIntent().getStringExtra("EXTRA_BUILDING_ID");

        updateApartmentLabel();

        chipEveryone.setChecked(true);
        spinnerTenants.setVisibility(View.GONE);

        chipEveryone.setOnClickListener(v -> {
            chipEveryone.setChecked(true);
            chipPickTenants.setChecked(false);
            spinnerTenants.setVisibility(View.GONE);
        });

        chipPickTenants.setOnClickListener(v -> {
            chipPickTenants.setChecked(true);
            chipEveryone.setChecked(false);
            spinnerTenants.setVisibility(View.VISIBLE);
        });

        spinnerApartments.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < apartmentList.size()) {
                    Apartment selectedApartment = apartmentList.get(position);
                    houseCode = selectedApartment.getCode();
                    apartmentName = selectedApartment.getName();
                    updateApartmentLabel();
                    loadTenants();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        etBody.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                tvMessageCount.setText(length + "/200");
                if (length >= 180) {
                    tvMessageCount.setTextColor(getColor(android.R.color.holo_red_dark));
                } else {
                    tvMessageCount.setTextColor(getColor(android.R.color.darker_gray));
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        loadApartments();
        setupBottomNav(R.id.nav_notifications);

        btnSend.setOnClickListener(v -> sendNotification());
    }

    private void updateApartmentLabel() {
        if (apartmentName == null || apartmentName.isEmpty()) {
            tvSelectedApartment.setText("Apartment: Not selected");
        } else if (houseCode == null || houseCode.isEmpty()) {
            tvSelectedApartment.setText("Apartment: " + apartmentName);
        } else {
            tvSelectedApartment.setText("Apartment: " + apartmentName + " (" + houseCode + ")");
        }
    }

    private void loadApartments() {
        ApartmentApi apartmentApi = ApiClient.getApartmentApi();
        BuildingApi buildingApi = ApiClient.getBuildingApi();

        String landlordId = FirebaseAuth.getInstance().getUid();
        if (landlordId == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // If notifications page was opened from a specific building/apartment,
        // keep that filtering.
        if (selectedBuildingId != null && !selectedBuildingId.isEmpty()) {
            apartmentApi.getApartmentsByBuilding(selectedBuildingId).enqueue(new Callback<List<Apartment>>() {
                @Override
                public void onResponse(Call<List<Apartment>> call, Response<List<Apartment>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        bindApartments(response.body());
                    } else {
                        Toast.makeText(LettingAgentNotificationsActivity.this,
                                "Failed to load apartments", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<List<Apartment>> call, Throwable t) {
                    Toast.makeText(LettingAgentNotificationsActivity.this,
                            "Failed to load apartments", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        // Otherwise load only buildings owned by this agent,
        // then load apartments inside those buildings.
        buildingApi.getBuildingsByLandlord(landlordId).enqueue(new Callback<List<Building>>() {
            @Override
            public void onResponse(Call<List<Building>> call, Response<List<Building>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(LettingAgentNotificationsActivity.this,
                            "Failed to load buildings", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<Building> buildings = response.body();
                if (buildings.isEmpty()) {
                    bindApartments(new ArrayList<>());
                    return;
                }

                List<Apartment> allAgentApartments = new ArrayList<>();
                final int[] remaining = {buildings.size()};

                for (Building building : buildings) {
                    apartmentApi.getApartmentsByBuilding(building.getId()).enqueue(new Callback<List<Apartment>>() {
                        @Override
                        public void onResponse(Call<List<Apartment>> call, Response<List<Apartment>> apartmentResponse) {
                            if (apartmentResponse.isSuccessful() && apartmentResponse.body() != null) {
                                allAgentApartments.addAll(apartmentResponse.body());
                            }

                            remaining[0]--;
                            if (remaining[0] == 0) {
                                bindApartments(allAgentApartments);
                            }
                        }

                        @Override
                        public void onFailure(Call<List<Apartment>> call, Throwable t) {
                            remaining[0]--;
                            if (remaining[0] == 0) {
                                bindApartments(allAgentApartments);
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<List<Building>> call, Throwable t) {
                Toast.makeText(LettingAgentNotificationsActivity.this,
                        "Failed to load buildings", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindApartments(List<Apartment> apartments) {
        apartmentList.clear();
        apartmentNames.clear();

        apartmentList.addAll(apartments);

        for (Apartment apartment : apartments) {
            apartmentNames.add(apartment.getName() + " (" + apartment.getCode() + ")");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                apartmentNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerApartments.setAdapter(adapter);

        if (houseCode != null && !houseCode.isEmpty()) {
            for (int i = 0; i < apartmentList.size(); i++) {
                if (houseCode.equals(apartmentList.get(i).getCode())) {
                    spinnerApartments.setSelection(i);
                    break;
                }
            }
        } else if (!apartmentList.isEmpty()) {
            Apartment firstApartment = apartmentList.get(0);
            houseCode = firstApartment.getCode();
            apartmentName = firstApartment.getName();
            updateApartmentLabel();
            loadTenants();
        }
    }

    private void loadTenants() {
        if (houseCode == null || houseCode.isEmpty()) return;

        ApiClient.getUserApi().getRoommates(houseCode).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                tenantUsers.clear();
                tenantNames.clear();

                if (response.isSuccessful() && response.body() != null) {
                    for (User user : response.body()) {
                        if ("2".equals(user.getRole()) || "TENANT".equalsIgnoreCase(user.getRole())) {
                            tenantUsers.add(user);
                            tenantNames.add(user.getFullName());
                        }
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        LettingAgentNotificationsActivity.this,
                        android.R.layout.simple_spinner_item,
                        tenantNames
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerTenants.setAdapter(adapter);
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(LettingAgentNotificationsActivity.this,
                        "Failed to load tenants", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendNotification() {
        String title = etTitle.getText().toString().trim();
        String body = etBody.getText().toString().trim();

        if (houseCode == null || houseCode.isEmpty()) {
            Toast.makeText(this, "Please select an apartment", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty() || body.isEmpty()) {
            Toast.makeText(this, "Enter both title and message", Toast.LENGTH_SHORT).show();
            return;
        }

        SendNotificationRequest request;

        if (chipEveryone.isChecked()) {
            request = new SendNotificationRequest(houseCode, null, title, body);
        } else {
            if (tenantUsers.isEmpty() || spinnerTenants.getSelectedItemPosition() < 0) {
                Toast.makeText(this, "Please pick a tenant", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedTenantId = tenantUsers.get(spinnerTenants.getSelectedItemPosition()).getId();
            request = new SendNotificationRequest(
                    houseCode,
                    Arrays.asList(selectedTenantId),
                    title,
                    body
            );
        }

        btnSend.setEnabled(false);
        btnSend.setText("Sending...");

        ApiClient.getNotificationApi().sendNotification(request).enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(Call<Integer> call, Response<Integer> response) {
                btnSend.setEnabled(true);
                btnSend.setText("Send Message");

                if (response.isSuccessful()) {
                    etTitle.setText("");
                    etBody.setText("");
                    tvMessageCount.setText("0/200");

                    Snackbar.make(findViewById(android.R.id.content),
                            "Message sent! Success count: " + response.body(),
                            Snackbar.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LettingAgentNotificationsActivity.this,
                            "Failed to send message",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Integer> call, Throwable t) {
                btnSend.setEnabled(true);
                btnSend.setText("Send Message");

                Toast.makeText(LettingAgentNotificationsActivity.this,
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupBottomNav(int selectedId) {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(selectedId);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == selectedId) return true;

            if (itemId == R.id.nav_tickets) {
                startActivity(new Intent(this, LettingAgentTicketsActivity.class));
            } else if (itemId == R.id.nav_buildings) {
                startActivity(new Intent(this, LettingAgentBuildingsActivity.class));
            } else if (itemId == R.id.nav_notifications) {
                startActivity(new Intent(this, LettingAgentNotificationsActivity.class));
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, LettingAgentProfileActivity.class));
            } else {
                return false;
            }

            overridePendingTransition(0, 0);
            finish();
            return true;
        });
    }
}