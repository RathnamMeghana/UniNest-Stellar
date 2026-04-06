package com.example.uninest.ui.auth;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.example.uninest.utils.ImageUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddBuildingActivity extends AppCompatActivity {

    private EditText etBuildingName;
    private ImageView imgPreview;
    private Button btnSave;
    private Uri selectedImageUri;
    private BuildingApi buildingApi;
    private FirebaseAuth mAuth;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            selectedImageUri = result.getData().getData();
                            Glide.with(this).load(selectedImageUri).fitCenter().into(imgPreview);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_building);

        mAuth = FirebaseAuth.getInstance();
        buildingApi = ApiClient.getBuildingApi();
        etBuildingName = findViewById(R.id.etBuildingName);
        imgPreview = findViewById(R.id.imgPreview);
        btnSave = findViewById(R.id.btnSaveBuilding);

        imgPreview.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveBuilding());
    }

    private void saveBuilding() {
        String name = etBuildingName.getText().toString().trim();
        FirebaseUser user = mAuth.getCurrentUser();

        if (name.isEmpty() || selectedImageUri == null || user == null) {
            Toast.makeText(this, "Please fill name and select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Processing...");

        // Convert image to a SMALL Base64 string
        String smallBase64 = convertImageToResizedBase64(selectedImageUri);

        if (smallBase64 == null) {
            btnSave.setEnabled(true);
            btnSave.setText("Save Building");
            return;
        }

        BuildingRequest request = new BuildingRequest();
        request.setName(name);
        request.setLandlordId(user.getUid());
        request.setImageUrl(smallBase64); // This is now a safe, short string
        request.setAddressLine1("Street");
        request.setCity("City");
        request.setPostcode("Postcode");
        request.setCountry("Country");
        request.setActive(true);

        buildingApi.createBuilding(request).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddBuildingActivity.this, "Building Created!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    btnSave.setEnabled(true);
                    Toast.makeText(AddBuildingActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                btnSave.setEnabled(true);
                com.example.uninest.utils.NetworkErrorDialog.show(
                        AddBuildingActivity.this,
                        AddBuildingActivity.this::saveBuilding
                );
            }
        });
    }

    private String convertImageToResizedBase64(Uri uri) {
        try {
            return ImageUtils.encodeImageUriToBase64(this, uri, 640, 60);
        } catch (Exception e) {
            Log.e("IMAGE_ERROR", "Resizing failed", e);
            return null;
        }
    }
}
