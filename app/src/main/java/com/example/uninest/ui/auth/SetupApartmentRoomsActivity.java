package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;

public class SetupApartmentRoomsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_apartment_rooms);

        View btnNext = findViewById(R.id.btnNextRooms);
        btnNext.setOnClickListener(v -> {
            // later you can return selected room counts via setResult(...)


            finish();
        });
    }
}
