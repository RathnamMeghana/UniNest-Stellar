package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        LinearLayout getStartedButton = findViewById(R.id.getStartedButton);

        // Get Started, LOGIN and clear onboarding from back stack
        getStartedButton.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, TenantSignUpActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
