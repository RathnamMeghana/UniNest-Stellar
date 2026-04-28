package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.data.api.ApartmentApi;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.ApartmentRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddApartmentActivity extends AppCompatActivity {

    private EditText etApartmentName;
    private EditText etMaxTenants; // Renamed from etTotalRooms
    private ApartmentApi apartmentApi;
    private Button btnSave;

    private String buildingId;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_apartment);

        // Link to UI
        etApartmentName = findViewById(R.id.etApartmentName);
        // Note: Ensure your XML ID is etMaxTenants or update this to match your XML
        etMaxTenants = findViewById(R.id.etMaxTenants);

        Button btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSaveApartment);

        buildingId = getIntent().getStringExtra("EXTRA_BUILDING_ID");
        if (buildingId == null) {
            Toast.makeText(this, "Error: Building ID not provided.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mAuth = FirebaseAuth.getInstance();
        apartmentApi = ApiClient.getApartmentApi();

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveApartment());
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }
    }

    private void saveApartment() {
        String name = etApartmentName.getText().toString().trim();
        String maxTenantsStr = etMaxTenants.getText().toString().trim();

        // 1. Validation
        if (name.isEmpty()) {
            etApartmentName.setError("Please enter an apartment name");
            return;
        }

        if (maxTenantsStr.isEmpty()) {
            etMaxTenants.setError("Please enter maximum number of tenants");
            return;
        }

        int maxTenants;
        try {
            maxTenants = Integer.parseInt(maxTenantsStr);
        } catch (NumberFormatException e) {
            etMaxTenants.setError("Please enter a valid number");
            return;
        }

        // 2. Auth Check
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Error: Landlord is not logged in.", Toast.LENGTH_LONG).show();
            return;
        }

        // 3. Build Request
        ApartmentRequest request = new ApartmentRequest();
        request.setBuildingId(buildingId);
        request.setName(name);
        request.setLandlordId(user.getUid());

        // IMPORTANT: We set both to the same value so your Card UI works
        request.setMaxTenants(maxTenants);
        request.setTotalRooms(maxTenantsStr);

        request.setDescription("Single Unit Apartment");
        request.setRentPrice(0.0); // Default or add a field for this
        request.setActive(true);

        btnSave.setEnabled(false);

        apartmentApi.createApartment(request).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                btnSave.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(AddApartmentActivity.this, "Apartment created!", Toast.LENGTH_SHORT).show();

                    // Return to list
                    //Intent intent = new Intent(AddApartmentActivity.this, LettingAgentApartmentsActivity.class);
                    //intent.putExtra("EXTRA_BUILDING_ID", buildingId);
                    //startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(AddApartmentActivity.this, "Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                btnSave.setEnabled(true);
                Toast.makeText(AddApartmentActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}