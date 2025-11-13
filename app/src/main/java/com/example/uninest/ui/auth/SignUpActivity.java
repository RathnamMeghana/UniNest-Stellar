package com.example.uninest.ui.auth;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.util.Patterns;
import android.widget.AutoCompleteTextView;
import androidx.appcompat.app.AppCompatActivity;


import com.example.uninest.R;

public class SignUpActivity extends AppCompatActivity {

    // private Spinner spinnerCompanyName;
    private AutoCompleteTextView actvCompanyName;
    private EditText etCompanyEmail, etPassword, etConfirmPassword;
    private Button btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // connect XML views to Java
        actvCompanyName = findViewById(R.id.actvCompanyName);
        etCompanyEmail = findViewById(R.id.etCompanyEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);

        setupCompanySpinner();
        setupSignUpButton();
    }

    private void setupCompanySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.letting_companies,
                android.R.layout.simple_list_item_1
        );
        actvCompanyName.setAdapter(adapter);
    }

    private void setupSignUpButton() {
        btnSignUp.setOnClickListener(v -> {
            String company = actvCompanyName.getText().toString();
            String email = etCompanyEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirm = etConfirmPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(SignUpActivity.this,
                        "Please fill in all fields",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(SignUpActivity.this,
                        "Please enter a valid email address",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirm)) {
                Toast.makeText(SignUpActivity.this,
                        "Passwords do not match",
                        Toast.LENGTH_SHORT).show();
                return;
            }

        });
    }
}