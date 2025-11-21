package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;

public class AddApartmentActivity extends AppCompatActivity {

    private EditText etApartmentName;
    private EditText etTotalRooms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_apartment);

        etApartmentName = findViewById(R.id.etApartmentName);
        etTotalRooms = findViewById(R.id.etTotalRooms);
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnSave = findViewById(R.id.btnSaveApartment);

        btnCancel.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            String name = etApartmentName.getText().toString().trim();
            String totalRoomsText = etTotalRooms.getText().toString().trim();

            if (name.isEmpty()) {
                etApartmentName.setError("Please enter an apartment name");
                return;
            }

            if (totalRoomsText.isEmpty()) {
                etTotalRooms.setError("Please enter total number of rooms");
                return;
            }

            int totalRooms;
            try {
                totalRooms = Integer.parseInt(totalRoomsText);
            } catch (NumberFormatException e) {
                etTotalRooms.setError("Please enter a valid number");
                return;
            }

            // Later: call Spring Boot + Firebase API here with name + totalRooms.
            Toast.makeText(
                    AddApartmentActivity.this,
                    "Apartment saved: " + name + " (" + totalRooms + " rooms) (design only, API later)",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
        });
    }
}
