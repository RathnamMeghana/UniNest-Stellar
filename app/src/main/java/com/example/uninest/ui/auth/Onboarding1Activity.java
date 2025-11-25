package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;

public class Onboarding1Activity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding1);

        // NEXT → Onboarding 2
        LinearLayout nextBtn = findViewById(R.id.nextButton);
        nextBtn.setOnClickListener(v ->
                startActivity(new Intent(Onboarding1Activity.this, Onboarding2Activity.class))
        );

        // SKIP → SignUp
        findViewById(R.id.skipButton).setOnClickListener(v -> {
            startActivity(new Intent(Onboarding1Activity.this, TenantSignUpActivity.class));
            finish();
        });
    }
}
