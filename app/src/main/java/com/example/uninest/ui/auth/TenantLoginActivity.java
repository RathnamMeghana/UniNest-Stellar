package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.SessionManager;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;


public class TenantLoginActivity extends AppCompatActivity {

    private EditText etTenantEmail, etTenantPassword;
    private MaterialButton btnTenantLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_login);

        // Init Firebase & Session
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

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
                etTenantEmail.setError("Invalid email address");
                return;
            }

            performLogin(email, password);
        });
    }

    private void performLogin(String email, String password) {
        // Disable button
        btnTenantLogin.setEnabled(false);
        btnTenantLogin.setText("Signing In...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Login successful, now check role and get houseCode
                        fetchUserData(mAuth.getCurrentUser().getUid(), email);
                    } else {
                        btnTenantLogin.setEnabled(true);
                        btnTenantLogin.setText("Login");
                        Toast.makeText(this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchUserData(String uid, String email) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // This fetches the role from Firestore (e.g., "1" for Agent, "2" for Tenant)
                        String role = documentSnapshot.getString("role");

                        // Check if user is actually a Tenant (Assuming Tenant role is stored as "2" in Firestore)
                        if ("2".equals(role)) {
                            String houseCode = documentSnapshot.getString("houseCode");

                            // Retrieve Names
                            String fName = documentSnapshot.getString("firstName");
                            String lName = documentSnapshot.getString("lastName");
                            String fullName = (fName != null ? fName : "") + " " + (lName != null ? lName : "");

                            // Save FULL session data for Tenant
                            sessionManager.saveTenantSession(uid, email, role, houseCode, fullName.trim());

                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();

                            // Navigate to Tenant Home Screen
                            Intent intent = new Intent(TenantLoginActivity.this, TenantHomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            // User is an agent (role "1") or has another role trying to login as tenant
                            mAuth.signOut();
                            btnTenantLogin.setEnabled(true);
                            btnTenantLogin.setText("Login");
                            Toast.makeText(this, "Access Denied: Not a Tenant account", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        btnTenantLogin.setEnabled(true);
                        btnTenantLogin.setText("Login");
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    btnTenantLogin.setEnabled(true);
                    btnTenantLogin.setText("Login");
                    Toast.makeText(this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}