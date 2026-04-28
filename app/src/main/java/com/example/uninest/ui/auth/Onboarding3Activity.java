package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;

public class Onboarding3Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding3);

        // --- Views ---
        LinearLayout nextButton = findViewById(R.id.nextButton);
        TextView skipButton = findViewById(R.id.skipButton);

        // NEXT,go to Welcome screen
        nextButton.setOnClickListener(v -> {
            Intent intent = new Intent(Onboarding3Activity.this, WelcomeActivity.class);
            startActivity(intent);
            finish();   // close onboarding so back doesn't return here
        });

        // SKIP,go straight to signup skip welcome too
        skipButton.setOnClickListener(v -> {
            Intent intent = new Intent(Onboarding3Activity.this, TenantSignUpActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
