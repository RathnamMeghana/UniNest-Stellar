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

    // prevents double submits -> avoids duplicate buildings
    private boolean isSubmitting = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_building);

        mAuth = FirebaseAuth.getInstance();

        etBuildingName = findViewById(R.id.etBuildingName);
        imgPreview = findViewById(R.id.imgPreview);
        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSaveBuilding);

        // Retrofit API
        buildingApi = ApiClient.getBuildingApi();

        imgPreview.setOnClickListener(v ->
                Toast.makeText(this, "Image picker TODO", Toast.LENGTH_SHORT).show()
        );

        btnCancel.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> saveBuilding());
    }

    private void saveBuilding() {
        if (isSubmitting) return;
        isSubmitting = true;
        btnSave.setEnabled(false);

        String name = etBuildingName.getText().toString().trim();

        if (name.isEmpty()) {
            isSubmitting = false;
            btnSave.setEnabled(true);
            etBuildingName.setError("Please enter a building name");
            return;
        }


        // --- FIX: Get the currently logged-in user's ID ---
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            isSubmitting = false;
            btnSave.setEnabled(true);
            Toast.makeText(this, "Error: User is not logged in.", Toast.LENGTH_LONG).show();
            return;
        }

        String landlordId = user.getUid();


        // TODO: once you add more fields to the layout, read them here
        BuildingRequest request = new BuildingRequest();
        request.setName(name);
        request.setAddressLine1("Dummy address line 1");
        request.setCity("Dummy city");
        request.setPostcode("0000");
        request.setCountry("Ireland");
        request.setLandlordId(landlordId);
        request.setActive(true);

        buildingApi.createBuilding(request).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                isSubmitting = false;
                btnSave.setEnabled(true);

                if (!response.isSuccessful()) {
                    Toast.makeText(AddBuildingActivity.this,
                            "Failed to create building: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(AddBuildingActivity.this, "Building created!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                isSubmitting = false;
                btnSave.setEnabled(true);

                Log.e("AddBuildingActivity", "API Error", t);
                Toast.makeText(AddBuildingActivity.this,
                        "Error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}