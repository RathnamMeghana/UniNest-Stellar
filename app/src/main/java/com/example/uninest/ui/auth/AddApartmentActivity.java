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
import com.example.uninest.model.BuildingRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.sql.Timestamp;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddApartmentActivity extends AppCompatActivity {

    private EditText etApartmentName;
    private EditText etTotalRooms;
    private ApartmentApi apartmentApi;
    private Button btnSave;

    private String buildingId; // Store Building ID passed via Intent
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_apartment);

        etApartmentName = findViewById(R.id.etApartmentName);
        etTotalRooms = findViewById(R.id.etTotalRooms);
        Button btnCancel = findViewById(R.id.btnCancel);


        btnSave = findViewById(R.id.btnSaveApartment);

        // Retrieve Building ID from Intent
        buildingId = getIntent().getStringExtra("EXTRA_BUILDING_ID");
        if (buildingId == null) {
            Toast.makeText(this, "Error: Building ID not provided.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Retrofit API
        apartmentApi = ApiClient.getApartmentApi();

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveApartment());
        } else {
            Toast.makeText(this, "Error: Save button resource not found in layout.", Toast.LENGTH_LONG).show();
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }
    }

    private void saveApartment() {
        String name = etApartmentName.getText().toString().trim();
        String totalRoomsText = etTotalRooms.getText().toString().trim();

        //  Input Validation
        if (name.isEmpty()) {
            etApartmentName.setError("Please enter an apartment name");
            return;
        }

        if (totalRoomsText.isEmpty()) {
            etTotalRooms.setError("Please enter total number of rooms");
            return;
        }

        // Check for valid number of rooms
        try {
            Integer.parseInt(totalRoomsText);
        } catch (NumberFormatException e) {
            etTotalRooms.setError("Please enter a valid number");
            return;
        }
        FirebaseUser user = mAuth.getCurrentUser();
        String landlordId;

        if (user != null) {
            landlordId = user.getUid();
        } else {
            Toast.makeText(this, "Error: Landlord is not logged in.", Toast.LENGTH_LONG).show();
            return;
        }

        ApartmentRequest request = new ApartmentRequest();
        request.setBuildingId(buildingId);
        request.setName(name);
        request.setTotalRooms(totalRoomsText); // Use the validated string value


        request.setLandlordId(landlordId);  // later: from logged-in user
        request.setDescription("apt test");                   // replace later
        request.setRentPrice(2000.00);                     // replace later
        request.setActive(true);

        //  API Call
        if (btnSave == null) {

            Toast.makeText(this, "Internal Error: Cannot find button to disable.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false); // Disable button during API call

        apartmentApi.createApartment(request).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                // Check for null before re-enabling
                if (btnSave != null) {
                    btnSave.setEnabled(true); // Re-enable button
                }

                if (!response.isSuccessful()) {
                    Toast.makeText(AddApartmentActivity.this,
                            "Failed to create Apartment: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                String msg = response.body() != null
                        ? response.body()
                        : "Apartment created successfully";
                // Intent to go back to the apartments list for the current building
                Intent intent = new Intent(AddApartmentActivity.this, LettingAgentApartmentsActivity.class);
                // We MUST pass the buildingId back so the list loads correctly
                intent.putExtra("EXTRA_BUILDING_ID", buildingId);
                // Note: The previous activity should be updated to handle a refresh on resume,
                // but this ensures the next screen gets the context it needs.
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                // Check for null before re-enabling
                if (btnSave != null) {
                    btnSave.setEnabled(true); // Re-enable button on failure
                }
                Toast.makeText(AddApartmentActivity.this,
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}