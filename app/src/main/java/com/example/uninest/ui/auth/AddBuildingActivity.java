package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;

public class AddBuildingActivity extends AppCompatActivity {

    private EditText etBuildingName;
    private ImageView imgPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_building);

        etBuildingName = findViewById(R.id.etBuildingName);
        imgPreview = findViewById(R.id.imgPreview);
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnSave = findViewById(R.id.btnSaveBuilding);

        imgPreview.setOnClickListener(v ->
                Toast.makeText(this, "Image picker TODO", Toast.LENGTH_SHORT).show()
        );

        btnCancel.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            String name = etBuildingName.getText().toString().trim();

            if (name.isEmpty()) {
                etBuildingName.setError("Please enter a building name");
                return;
            }

            // Later: call Spring Boot + Firebase API here.
            Toast.makeText(
                    AddBuildingActivity.this,
                    "Building saved (design only, API later)",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
        });
    }
}
