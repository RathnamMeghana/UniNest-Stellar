package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.BuildConfig;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.logoImageView);

        // Start pop animation
        Animation popAnim = AnimationUtils.loadAnimation(this, R.anim.logo_pop);
        logo.startAnimation(popAnim);

        // Go to next screen after short delay
        new Handler().postDelayed(() -> {
            SessionManager sessionManager = new SessionManager(SplashActivity.this);
            String role = sessionManager.getUserRole();

            Intent intent;

            if ("agent".equals(BuildConfig.FLAVOR)) {
                // AGENT APP ROUTING
                if ("1".equals(role)) {
                    intent = new Intent(SplashActivity.this, LettingAgentBuildingsActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, LettingAgentLoginActivity.class);
                }
            } else {
                // TENANT APP ROUTING (Default)
                if ("2".equals(role)) {
                    // Active Tenant Session
                    intent = new Intent(SplashActivity.this, TenantHomeActivity.class);
                } else if (!sessionManager.isFirstTimeSetup()) {
                    // Not first time, but not logged in
                    intent = new Intent(SplashActivity.this, TenantLoginActivity.class);
                } else {
                    // First time ever opening the app
                    intent = new Intent(SplashActivity.this, Onboarding1Activity.class);
                }
            }

            //  clear the task stack so users can't press 'back' to the splash screen
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 1500);  // 1.5 seconds
    }
}
