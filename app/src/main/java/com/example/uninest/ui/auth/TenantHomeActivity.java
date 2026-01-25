package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class TenantHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_home);

        SessionManager sessionManager = new SessionManager(this);
        String houseCode = sessionManager.fetchHouseCode();

        // Setup Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_home); // Highlight Home

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_calendar) {
                startActivity(new Intent(getApplicationContext(), TenantCalendarActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_tickets) {
                startActivity(new Intent(getApplicationContext(), TenantTicketsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }
}