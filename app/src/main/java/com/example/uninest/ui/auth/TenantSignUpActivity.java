package com.example.uninest.ui.auth;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.uninest.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public class TenantSignUpActivity extends AppCompatActivity {

    private EditText etHouseCode, etTenantEmail, etTenantPassword, etTenantConfirmPassword;
    private Button btnTenantSignUp;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

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

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        // Button click
        btnTenantSignUp.setOnClickListener(v -> {
            String houseCode = etHouseCode.getText().toString().trim();
            String email = etTenantEmail.getText().toString().trim();
            String password = etTenantPassword.getText().toString();
            String confirm = etTenantConfirmPassword.getText().toString();
            int role = 2;


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
            signUp(email, password, String.valueOf(role), houseCode);
        });
    }


    private void signUp(String email, String password, String role, String houseCode) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Save user info in Firestore
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("email", email);
                        userMap.put("role", role);
                        userMap.put("houseCode", houseCode);

                        db.collection("users").document(user.getUid())
                                .set(userMap)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Signup Successful! Welcome.", Toast.LENGTH_LONG).show();

                                })
                                .addOnFailureListener(e -> {
                                    // When Auth worked but Firestore failed
                                    Toast.makeText(this, "Signup Failed: Could not save profile information. Please contact support.", Toast.LENGTH_LONG).show();
                                });
                    } else {
                        // Handle Authentication Failure
                        Exception exception = task.getException();

                        String errorMessage = "Authentication failed. Please try again.";

                        if (exception != null) {
                            if (exception instanceof FirebaseAuthUserCollisionException) {
                                // Error for "The email address is already used"
                                errorMessage = "That email address is already registered.";
                                etTenantEmail.setError(errorMessage);
                            } else if (exception instanceof FirebaseAuthWeakPasswordException) {
                                // Error for "The password must be 6 characters long or more."
                                FirebaseAuthWeakPasswordException weakPasswordException = (FirebaseAuthWeakPasswordException) exception;
                                errorMessage = "Weak password: " + weakPasswordException.getReason();
                                etTenantPassword.setError(errorMessage);
                            } else {
                                // Catch other exceptions like network problems
                                errorMessage = "Signup failed: " + exception.getLocalizedMessage();
                            }
                        }

                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}

