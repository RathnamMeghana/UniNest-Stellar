package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.BuildingApi;
import com.example.uninest.model.Building;
import com.example.uninest.utils.AgentBottomNavHelper;
import com.example.uninest.utils.DestructiveConfirmationDialog;
import com.example.uninest.utils.NetworkErrorDialog;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;



public class LettingAgentBuildingsActivity extends AppCompatActivity {

    private LinearLayout buildingList;
    private BuildingApi buildingApi;

    private FirebaseAuth mAuth;
    private boolean hasLoadedBuildings;


    //  Refresh only when AddBuildingActivity returns RESULT_OK
    private final ActivityResultLauncher<Intent> addBuildingLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadBuildingsFromApi(); // refresh immediately
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letting_agent_buildings);

        mAuth = FirebaseAuth.getInstance();


        buildingList = findViewById(R.id.layoutBuildingList);
        Button btnNewBuilding = findViewById(R.id.btnNewBuilding);

        buildingApi = ApiClient.getBuildingApi();


        btnNewBuilding.setOnClickListener(v -> {
            Intent intent = new Intent(LettingAgentBuildingsActivity.this, AddBuildingActivity.class);
            addBuildingLauncher.launch(intent);
        });


        // Bottom nav clicks
        AgentBottomNavHelper.setup(this, R.id.nav_buildings);
        loadBuildingsIfNeeded();


    }

    @Override
    protected void onStart() {
        super.onStart();
        loadBuildingsIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AgentBottomNavHelper.syncSelected(this, R.id.nav_buildings);
    }

    private void loadBuildingsFromApi() {

        String landlordId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (landlordId == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        buildingApi.getBuildingsByLandlord(landlordId).enqueue(new Callback<List<Building>>() {
            @Override
            public void onResponse(Call<List<Building>> call, Response<List<Building>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    NetworkErrorDialog.show(
                            LettingAgentBuildingsActivity.this,
                            "Something went wrong",
                            "Check your connection and try again.",
                            LettingAgentBuildingsActivity.this::loadBuildingsFromApi
                    );
                    return;
                }

                buildingList.removeAllViews();
                hasLoadedBuildings = true;

                List<Building> buildings = response.body();
                if (buildings.isEmpty()) {
                    Toast.makeText(LettingAgentBuildingsActivity.this,
                            "No buildings yet. Tap + New Building", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (Building b : buildings) addBuildingCard(b);
            }

            @Override
            public void onFailure(Call<List<Building>> call, Throwable t) {
                hasLoadedBuildings = false;
                Log.e("Buildings", "Error", t);
                NetworkErrorDialog.show(
                        LettingAgentBuildingsActivity.this,
                        "Something went wrong",
                        "Check your connection and try again.",
                        LettingAgentBuildingsActivity.this::loadBuildingsFromApi
                );
            }
        });
    }

    private void loadBuildingsIfNeeded() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && !hasLoadedBuildings) {
            loadBuildingsFromApi();
        }
    }


    private void addBuildingCard(Building building) {
        BuildingCardView card = new BuildingCardView(this);
        card.setBuildingName(building.getName());
        card.setApartmentCount(building.getApartmentCount());

        // If the building has an image string, use the Base64 loader.
        // Otherwise, fall back to the placeholder logic.
        if (building.getImageUrl() != null && !building.getImageUrl().isEmpty()) {
           // card.setBuildingImageFromBase64(building.getImageUrl());
            card.setBuildingImageFromBase64(building);
        } else {
            // Fallback placeholders based on name
            if (building.getName() != null && building.getName().toLowerCase().contains("green")) {
                card.setBuildingImage(R.drawable.green_park_placeholder);
            } else if (building.getName() != null && building.getName().toLowerCase().contains("mourne")) {
                card.setBuildingImage(R.drawable.mourne_view_placeholder);
            } else {
                card.setBuildingImage(R.drawable.building_placeholder);
            }
        }

        // Tap building card -> open Apartments screen
        card.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LettingAgentBuildingsActivity.this,
                    LettingAgentApartmentsActivity.class
            );
            intent.putExtra("EXTRA_BUILDING_NAME", building.getName());
            intent.putExtra("EXTRA_BUILDING_ID", building.getId());
            startActivity(intent);
        });

        card.setOnDeleteClickListener(v -> showBuildingDeleteConfirmation(building));

        buildingList.addView(card);
    }

    private void showBuildingDeleteConfirmation(Building building) {
        String buildingName = building.getName() != null && !building.getName().trim().isEmpty()
                ? building.getName().trim()
                : "this building";
        String title = "this building".equals(buildingName)
                ? "Delete this building?"
                : "Delete " + buildingName + "?";

        DestructiveConfirmationDialog.show(
                this,
                "Delete building",
                title,
                "This building will be removed from your portfolio.",
                "Every apartment inside it will be deleted too, and this can't be undone.",
                "Delete building",
                () -> {
                    buildingApi.deleteBuilding(building.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(LettingAgentBuildingsActivity.this, "Building deleted", Toast.LENGTH_SHORT).show();
                                loadBuildingsFromApi();
                            } else {
                                Toast.makeText(LettingAgentBuildingsActivity.this, "Failed to delete building", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(LettingAgentBuildingsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
        );
    }

}
