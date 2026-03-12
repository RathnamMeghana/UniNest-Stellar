package com.example.uninest.ui.auth;

import android.content.Intent;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.ApartmentApi;
import com.example.uninest.model.Apartment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.uninest.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LettingAgentApartmentsActivity extends AppCompatActivity {

    private LinearLayout apartmentList;
    private ApartmentApi apartmentApi;

    private String buildingName;
    private String buildingId;
    private String loggedInRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letting_agent_apartments);

        SessionManager session = new SessionManager(this);
        loggedInRole = session.getUserRole();

        apartmentList = findViewById(R.id.layoutApartmentList);
        Button btnNewApartment = findViewById(R.id.btnNewApartment);
        Button btnAddMultiple = findViewById(R.id.btnAddMultiple);
        TextView tvBuildingName = findViewById(R.id.tvBuildingName);

        apartmentApi = ApiClient.getApartmentApi();

        buildingName = getIntent().getStringExtra("EXTRA_BUILDING_NAME");
        buildingId = getIntent().getStringExtra("EXTRA_BUILDING_ID");

        if (buildingName != null && !buildingName.isEmpty()) {
            tvBuildingName.setText(buildingName);
        } else {
            tvBuildingName.setText("Apartments");
        }

        btnNewApartment.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LettingAgentApartmentsActivity.this,
                    AddApartmentActivity.class
            );
            intent.putExtra("EXTRA_BUILDING_ID", buildingId);
            startActivity(intent);
        });

        btnAddMultiple.setOnClickListener(v -> {
            Intent intent = new Intent(this, BulkApartmentWithRoomsActivity.class);
            intent.putExtra("EXTRA_BUILDING_ID", buildingId);
            startActivity(intent);
        });

        setupBottomNav(R.id.nav_buildings);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApartments();
    }

    private void loadApartments() {
        apartmentList.removeAllViews();

        apartmentApi.getApartmentsByBuilding(buildingId).enqueue(new Callback<List<Apartment>>() {
            @Override
            public void onResponse(Call<List<Apartment>> call, Response<List<Apartment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Apartment apartment : response.body()) {
                        addApartmentCard(apartment);
                    }
                } else {
                    Toast.makeText(LettingAgentApartmentsActivity.this, "Failed to load", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Apartment>> call, Throwable t) {
                Log.e("ApartmentAPI", "Error: " + t.getMessage());
            }
        });
    }

    private void addApartmentCard(Apartment apartment) {
        ApartmentCardView card = new ApartmentCardView(this);

        card.setApartmentName(apartment.getName());

        String occupied = String.valueOf(apartment.getOccupiedCount());
        String capacity = String.valueOf(apartment.getMaxTenants());
        card.setTenantInfo(occupied, capacity);

        card.setOnClickListener(v -> openApartmentTenants(apartment));
        card.setOnDeleteClickListener(v -> showApartmentDeleteConfirmation(apartment));

        card.setOnNotifyClickListener(v -> openApartmentNotifications(apartment));

        apartmentList.addView(card);
    }

    private void openApartmentNotifications(Apartment apartment) {
        Intent intent = new Intent(
                LettingAgentApartmentsActivity.this,
                LettingAgentNotificationsActivity.class
        );

        intent.putExtra("EXTRA_HOUSE_CODE", apartment.getCode());
        intent.putExtra("EXTRA_APARTMENT_NAME", apartment.getName());
        intent.putExtra("EXTRA_BUILDING_ID", buildingId);
        startActivity(intent);
    }

    private void showApartmentDeleteConfirmation(Apartment apartment) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Apartment?")
                .setMessage("This will delete the apartment and unassign any tenants. This action cannot be undone.")
                .setPositiveButton("Delete", (d, which) -> {
                    apartmentApi.deleteApartment(apartment.getCode()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(LettingAgentApartmentsActivity.this, "Apartment deleted", Toast.LENGTH_SHORT).show();
                                loadApartments();
                            } else {
                                Toast.makeText(LettingAgentApartmentsActivity.this, "Failed to delete apartment", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(LettingAgentApartmentsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.RED);
    }

    private void openApartmentTenants(Apartment apartment) {
        Intent intent = new Intent(
                LettingAgentApartmentsActivity.this,
                ApartmentTenantsActivity.class
        );

        intent.putExtra("EXTRA_BUILDING_NAME", buildingName);
        intent.putExtra("EXTRA_APARTMENT_NAME", apartment.getName());
        intent.putExtra("EXTRA_APARTMENT_ID", apartment.getCode());
        intent.putExtra("EXTRA_USER_ROLE", loggedInRole);
        intent.putExtra("EXTRA_HOUSE_CODE", apartment.getCode());
        intent.putExtra("EXTRA_MAX_TENANTS", apartment.getMaxTenants());
        intent.putExtra("EXTRA_TOTAL_ROOMS", apartment.getTotalRooms());

        startActivity(intent);
    }

    private void setupBottomNav(int selectedId) {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(selectedId);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

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
            } else if (itemId == R.id.nav_notifications) {
                startActivity(new Intent(this, LettingAgentNotificationsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;}
                else if (itemId == R.id.nav_notifications) {
                    startActivity(new Intent(this, LettingAgentNotificationsActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, LettingAgentProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }
}
