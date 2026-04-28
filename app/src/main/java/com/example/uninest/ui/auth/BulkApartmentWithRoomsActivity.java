package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.data.api.ApartmentApi;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.BulkApartmentRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BulkApartmentWithRoomsActivity extends AppCompatActivity {

    private EditText etApartmentCount;
    private EditText etMaxTenants; // NEW: max tenants per apartment
    private LinearLayout layoutRoomCounters;
    private ApartmentApi api;
    private String buildingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bulk_apartments_with_rooms);

        etApartmentCount = findViewById(R.id.etApartmentCount);
        etMaxTenants = findViewById(R.id.etMaxTenants);
        layoutRoomCounters = findViewById(R.id.layoutRoomCounters);

        api = ApiClient.getApartmentApi();

        buildingId = getIntent().getStringExtra("EXTRA_BUILDING_ID");
        if (buildingId == null) {
            Toast.makeText(this, "Error: Building ID not provided", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupRoomRows();

        findViewById(R.id.btnCreateAll).setOnClickListener(v -> submitBulk());
    }

    /**
     * Sets up the room counter rows dynamically
     */
    private void setupRoomRows() {
        String[] roomTypes = {"Bedroom", "Bathroom", "Kitchen", "Living Room", "Laundry"};

        for (int i = 0; i < layoutRoomCounters.getChildCount(); i++) {
            View row = layoutRoomCounters.getChildAt(i);

            TextView tvLabel = row.findViewById(R.id.tvRoomLabel);
            TextView tvCount = row.findViewById(R.id.tvRoomCount);
            TextView btnMinus = row.findViewById(R.id.btnMinus);
            TextView btnPlus = row.findViewById(R.id.btnPlus);

            // Assign room type
            tvLabel.setText(roomTypes[i]);
            tvCount.setText("0");

            // Increment
            btnPlus.setOnClickListener(v -> {
                int count = Integer.parseInt(tvCount.getText().toString());
                tvCount.setText(String.valueOf(count + 1));
            });

            // Decrement
            btnMinus.setOnClickListener(v -> {
                int count = Integer.parseInt(tvCount.getText().toString());
                if (count > 0) tvCount.setText(String.valueOf(count - 1));
            });
        }
    }

    /**
     * Collects apartment count, room template, and max tenants, then calls backend
     */
    private void submitBulk() {
        // ----- 1. Validate Apartment Count -----
        String countText = etApartmentCount.getText().toString().trim();
        if (countText.isEmpty()) {
            etApartmentCount.setError("Enter number of apartments");
            return;
        }

        int apartmentCount;
        try {
            apartmentCount = Integer.parseInt(countText);
            if (apartmentCount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            etApartmentCount.setError("Enter a valid number > 0");
            return;
        }

        // ----- 2. Validate Max Tenants -----
        String maxTenantsText = etMaxTenants.getText().toString().trim();
        if (maxTenantsText.isEmpty()) {
            etMaxTenants.setError("Enter maximum tenants per apartment");
            return;
        }

        int maxTenants;
        try {
            maxTenants = Integer.parseInt(maxTenantsText);
            if (maxTenants <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            etMaxTenants.setError("Enter a valid number > 0");
            return;
        }

        // ----- 3. Collect Room Template -----
        Map<String, Integer> roomTemplate = new HashMap<>();
        for (int i = 0; i < layoutRoomCounters.getChildCount(); i++) {
            View row = layoutRoomCounters.getChildAt(i);
            String type = ((TextView) row.findViewById(R.id.tvRoomLabel)).getText().toString();
            int count = Integer.parseInt(((TextView) row.findViewById(R.id.tvRoomCount)).getText().toString());
            roomTemplate.put(type, count);
        }

        // ----- 4. Build Request Object -----
        BulkApartmentRequest request = new BulkApartmentRequest();
        request.setBuildingId(buildingId);
        request.setApartmentCount(apartmentCount);   // Total apartments to create
        request.setMaxTenants(maxTenants);           // Max tenants per apartment
        request.setRoomTemplate(roomTemplate);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            request.setLandlordId(user.getUid());
        } else {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_LONG).show();
            return;
        }

        // ----- 5. Disable button while API call -----
        findViewById(R.id.btnCreateAll).setEnabled(false);

        // ----- 6. Make Backend Call -----
        api.createApartmentsWithRooms(request).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                findViewById(R.id.btnCreateAll).setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(BulkApartmentWithRoomsActivity.this,
                            "Apartments created successfully!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(BulkApartmentWithRoomsActivity.this,
                            "Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                findViewById(R.id.btnCreateAll).setEnabled(true);
                Toast.makeText(BulkApartmentWithRoomsActivity.this,
                        "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
