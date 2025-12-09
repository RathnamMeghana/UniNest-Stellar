package com.example.uninest.ui.auth;

import static android.content.Intent.getIntent;

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

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoomTypeDetailActivity extends AppCompatActivity {

    private String houseCode;
    private String roomTypeName;
    private int roomCounter = 0; // current number of rooms
    private ApartmentApi api;
    private LinearLayout roomInstancesLayout;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_type_detail);
        userRole = getIntent().getStringExtra("EXTRA_USER_ROLE");
        houseCode = getIntent().getStringExtra("EXTRA_HOUSE_CODE");

        // UI references
        TextView tvApartmentHeader = findViewById(R.id.tvHeaderApartmentName);
        TextView tvRoomTypeTitle = findViewById(R.id.tvRoomTypeTitle);

        // If your LinearLayout already has an ID inside ScrollView
        roomInstancesLayout = findViewById(R.id.layoutRoomInstances);

        // Intent extras
        //houseCode = getIntent().getStringExtra("EXTRA_HOUSE_CODE");
        roomTypeName = getIntent().getStringExtra("EXTRA_ROOM_TYPE_NAME");
        String apartmentName = getIntent().getStringExtra("EXTRA_APARTMENT_NAME");



        if (apartmentName != null) tvApartmentHeader.setText(apartmentName);
        if (roomTypeName != null) tvRoomTypeTitle.setText(roomTypeName);

        // Initialize API
        api = ApiClient.getApartmentApi();

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        if (houseCode != null && !houseCode.isEmpty()) {
            fetchRooms(houseCode);
        } else {
            Log.e("TENANTS_ACTIVITY", "House code is missing for fetching rooms");
        }



        // Load initial rooms based on intent or default
        //int defaultCount = 2;
        //int roomCount = getIntent().getIntExtra("EXTRA_ROOM_COUNT", defaultCount);
        //roomCounter = roomCount;

        //for (int i = 1; i <= roomCount; i++) {
        //    addRoomCard(roomTypeName + " " + i);
        // }


    }

    // Add a room card to the existing layout
    private void addRoomCard(String label) {
        View card = getLayoutInflater().inflate(R.layout.view_room_instance_card, roomInstancesLayout, false);
        TextView labelView = card.findViewById(R.id.tvRoomInstanceLabel);
        labelView.setText(label);
        roomInstancesLayout.addView(card);
    }

    // Call backend API to create a new room
    private void addNewRoom() {
        roomCounter++; // increment the counter
        String label = roomTypeName + " " + roomCounter; // like Bedroom 1

        Room room = new Room();
        room.setType(roomTypeName);
        room.setLabel(label);

        api.createRoom(houseCode, room).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    addRoomCard(label); // update UI immediately
                    Toast.makeText(RoomTypeDetailActivity.this, "Room added", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("API_CALL", "Failed to add room: " + response.code());
                    Toast.makeText(RoomTypeDetailActivity.this, "Failed to add room", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.e("API_CALL", "Network failure: " + t.getMessage());
                Toast.makeText(RoomTypeDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void fetchRooms(String houseCode) {
        api.getRooms(houseCode).enqueue(new Callback<List<Room>>() {
            @Override
            public void onResponse(Call<List<Room>> call, Response<List<Room>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayFilteredRooms(response.body());
                } else {
                    Log.e("ROOMS", "Failed to fetch rooms: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Room>> call, Throwable t) {
                Log.e("ROOMS", "Network error: " + t.getMessage());
            }
        });
    }

    private void displayRooms(List<Room> rooms) {
        roomInstancesLayout.removeAllViews(); // clear any previous cards

        for (Room room : rooms) {
            addRoomCard(room.getType());
        }
    }


    private void displayFilteredRooms(List<Room> rooms) {
        roomInstancesLayout.removeAllViews();
        roomCounter = 0;

        if (roomTypeName == null || roomTypeName.isEmpty()) {
            Log.e("ROOMS_ERROR", "roomTypeName is null or empty. Cannot filter.");
            return;
        }

        String target = roomTypeName.trim();

        for (Room room : rooms) {
            if (room.getType() != null && room.getType().trim().equalsIgnoreCase(target)) {
                roomCounter++;

                String label = room.getLabel();
                if (label == null || label.isEmpty()) {
                    label = target + " " + roomCounter; // fallback label
                }

                addRoomCard(label);
            }
        }

        if (roomCounter == 0) {
            Log.d("ROOMS_DEBUG", "No rooms found for type: " + target);
        }
    }
}
