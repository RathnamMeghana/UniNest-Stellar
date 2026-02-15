package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.Calendar;

public class TenantHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_home);

        SessionManager sessionManager = new SessionManager(this);

        TextView tvGreeting = findViewById(R.id.tvGreeting);

        String fullName = sessionManager.getUserFullName();

        String firstName = (fullName != null && fullName.contains(" "))
                ? fullName.split(" ")[0] : (fullName != null ? fullName : "User");

        // Determine Time of Day for greeting
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String timeGreeting = "Good Morning";
        if (hour >= 12 && hour < 17) timeGreeting = "Good Afternoon";
        else if (hour >= 17) timeGreeting = "Good Evening";

        tvGreeting.setText(timeGreeting + ",\n" + firstName + "!");

        // 2. BOTTOM NAV LOGIC
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) return true;

            if (itemId == R.id.nav_bills) {
                startActivity(new Intent(this, TenantBillsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_calendar) {
                startActivity(new Intent(this, TenantCalendarActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_tickets) {
                startActivity(new Intent(this, TenantTicketsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }
}