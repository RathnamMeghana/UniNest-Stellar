package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.BuildingApi;
import com.example.uninest.model.BuildingRequest;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuth;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddBuildingActivity extends AppCompatActivity {

    private EditText etBuildingName;
    private ImageView imgPreview;
    private Button btnSave;
    private Button btnCancel;

    private BuildingApi buildingApi;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_building);

        etBuildingName = findViewById(R.id.etBuildingName);
        imgPreview      = findViewById(R.id.imgPreview);
        btnCancel       = findViewById(R.id.btnCancel);
        btnSave         = findViewById(R.id.btnSaveBuilding);

        // Retrofit API
        buildingApi = ApiClient.getBuildingApi();

        imgPreview.setOnClickListener(v ->
                Toast.makeText(this, "Image picker TODO", Toast.LENGTH_SHORT).show()
        );

        btnCancel.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveBuilding());
    }

    private void saveBuilding() {
        String name = etBuildingName.getText().toString().trim();

        if (name.isEmpty()) {
            etBuildingName.setError("Please enter a building name");
            return;
        }

        // --- FIX: Get the currently logged-in user's ID ---
        FirebaseUser user = mAuth.getCurrentUser();
        String landlordId;

        if (user != null) {
            // Use the real Firebase User ID (UID)
            landlordId = user.getUid();
        } else {
            // Handle case where user is not logged in (should not happen in production)
            Toast.makeText(this, "Error: User is not logged in.", Toast.LENGTH_LONG).show();
            return;
        }

        // TODO: once you add more fields to the layout, read them here
        BuildingRequest request = new BuildingRequest();
        request.setName(name);
        request.setAddressLine1("Dummy address line 1"); // replace later
        request.setCity("Dummy city");                   // replace later
        request.setPostcode("0000");                     // replace later
        request.setCountry("Ireland");                   // replace later
        request.setLandlordId(landlordId);               // NOW using the real logged-in user ID
        request.setActive(true);

        btnSave.setEnabled(false);

        buildingApi.createBuilding(request).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                btnSave.setEnabled(true);

                if (!response.isSuccessful()) {
                    // Log the error body if available for debugging
                    String errorBody = response.errorBody() != null ? response.errorBody().toString() : "";
                    Log.e("AddBuildingActivity", "Failed to create building: " + response.code() + ", Body: " + errorBody);
                    Toast.makeText(AddBuildingActivity.this,
                            "Failed to create building: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                String msg = response.body() != null
                        ? response.body()
                        : "Building created successfully";

                Toast.makeText(AddBuildingActivity.this, msg, Toast.LENGTH_SHORT).show();
                finish(); // go back to buildings list
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                btnSave.setEnabled(true);
                Log.e("AddBuildingActivity", "API Error", t);
                Toast.makeText(AddBuildingActivity.this,
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}