package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;

public class RoomTypeDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_type_detail);

        TextView tvApartmentHeader = findViewById(R.id.tvHeaderApartmentName);
        TextView tvRoomTypeTitle = findViewById(R.id.tvRoomTypeTitle);
        LinearLayout roomInstancesLayout = findViewById(R.id.layoutRoomInstances);

        String apartmentName = getIntent().getStringExtra("EXTRA_APARTMENT_NAME");
        String roomTypeName  = getIntent().getStringExtra("EXTRA_ROOM_TYPE_NAME");

        if (apartmentName != null) tvApartmentHeader.setText(apartmentName);
        if (roomTypeName != null) tvRoomTypeTitle.setText(roomTypeName);

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        int defaultCount = 2;
        int roomCount = getIntent().getIntExtra("EXTRA_ROOM_COUNT", defaultCount);

        for (int i = 1; i <= roomCount; i++) {

            View card = getLayoutInflater()
                    .inflate(R.layout.view_room_instance_card, roomInstancesLayout, false);

            TextView labelView = card.findViewById(R.id.tvRoomInstanceLabel);
            labelView.setText(roomTypeName + " " + i);

            // later: set click listener to open per-room settings screen
            // card.setOnClickListener(...);

            roomInstancesLayout.addView(card);
        }
    }
}
