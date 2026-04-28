package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;

public class Onboarding2Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding2);

        // NEXT →  Onboarding3
        LinearLayout nextBtn = findViewById(R.id.nextButton);
        nextBtn.setOnClickListener(v ->
                startActivity(new Intent(Onboarding2Activity.this, Onboarding3Activity.class))
        );

        // SKIP → SignUp
        findViewById(R.id.skipButton).setOnClickListener(v -> {
            startActivity(new Intent(Onboarding2Activity.this, TenantSignUpActivity.class));
            finish();
        });
    }
}
