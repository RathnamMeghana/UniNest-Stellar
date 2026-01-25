package com.example.uninest.ui.auth;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.uninest.R;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.BuildingApi;
import com.example.uninest.model.BuildingRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

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
    private Uri selectedImageUri;
    private boolean isSubmitting = false;

    // 1. Setup the Image Picker Launcher
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            selectedImageUri = result.getData().getData();
                            Glide.with(this).load(selectedImageUri).into(imgPreview);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_building);

        mAuth = FirebaseAuth.getInstance();
        etBuildingName = findViewById(R.id.etBuildingName);
        imgPreview = findViewById(R.id.imgPreview);
        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSaveBuilding);

        buildingApi = ApiClient.getBuildingApi();

        // 2. Click to pick image
        imgPreview.setOnClickListener(v -> openPhotoPicker());

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveBuilding());
    }

    private void openPhotoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void saveBuilding() {
        if (isSubmitting) return;

        String name = etBuildingName.getText().toString().trim();
        FirebaseUser user = mAuth.getCurrentUser();

        // Validations
        if (name.isEmpty()) {
            etBuildingName.setError("Please enter a building name");
            return;
        }
        if (user == null) {
            Toast.makeText(this, "Error: User is not logged in.", Toast.LENGTH_LONG).show();
            return;
        }
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        isSubmitting = true;
        btnSave.setEnabled(false);

        // 3. Convert Image to Base64
        String base64Image = convertImageToBase64(selectedImageUri);

        // 4. Create Request for the API
        BuildingRequest request = new BuildingRequest();
        request.setName(name);
        request.setAddressLine1("Dummy address line 1");
        request.setCity("Dummy city");
        request.setPostcode("0000");
        request.setCountry("Ireland");
        request.setLandlordId(user.getUid());
        request.setActive(true);
        request.setImageUrl(base64Image); // Sending the encoded image string to your API

        // 5. Call your Spring Boot API
        buildingApi.createBuilding(request).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                isSubmitting = false;
                btnSave.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(AddBuildingActivity.this, "Building created via API!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Log.e("API_ERROR", "Code: " + response.code());
                    Toast.makeText(AddBuildingActivity.this, "Server Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                isSubmitting = false;
                btnSave.setEnabled(true);
                Log.e("AddBuildingActivity", "API Failure", t);
                Toast.makeText(AddBuildingActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper method to turn URI into Base64 string
    private String convertImageToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Use 25% quality to stay under Firestore document limits
            bitmap.compress(Bitmap.CompressFormat.JPEG, 25, baos);
            byte[] bytes = baos.toByteArray();
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e("IMAGE_ERROR", "Failed to encode image", e);
            return null;
        }
    }
}