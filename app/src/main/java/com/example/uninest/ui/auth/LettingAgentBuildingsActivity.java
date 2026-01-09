package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.BuildingApi;
import com.example.uninest.model.Building;

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
    private FirebaseAuth.AuthStateListener authListener;


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
        findViewById(R.id.navTickets).setOnClickListener(v -> {
            Intent intent = new Intent(LettingAgentBuildingsActivity.this, LettingAgentTicketsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.navApartments).setOnClickListener(v -> {
            // current screen – maybe scroll to top
            buildingList.scrollTo(0, 0);
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            // TODO navigate to ProfileActivity
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // This listener handles the initial load when the user logs in or app starts
        if (authListener == null) {
            authListener = firebaseAuth -> {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    loadBuildingsFromApi();
                }
            };
        }
        mAuth.addAuthStateListener(authListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authListener != null) mAuth.removeAuthStateListener(authListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
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
                    Toast.makeText(LettingAgentBuildingsActivity.this,
                            "Failed to load buildings", Toast.LENGTH_SHORT).show();
                    return;
                }

                buildingList.removeAllViews();

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
                Log.e("Buildings", "Error", t);
                Toast.makeText(LettingAgentBuildingsActivity.this,
                        "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void addBuildingCard(Building building) {
        BuildingCardView card = new BuildingCardView(this);
        card.setBuildingName(building.getName());
        card.setApartmentCount(0); // you'll hook real count later

        // Simple placeholder logic so UI works
        if (building.getName() != null &&
                building.getName().toLowerCase().contains("green")) {
            card.setBuildingImage(R.drawable.green_park_placeholder);
        } else if (building.getName() != null &&
                building.getName().toLowerCase().contains("mourne")) {
            card.setBuildingImage(R.drawable.mourne_view_placeholder);
        } else {
            card.setBuildingImage(R.drawable.building_placeholder); // if you have one
        }
        //  Tap building card -> open Apartments screen with that building name
        card.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LettingAgentBuildingsActivity.this,
                    LettingAgentApartmentsActivity.class
            );
            intent.putExtra("EXTRA_BUILDING_NAME", building.getName());
            intent.putExtra("EXTRA_BUILDING_ID", building.getId());

            startActivity(intent);
        });
        buildingList.addView(card);
    }
}
