package com.example.uninest.ui.auth;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.uninest.R;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class TenantSignUpActivity extends AppCompatActivity {

    private EditText etHouseCode, etTenantEmail, etTenantPassword, etTenantConfirmPassword;
    private Button btnTenantSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_sign_up);

        // Hook up views
        etHouseCode = findViewById(R.id.etHouseCode);
        etTenantEmail = findViewById(R.id.etTenantEmail);
        etTenantPassword = findViewById(R.id.etTenantPassword);
        etTenantConfirmPassword = findViewById(R.id.etTenantConfirmPassword);
        btnTenantSignUp = findViewById(R.id.btnTenantSignUp);

        // Button click
        btnTenantSignUp.setOnClickListener(v -> {
            String houseCode = etHouseCode.getText().toString().trim();
            String email = etTenantEmail.getText().toString().trim();
            String password = etTenantPassword.getText().toString();
            String confirm = etTenantConfirmPassword.getText().toString();

            if (houseCode.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(TenantSignUpActivity.this,
                        "Please fill in all fields",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(TenantSignUpActivity.this,
                        "Please enter a valid email address",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirm)) {
                Toast.makeText(TenantSignUpActivity.this,
                        "Passwords do not match",
                        Toast.LENGTH_SHORT).show();
                return;
            }

        });
    }
}