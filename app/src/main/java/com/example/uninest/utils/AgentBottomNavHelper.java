package com.example.uninest.utils;

import android.content.Intent;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.ui.auth.LettingAgentBuildingsActivity;
import com.example.uninest.ui.auth.LettingAgentHomeActivity;
import com.example.uninest.ui.auth.LettingAgentNotificationsActivity;
import com.example.uninest.ui.auth.LettingAgentProfileActivity;
import com.example.uninest.ui.auth.LettingAgentTicketsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public final class AgentBottomNavHelper {

    private AgentBottomNavHelper() {
    }

    public static void setup(AppCompatActivity activity, @IdRes int selectedId) {
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNavigationView);
        if (bottomNav == null) {
            return;
        }

        bottomNav.setSelectedItemId(selectedId);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == selectedId) {
                return true;
            }

            Intent intent = buildIntent(activity, itemId);
            if (intent == null) {
                return false;
            }

            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
            activity.finish();
            return true;
        });
    }

    public static void syncSelected(AppCompatActivity activity, @IdRes int selectedId) {
        BottomNavigationView bottomNav = activity.findViewById(R.id.bottomNavigationView);
        if (bottomNav != null && bottomNav.getSelectedItemId() != selectedId) {
            bottomNav.setSelectedItemId(selectedId);
        }
    }

    private static Intent buildIntent(AppCompatActivity activity, int itemId) {
        if (itemId == R.id.nav_home) {
            return new Intent(activity, LettingAgentHomeActivity.class);
        }
        if (itemId == R.id.nav_tickets) {
            return new Intent(activity, LettingAgentTicketsActivity.class);
        }
        if (itemId == R.id.nav_buildings) {
            return new Intent(activity, LettingAgentBuildingsActivity.class);
        }
        if (itemId == R.id.nav_notifications) {
            return new Intent(activity, LettingAgentNotificationsActivity.class);
        }
        if (itemId == R.id.nav_profile) {
            return new Intent(activity, LettingAgentProfileActivity.class);
        }
        return null;
    }
}
