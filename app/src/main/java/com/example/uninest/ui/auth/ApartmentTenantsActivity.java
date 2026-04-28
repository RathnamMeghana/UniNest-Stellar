package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.data.api.ApartmentApi;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.Room;
import com.example.uninest.model.User;
import com.example.uninest.utils.AgentBottomNavHelper;
import com.example.uninest.utils.DestructiveConfirmationDialog;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

public class ApartmentTenantsActivity extends AppCompatActivity {

    private LinearLayout tenantList;
    private LinearLayout roomsList;
    private TextView tvBuildingName;
    private TextView tvApartmentName;
    private TextView tvTenantCount;
    private ApartmentApi apartmentApi;
    private String apartmentId;
    private String userRole;

    // Hard-coded house code
    private String houseCode;

    private TextView tvHouseCode;
    private View btnShareCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apartment_tenants);

        tenantList = findViewById(R.id.layoutTenantList);
        roomsList = findViewById(R.id.layoutRoomsList);
        tvBuildingName = findViewById(R.id.tvBuildingName);
        tvApartmentName = findViewById(R.id.tvApartmentName);
        tvTenantCount = findViewById(R.id.tvTenantCount);

        tvHouseCode = findViewById(R.id.tvHouseCode);
        btnShareCode = findViewById(R.id.btnShareCode);

        apartmentApi = ApiClient.getApartmentApi();

        // Get extras from previous screen
        String buildingName = getIntent().getStringExtra("EXTRA_BUILDING_NAME");
        String apartmentName = getIntent().getStringExtra("EXTRA_APARTMENT_NAME");
        houseCode = getIntent().getStringExtra("EXTRA_HOUSE_CODE");
        apartmentId = getIntent().getStringExtra("EXTRA_APARTMENT_ID");
        userRole = getIntent().getStringExtra("EXTRA_USER_ROLE");

        // Set building/apartment names
        tvBuildingName.setText(buildingName != null ? buildingName : "Apartments");
        tvApartmentName.setText(apartmentName != null ? apartmentName : "Apartment");

        // Fetch tenants and rooms if apartmentId is available
        if (apartmentId != null && !apartmentId.isEmpty()) {
            fetchTenants(apartmentId);

            // ONLY fetch rooms if houseCode is also present
            if (houseCode != null && !houseCode.isEmpty()) {
                fetchRooms(houseCode);
            } else {
                Log.e("TENANTS_ACTIVITY", "House Code is missing. Rooms will not load.");

            }
        } else {
            Log.e("TENANTS_ACTIVITY", "Apartment ID is missing.");
            tvTenantCount.setText("Error: ID Missing");
        }

        // Setup "New Room" button
        View btnNewRoom = findViewById(R.id.btnNewRoom);
        if ("1".equals(userRole)) { // letting agent
            btnNewRoom.setVisibility(View.VISIBLE);
            btnNewRoom.setOnClickListener(v -> {
                Intent intent = new Intent(
                        ApartmentTenantsActivity.this,
                        SetupApartmentRoomsActivity.class
                );

                intent.putExtra("EXTRA_HOUSE_CODE", houseCode);
                intent.putExtra("EXTRA_USER_ROLE", userRole);

                startActivityForResult(intent, 101);
            });
        } else { // tenant
            btnNewRoom.setVisibility(View.GONE);
        }

        setupHouseCodeDisplay();

        AgentBottomNavHelper.setup(this, R.id.nav_buildings);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            fetchRooms(houseCode); // This re-fetches and updates the counts dynamically
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AgentBottomNavHelper.syncSelected(this, R.id.nav_buildings);
    }

    private void setupHouseCodeDisplay() {
        if (houseCode != null && !houseCode.isEmpty()) {
            tvHouseCode.setText(houseCode);

            // 1. Click text to Copy to Clipboard
            tvHouseCode.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("House Code", houseCode);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Code copied to clipboard", Toast.LENGTH_SHORT).show();
            });

            // 2. Click button to Share via WhatsApp/SMS/Email
            btnShareCode.setOnClickListener(v -> {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");

                String shareBody = "Download the UniNest app and join my apartment using this code: " + houseCode;

                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Join my Apartment on UniNest");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);

                startActivity(Intent.createChooser(shareIntent, "Share Code via"));
            });

        } else {
            tvHouseCode.setText("N/A");
            btnShareCode.setVisibility(View.GONE);
        }
    }

    // Fetch tenants
    private void fetchTenants(String id) {
        apartmentApi.getUsersForApartment(id).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayTenants(response.body());
                } else {
                    Log.e("API_CALL", "Failed to fetch tenants: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Log.e("API_CALL", "Network error fetching tenants", t);
            }
        });
    }

    // Display tenants
    private void displayTenants(List<User> tenants) {
        tenantList.removeAllViews();

        int occupied = tenants.size();
        String totalRoomsStr = getIntent().getStringExtra("EXTRA_TOTAL_ROOMS");
        int capacity = (totalRoomsStr != null) ? Integer.parseInt(totalRoomsStr) : 0;
        tvTenantCount.setText(occupied + "/" + capacity + " Tenants");

        for (User tenant : tenants) {
            TenantCardView card = new TenantCardView(this);
            card.setTenantName(tenant.getFullName());
            card.setRoomLabel("Room");

            com.example.uninest.utils.ImageUtils.loadProfileImage(
                    card.getAvatarImageView(),
                    tenant.getProfileImageUrl()
            );


            boolean canDelete = "1".equals(userRole);
            card.showDeleteButton(canDelete);

            if (canDelete) {
                card.setOnDeleteClickListener(v -> {
                    String tenantName = tenant.getFullName() != null && !tenant.getFullName().trim().isEmpty()
                            ? tenant.getFullName().trim()
                            : "this tenant";
                    String title = "this tenant".equals(tenantName)
                            ? "Remove this tenant?"
                            : "Remove " + tenantName + "?";

                    DestructiveConfirmationDialog.show(
                            this,
                            "Remove tenant",
                            title,
                            "They will lose access to this apartment right away.",
                            "They'll need a new invite or house code to join again.",
                            "Remove tenant",
                            () -> removeTenant(tenant.getEmail())
                    );
                });
            }

            tenantList.addView(card);
        }
    }

    // Fetch rooms
    private void fetchRooms(String houseCode) {
        apartmentApi.getRooms(houseCode).enqueue(new Callback<List<Room>>() {
            @Override
            public void onResponse(Call<List<Room>> call, Response<List<Room>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayRoomTypes(response.body());
                } else {
                    Log.e("ROOMS", "Failed to fetch rooms: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Room>> call, Throwable t) {
                Log.e("ROOMS", "Network error fetching rooms", t);
            }
        });
    }

    // Display unique room types
    private void displayRoomTypes(List<Room> rooms) {
        roomsList.removeAllViews();
        if (rooms == null || rooms.isEmpty()) return;

        // Group by type and count
        java.util.Map<String, Integer> roomCounts = new java.util.HashMap<>();
        for (Room room : rooms) {
            String type = room.getType();
            if (type != null) {
                String key = type.trim();
                roomCounts.put(key, roomCounts.getOrDefault(key, 0) + 1);
            }
        }

        for (java.util.Map.Entry<String, Integer> entry : roomCounts.entrySet()) {
            RoomTypeCardView card = new RoomTypeCardView(this);

            card.setRoomData(entry.getKey(), entry.getValue());

            roomsList.addView(card);
        }
    }

    // Open RoomTypeDetailActivity for selected type
    private void openRoomDetail(String roomType) {
        Intent intent = new Intent(this, RoomTypeDetailActivity.class);
        intent.putExtra("EXTRA_ROOM_TYPE_NAME", roomType);
        intent.putExtra("EXTRA_APARTMENT_NAME", tvApartmentName.getText().toString());
        intent.putExtra("EXTRA_HOUSE_CODE", houseCode);
        intent.putExtra("EXTRA_USER_ROLE", userRole);
        startActivity(intent);
    }

    // Remove tenant
    private void removeTenant(String email) {
        apartmentApi.removeTenant(email).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ApartmentTenantsActivity.this, "Tenant removed", Toast.LENGTH_SHORT).show();
                    if (apartmentId != null) fetchTenants(apartmentId);
                } else {
                    Toast.makeText(ApartmentTenantsActivity.this, "Failed to remove tenant", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("API_CALL", "Failed to remove tenant", t);
                com.example.uninest.utils.NetworkErrorDialog.show(
                        ApartmentTenantsActivity.this,
                        () -> removeTenant(email)
                );
            }
        });
    }

}
