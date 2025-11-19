package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;

public class LettingAgentBuildingsActivity extends AppCompatActivity {

    private LinearLayout buildingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letting_agent_buildings);

        buildingList = findViewById(R.id.layoutBuildingList);
        Button btnNewBuilding = findViewById(R.id.btnNewBuilding);

        // For now: adding hard-coded buildings using the reusable component
        addDummyBuildings();

        // New Building button -> open AddBuildingActivity (design only)
        btnNewBuilding.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LettingAgentBuildingsActivity.this,
                    AddBuildingActivity.class
            );
            startActivity(intent);
        });

        // Bottom nav clicks
        findViewById(R.id.navTickets).setOnClickListener(v -> {
            // TODO navigate to TicketsActivity
        });

        findViewById(R.id.navApartments).setOnClickListener(v -> {
            // current screen – maybe scroll to top
            buildingList.scrollTo(0, 0);
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            // TODO navigate to ProfileActivity
        });
    }

    private void addDummyBuildings() {
        // Green Park
        BuildingCardView card1 = new BuildingCardView(this);
        card1.setBuildingName("Green Park Student Accommodation");
        card1.setApartmentCount(40);
        card1.setBuildingImage(R.drawable.green_park_placeholder);
        card1.setOnClickListener(v -> {
            // TODO open Apartments list for Green Park
        });
        buildingList.addView(card1);

        // Mourne View
        BuildingCardView card2 = new BuildingCardView(this);
        card2.setBuildingName("Mourne View Student Accommodation");
        card2.setApartmentCount(20);
        card2.setBuildingImage(R.drawable.mourne_view_placeholder);
        card2.setOnClickListener(v -> {
            // TODO open Apartments list for Mourne View
        });
        buildingList.addView(card2);
    }
}
