package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.uninest.model.NameUpdateRequest;
import com.example.uninest.SessionManager;
import com.example.uninest.utils.AgentBottomNavHelper;
import com.example.uninest.utils.DestructiveConfirmationDialog;
import com.example.uninest.utils.NameEditDialog;
import com.example.uninest.utils.NetworkErrorDialog;

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

        AgentBottomNavHelper.setup(this, R.id.nav_buildings);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AgentBottomNavHelper.syncSelected(this, R.id.nav_buildings);
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
                    NetworkErrorDialog.show(
                            LettingAgentApartmentsActivity.this,
                            "Something went wrong",
                            "Check your connection and try again.",
                            LettingAgentApartmentsActivity.this::loadApartments
                    );
                }
            }

            @Override
            public void onFailure(Call<List<Apartment>> call, Throwable t) {
                Log.e("ApartmentAPI", "Error: " + t.getMessage());
                NetworkErrorDialog.show(
                        LettingAgentApartmentsActivity.this,
                        "Something went wrong",
                        "Check your connection and try again.",
                        LettingAgentApartmentsActivity.this::loadApartments
                );
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
        card.setOnEditClickListener(v -> showApartmentRenameDialog(apartment));
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
        String apartmentName = apartment.getName() != null && !apartment.getName().trim().isEmpty()
                ? apartment.getName().trim()
                : "this apartment";
        String title = "this apartment".equals(apartmentName)
                ? "Delete this apartment?"
                : "Delete " + apartmentName + "?";
        String message = buildingName != null && !buildingName.trim().isEmpty()
                ? "This apartment will be removed from " + buildingName.trim() + "."
                : "This apartment will be removed from the building.";

        DestructiveConfirmationDialog.show(
                this,
                "Delete apartment",
                title,
                message,
                "Any assigned tenants will be unassigned, and this can't be undone.",
                "Delete apartment",
                () -> {
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
                }
        );
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

    private void showApartmentRenameDialog(Apartment apartment) {
        String currentName = apartment.getName() != null ? apartment.getName().trim() : "";
        NameEditDialog.show(
                this,
                "Update apartment",
                "Rename apartment",
                null,
                currentName,
                "Enter apartment name",
                updatedName -> renameApartment(apartment, updatedName)
        );
    }

    private void renameApartment(Apartment apartment, String updatedName) {
        NameUpdateRequest request = new NameUpdateRequest();
        request.setName(updatedName);

        apartmentApi.updateApartmentName(apartment.getCode(), request).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(LettingAgentApartmentsActivity.this, "Apartment updated", Toast.LENGTH_SHORT).show();
                    loadApartments();
                } else {
                    Toast.makeText(LettingAgentApartmentsActivity.this, "Failed to update apartment", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(LettingAgentApartmentsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
