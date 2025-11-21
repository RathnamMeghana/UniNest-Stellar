package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;

public class LettingAgentLoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letting_agent_login);

        // Hook up views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            // Basic validation only (no Firebase yet)
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                        LettingAgentLoginActivity.this,
                        "Please fill in both email and password",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(
                        LettingAgentLoginActivity.this,
                        "Please enter a valid email address",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            // Design-only "success"
            Toast.makeText(
                    LettingAgentLoginActivity.this,
                    "Login successful (design only)",
                    Toast.LENGTH_SHORT
            ).show();

            // Go to letting-agent home (buildings screen)
            Intent intent = new Intent(
                    LettingAgentLoginActivity.this,
                    LettingAgentBuildingsActivity.class
            );
            startActivity(intent);
            finish(); // don’t return to login on back
        });
    }
}
