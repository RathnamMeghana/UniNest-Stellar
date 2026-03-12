package com.example.uninest.ui.auth;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.BuildingApi;
import com.example.uninest.model.Building;
import com.example.uninest.model.SendNotificationRequest;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LettingAgentBuildingsActivity extends AppCompatActivity {

    private LinearLayout buildingList;
    private BuildingApi buildingApi;
    private SessionManager sessionManager;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authListener;

    private final ActivityResultLauncher<Intent> addBuildingLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadBuildingsFromApi();
                }
            });

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(
                            this,
                            "Notifications are disabled. You may miss ticket alerts.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letting_agent_buildings);

        sessionManager = new SessionManager(this);
        mAuth = FirebaseAuth.getInstance();

        buildingList = findViewById(R.id.layoutBuildingList);
        Button btnNewBuilding = findViewById(R.id.btnNewBuilding);

        buildingApi = ApiClient.getBuildingApi();

        requestNotificationPermissionIfNeeded();

        btnNewBuilding.setOnClickListener(v -> {
            Intent intent = new Intent(LettingAgentBuildingsActivity.this, AddBuildingActivity.class);
            addBuildingLauncher.launch(intent);
        });

        setupBottomNav(R.id.nav_buildings);
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            loadBuildingsFromApi();
        }

        if (authListener == null) {
            authListener = firebaseAuth -> {
                if (firebaseAuth.getCurrentUser() != null) {
                    loadBuildingsFromApi();
                }
            };
        }
        mAuth.addAuthStateListener(authListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authListener != null) {
            mAuth.removeAuthStateListener(authListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void loadBuildingsFromApi() {
        String landlordId = FirebaseAuth.getInstance().getUid();
        if (landlordId == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        buildingApi.getBuildingsByLandlord(landlordId).enqueue(new Callback<List<Building>>() {
            @Override
            public void onResponse(Call<List<Building>> call, Response<List<Building>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(
                            LettingAgentBuildingsActivity.this,
                            "Failed to load buildings",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                buildingList.removeAllViews();

                List<Building> buildings = response.body();
                if (buildings.isEmpty()) {
                    Toast.makeText(
                            LettingAgentBuildingsActivity.this,
                            "No buildings yet. Tap + New Building",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                for (Building b : buildings) {
                    addBuildingCard(b);
                }
            }

            @Override
            public void onFailure(Call<List<Building>> call, Throwable t) {
                Log.e("Buildings", "Error", t);
                Toast.makeText(
                        LettingAgentBuildingsActivity.this,
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void addBuildingCard(Building building) {
        BuildingCardView card = new BuildingCardView(this);
        card.setBuildingName(building.getName());
        card.setApartmentCount(building.getApartmentCount());

        if (building.getImageUrl() != null && !building.getImageUrl().isEmpty()) {
            card.setBuildingImageFromBase64(building.getImageUrl());
        } else {
            if (building.getName() != null && building.getName().toLowerCase().contains("green")) {
                card.setBuildingImage(R.drawable.green_park_placeholder);
            } else if (building.getName() != null && building.getName().toLowerCase().contains("mourne")) {
                card.setBuildingImage(R.drawable.mourne_view_placeholder);
            } else {
                card.setBuildingImage(R.drawable.building_placeholder);
            }
        }

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
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Building?")
                .setMessage("This will also delete ALL apartments inside this building. This action cannot be undone.")
                .setPositiveButton("Delete", (d, which) -> {
                    buildingApi.deleteBuilding(building.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(
                                        LettingAgentBuildingsActivity.this,
                                        "Building deleted",
                                        Toast.LENGTH_SHORT
                                ).show();
                                loadBuildingsFromApi();
                            } else {
                                Toast.makeText(
                                        LettingAgentBuildingsActivity.this,
                                        "Failed to delete building",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(
                                    LettingAgentBuildingsActivity.this,
                                    "Error: " + t.getMessage(),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.RED);
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