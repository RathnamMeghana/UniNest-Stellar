package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.content.Intent;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    // private Spinner spinnerCompanyName;
    private AutoCompleteTextView actvCompanyName;
    private EditText etCompanyEmail, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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
            //role 1 for letting agent view
            int role = 1;

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
            signUp(email, password,  company, String.valueOf(role));

            });

        };

        private void signUp (String email, String password, String company, String role) {


            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();

                            // Save user info in Firestore
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("company", company);
                            userMap.put("email", email);
                            userMap.put("role", role);

                            db.collection("users").document(user.getUid())
                                    .set(userMap)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Signup Successful", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Error saving user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
