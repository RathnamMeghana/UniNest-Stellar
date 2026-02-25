package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.ApartmentApi;
import com.example.uninest.model.Apartment;
import com.example.uninest.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;


import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Query;

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

        TextView tvBuildingName = findViewById(R.id.tvBuildingName);

        apartmentApi = ApiClient.getApartmentApi();

        // Get data from previous screen
        buildingName = getIntent().getStringExtra("EXTRA_BUILDING_NAME");
        buildingId = getIntent().getStringExtra("EXTRA_BUILDING_ID");

        // Update title
        if (buildingName != null && !buildingName.isEmpty()) {
            tvBuildingName.setText(buildingName);
        } else {
            tvBuildingName.setText("Apartments");
        }

        // Load apartments
        loadApartments();

        // New Apartment button → open AddApartmentActivity
        btnNewApartment.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LettingAgentApartmentsActivity.this,
                    AddApartmentActivity.class
            );
            intent.putExtra("EXTRA_BUILDING_ID", buildingId);
            startActivity(intent);
        });



        Button btnAddMultiple = findViewById(R.id.btnAddMultiple);
        btnAddMultiple.setOnClickListener(v -> {
            Intent intent = new Intent(this, BulkApartmentWithRoomsActivity.class);
            intent.putExtra("EXTRA_BUILDING_ID", buildingId); // pass the building ID
            startActivity(intent);
        });


        // Bottom nav
        setupBottomNav(R.id.nav_buildings);
    }


    private void loadApartments() {
        apartmentList.removeAllViews();

        // Call the NEW filtered endpoint
        apartmentApi.getApartmentsByBuilding(buildingId).enqueue(new Callback<List<Apartment>>() {
            @Override
            public void onResponse(Call<List<Apartment>> call, Response<List<Apartment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Every apartment returned is now guaranteed to belong to this building
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
    // ------------------------
    // Load & filter apartments
    // ------------------------
//    private void loadApartments() {
//        apartmentList.removeAllViews();
//        apartmentList.setVisibility(View.VISIBLE);
//
//
//        apartmentApi.getAllApartments().enqueue(new Callback<List<Apartment>>() {
//            @Override
//            public void onResponse(Call<List<Apartment>> call,
//                                   Response<List<Apartment>> response) {
//
//                if (!response.isSuccessful() || response.body() == null) {
//                    Toast.makeText(LettingAgentApartmentsActivity.this,
//                            "Failed to load apartments", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//
//                List<Apartment> allApartments = response.body();
//                List<Apartment> filtered = new ArrayList<>();
//
//                // Filter by buildingId (since backend does not filter)
//                for (Apartment a : allApartments) {
//                    if (a.getBuildingId() != null &&
//                            a.getBuildingId().equalsIgnoreCase(buildingId)) {
//                        filtered.add(a);
//                    }
//                }
//
//                if (filtered.isEmpty()) {
//                    Toast.makeText(LettingAgentApartmentsActivity.this,
//                            "No apartments found for this building",
//                            Toast.LENGTH_SHORT).show();
//                }
//
//                for (Apartment apartment : filtered) {
//                    addApartmentCard(apartment);
//                }
//            }

//            @Override
//            public void onFailure(Call<List<Apartment>> call, Throwable t) {
//                Log.e("ApartmentAPI", "Error loading apartments", t);
//                Toast.makeText(LettingAgentApartmentsActivity.this,
//                        "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        });
//    }



    // ------------------------
    // Add card to UI
    // ------------------------
    private void addApartmentCard(Apartment apartment) {
        ApartmentCardView card = new ApartmentCardView(this);
        //Log.d("RENDER_DEBUG", "Adding card for: " + apartment.getName() + " Rooms: " + apartment.getTotalRooms());

        // Set apartment details
        card.setApartmentName(apartment.getName());
        String occupied = String.valueOf(apartment.getOccupiedCount());
        String total = apartment.getTotalRooms();

        card.setTenantInfo(occupied, total);

        card.setOnClickListener(v -> openApartmentTenants(apartment));

        apartmentList.addView(card);
    }

    // ------------------------
    // Navigate to tenant list
    // ------------------------
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
        intent.putExtra("EXTRA_TOTAL_ROOMS", apartment.getTotalRooms());


        startActivity(intent);
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
                startActivity(new Intent(this, LettingAgentProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }
}
