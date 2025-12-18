package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.data.api.ApartmentApi;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.Room;

import retrofit2.Call;
import retrofit2.Response;

public class SetupApartmentRoomsActivity extends AppCompatActivity {

    private String houseCode;
    private String userRole;
    private ApartmentApi api;
    private LinearLayout layoutRoomCounters; // store globally


    private final String[] roomTypes = {
            "Bedroom",
            "Bathroom",
            "Kitchen",
            "Living Room",
            "Laundry"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_apartment_rooms);

        api = ApiClient.getApartmentApi();


        // get extras
        houseCode = getIntent().getStringExtra("EXTRA_HOUSE_CODE");

        userRole = getIntent().getStringExtra("EXTRA_USER_ROLE");


        layoutRoomCounters = findViewById(R.id.layoutRoomCounters);

        setupRoomRows();

        findViewById(R.id.btnNextRooms).setOnClickListener(v -> addRoomsFromCounters());
    }

    private void setupRoomRows() {

        for (int i = 0; i < layoutRoomCounters.getChildCount(); i++) {

            View row = layoutRoomCounters.getChildAt(i);

            TextView tvLabel = row.findViewById(R.id.tvRoomLabel);
            TextView tvCount = row.findViewById(R.id.tvRoomCount);
            TextView btnMinus = row.findViewById(R.id.btnMinus);
            TextView btnPlus = row.findViewById(R.id.btnPlus);

            // assign room type
            tvLabel.setText(roomTypes[i]);

            tvCount.setText("0");

            // plus button
            btnPlus.setOnClickListener(v -> {
                int count = Integer.parseInt(tvCount.getText().toString());
                tvCount.setText(String.valueOf(count + 1));

            });

            // minus button
            btnMinus.setOnClickListener(v -> {
                int count = Integer.parseInt(tvCount.getText().toString());
                if (count > 0) tvCount.setText(String.valueOf(count - 1));
            });
        }
    }

    /**
     * Collect all room counts and send them to backend
     */
    private void addRoomsFromCounters() {
        int totalRooms = 0;

        for (int i = 0; i < layoutRoomCounters.getChildCount(); i++) {
            View row = layoutRoomCounters.getChildAt(i);
            TextView tvCount = row.findViewById(R.id.tvRoomCount);
            totalRooms += Integer.parseInt(tvCount.getText().toString());
        }


        // Second pass: Create rooms with unique labels
        for (int i = 0; i < layoutRoomCounters.getChildCount(); i++) {
            View row = layoutRoomCounters.getChildAt(i);
            TextView tvLabel = row.findViewById(R.id.tvRoomLabel);
            TextView tvCount = row.findViewById(R.id.tvRoomCount);

            String type = tvLabel.getText().toString();
            int count = Integer.parseInt(tvCount.getText().toString());

            // Loop from 1 up to the count to assign "1", "2", "3", etc.
            for (int j = 1; j <= count; j++) {
                // CHANGE: Pass 'j' (the room number) to createRoom
                createRoom(type, j, totalRooms);
            }
        }
    }


    private int roomsAdded = 0;

    private void createRoom(String type, int roomNumber, int totalRooms) {


        String roomLabel = type + " " + roomNumber;

        Room room = new Room();
        room.setType(type);
        room.setLabel(roomLabel);

        api.createRoom(houseCode, room).enqueue(new retrofit2.Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                roomsAdded++;
                if (!response.isSuccessful()) {
                    Log.e("CREATE_ROOM", "Error code: " + response.code() + ", body: " + response.errorBody());
                }

                if (roomsAdded >= totalRooms) {
                    Toast.makeText(SetupApartmentRoomsActivity.this, "Rooms added!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                roomsAdded++;
                Log.e("CREATE_ROOM", "Network error", t);

                if (roomsAdded >= totalRooms) {
                    Toast.makeText(SetupApartmentRoomsActivity.this, "Rooms added!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

        });
    }
    }
