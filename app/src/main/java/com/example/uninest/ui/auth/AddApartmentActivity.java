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

import java.sql.Timestamp;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddApartmentActivity extends AppCompatActivity {

    private EditText etApartmentName;
    private EditText etTotalRooms;
    private ApartmentApi apartmentApi;
    private Button btnSave;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_apartment);

        etApartmentName = findViewById(R.id.etApartmentName);
        etTotalRooms = findViewById(R.id.etTotalRooms);
        Button btnCancel = findViewById(R.id.btnCancel);


        btnSave = findViewById(R.id.btnSaveApartment);
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


        ApartmentRequest request = new ApartmentRequest();
        request.setBuildingId("123");
        request.setName(name);
        request.setTotalRooms(totalRoomsText); // Use the validated string value


        request.setLandlordId("123");  // later: from logged-in user
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
                Toast.makeText(AddApartmentActivity.this, msg, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(AddApartmentActivity.this, LettingAgentApartmentsActivity.class);
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