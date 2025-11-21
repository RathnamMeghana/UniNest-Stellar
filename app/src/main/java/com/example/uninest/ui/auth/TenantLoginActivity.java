package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.google.android.material.button.MaterialButton;

public class TenantLoginActivity extends AppCompatActivity {

    private EditText etTenantEmail, etTenantPassword;
    private MaterialButton btnTenantLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_login);

        etTenantEmail = findViewById(R.id.etTenantEmail);
        etTenantPassword = findViewById(R.id.etTenantPassword);
        btnTenantLogin = findViewById(R.id.btnTenantLogin);

        btnTenantLogin.setOnClickListener(v -> {
            String email = etTenantEmail.getText().toString().trim();
            String password = etTenantPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                        TenantLoginActivity.this,
                        "Please fill in both email and password",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(
                        TenantLoginActivity.this,
                        "Please enter a valid email address",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            // Design-only success
            Toast.makeText(
                    TenantLoginActivity.this,
                    "Tenant login successful (design only)",
                    Toast.LENGTH_SHORT
            ).show();

            // TODO: replace MainActivity with real tenant home when we have it
            // Intent intent = new Intent(TenantLoginActivity.this, TenantHomeActivity.class);
            // startActivity(intent);
            // finish();
        });
    }
}
