package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;

public class ApartmentTenantsActivity extends AppCompatActivity {

    private LinearLayout tenantList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apartment_tenants);

        tenantList = findViewById(R.id.layoutTenantList);
        TextView tvBuildingName = findViewById(R.id.tvBuildingName);
        TextView tvApartmentName = findViewById(R.id.tvApartmentName);
        TextView tvTenantCount = findViewById(R.id.tvTenantCount);

        // Get extras from previous screen (later, when we wire navigation)
        String buildingName = getIntent().getStringExtra("EXTRA_BUILDING_NAME");
        String apartmentName = getIntent().getStringExtra("EXTRA_APARTMENT_NAME");

        if (buildingName != null && !buildingName.isEmpty()) {
            tvBuildingName.setText(buildingName);
        } else {
            tvBuildingName.setText("Apartments");
        }

        if (apartmentName != null && !apartmentName.isEmpty()) {
            tvApartmentName.setText(apartmentName);
        } else {
            tvApartmentName.setText("Apartment");
        }

        // For now: hard-coded dummy tenants
        addDummyTenants();

        // Update tenant count text based on dummy list
        int occupied = 3;  // number of dummy tenants
        int capacity = 5;  // just an example
        tvTenantCount.setText(occupied + "/" + capacity + " Tenants");

    }

    private void addDummyTenants() {
        TenantCardView t1 = new TenantCardView(this);
        t1.setTenantName("Maya Smith");
        t1.setRoomLabel("Room 1");
        tenantList.addView(t1);

        TenantCardView t2 = new TenantCardView(this);
        t2.setTenantName("Ruth Mainland");
        t2.setRoomLabel("Room 2");
        tenantList.addView(t2);

        TenantCardView t3 = new TenantCardView(this);
        t3.setTenantName("Sarah Williams");
        t3.setRoomLabel("Room 3");
        tenantList.addView(t3);
    }
}
