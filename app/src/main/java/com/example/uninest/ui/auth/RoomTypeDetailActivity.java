package com.example.uninest.ui.auth;

import static android.content.Intent.getIntent;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

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

    private String houseCode = "APT-709F22C";
    private String roomTypeName;
    private int roomCounter = 0; // current number of rooms
    private ApartmentApi api;
    private LinearLayout roomInstancesLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_type_detail);

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

        fetchRooms();

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
        roomCounter++;
        String label = roomTypeName + " " + roomCounter;

        Room room = new Room();
        room.setType(roomTypeName);
        room.setLabel(label);


        api.createRoom(houseCode, room).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    addRoomCard(label); // update UI
                } else {
                    Log.e("API_CALL", "Failed to add room: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.e("API_CALL", "Network failure: " + t.getMessage());
            }
        });
    }

    private void fetchRooms() {
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
        // 1. Clear UI and reset counter
        roomInstancesLayout.removeAllViews();
        roomCounter = 0;


        final String targetRoomTypeName = roomTypeName != null ? roomTypeName.trim() : "";

        if (targetRoomTypeName.isEmpty()) {
            Log.e("ROOMS_ERROR", "roomTypeName is null or empty. Cannot filter.");
            return;
        }

        //  singular version for filtering
        String singularTarget = targetRoomTypeName;
        if (targetRoomTypeName.endsWith("s")) {
            // Simple heuristic: remove trailing 's' if the type is pluralized
            singularTarget = targetRoomTypeName.substring(0, targetRoomTypeName.length() - 1);
            Log.d("ROOMS_DEBUG", "Plural detected. Filtering with singular form: " + singularTarget);
        }

        final String finalTarget = singularTarget;
        Log.d("ROOMS_DEBUG", "Filtering for normalized target type: " + targetRoomTypeName +
                " (or singular: " + finalTarget + ")");


        for (Room room : rooms) {
            String roomApiType = room.getType();

            boolean isMatch = roomApiType != null &&
                    (roomApiType.equalsIgnoreCase(targetRoomTypeName) ||
                            roomApiType.equalsIgnoreCase(finalTarget));

            if (isMatch) {

                Log.d("ROOMS_DEBUG", "SUCCESS: Found a match for type " + roomApiType);

                roomCounter++;


                String cardLabel = room.getLabel();

                // If the API didn't get a label add one using the original roomTypeName
                if (cardLabel == null || cardLabel.isEmpty()) {
                    cardLabel = targetRoomTypeName + " " + roomCounter;
                }


                addRoomCard(cardLabel);
            } else {
                Log.d("ROOMS_DEBUG", "FAIL: Skipped room with type: " + roomApiType +
                        ". Target was: " + targetRoomTypeName);
            }
        }
    }
}
