package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;

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
            Intent intent = new Intent(SplashActivity.this, Onboarding1Activity.class);
            startActivity(intent);
            finish();
        }, 1500);  // 1.5 seconds
    }
}
