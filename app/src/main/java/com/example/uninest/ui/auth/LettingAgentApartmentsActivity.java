package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;

public class LettingAgentApartmentsActivity extends AppCompatActivity {

    private LinearLayout apartmentList;
    private String buildingName;   //  store so we can use it in click listeners

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letting_agent_apartments);

        apartmentList = findViewById(R.id.layoutApartmentList);
        Button btnNewApartment = findViewById(R.id.btnNewApartment);
        TextView tvBuildingName = findViewById(R.id.tvBuildingName);

        // 1️Get building name passed from LettingAgentBuildingsActivity
        buildingName = getIntent().getStringExtra("EXTRA_BUILDING_NAME");

        if (buildingName != null && !buildingName.isEmpty()) {
            tvBuildingName.setText(buildingName);
        } else {
            tvBuildingName.setText("Apartments");
        }

        // For now: add hard-coded apartments (dummy data)
        addDummyApartments();

        // New Apt button -> open AddApartmentActivity (design only for now)
        btnNewApartment.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LettingAgentApartmentsActivity.this,
                    AddApartmentActivity.class
            );
            startActivity(intent);
        });

        // Bottom nav clicks
        findViewById(R.id.navTickets).setOnClickListener(v -> {
            // TODO navigate to TicketsActivity
        });

        findViewById(R.id.navApartments).setOnClickListener(v -> {
            // current screen – maybe scroll to top
            apartmentList.scrollTo(0, 0);
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            // TODO navigate to ProfileActivity
        });
    }

    private void addDummyApartments() {
        // Apartment 1
        ApartmentCardView card1 = new ApartmentCardView(this);
        card1.setApartmentName("Apartment 1");
        card1.setTenantInfo(5, 5);   // 5/5 Tenants
        card1.setOnClickListener(v -> openApartmentTenants("Apartment 1"));
        apartmentList.addView(card1);

        // Apartment 2
        ApartmentCardView card2 = new ApartmentCardView(this);
        card2.setApartmentName("Apartment 2");
        card2.setTenantInfo(3, 5);   // 3/5 Tenants
        card2.setOnClickListener(v -> openApartmentTenants("Apartment 2"));
        apartmentList.addView(card2);

        // Apartment 3
        ApartmentCardView card3 = new ApartmentCardView(this);
        card3.setApartmentName("Apartment 3");
        card3.setTenantInfo(4, 5);
        card3.setOnClickListener(v -> openApartmentTenants("Apartment 3"));
        apartmentList.addView(card3);
    }

    //  Reusable method to navigate to tenants screen
    private void openApartmentTenants(String apartmentName) {
        Intent intent = new Intent(
                LettingAgentApartmentsActivity.this,
                ApartmentTenantsActivity.class
        );
        intent.putExtra("EXTRA_BUILDING_NAME", buildingName);
        intent.putExtra("EXTRA_APARTMENT_NAME", apartmentName);
        startActivity(intent);
    }
}
