package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


public class LettingAgentLoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letting_agent_login);

        // Hook up views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);


        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        btnLogin.setOnClickListener(v -> login());
        //btnLogin.setOnClickListener(v -> resetPassword());
    }
    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        // Validation
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in both email and password", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firebase Sign-in
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) return;

                        // Fetch user profile from Firestore
                        db.collection("users")
                                .document(user.getUid())
                                .get()
                                .addOnSuccessListener(this::handleUserDocument)
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
                                );

                    } else {
                        Toast.makeText(this, "Login failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleUserDocument(DocumentSnapshot doc) {

        if (!doc.exists()) {
            Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String role = doc.getString("role");
        String company = doc.getString("company");
        String apartmentId = doc.getString("houseCode");
        SessionManager session = new SessionManager(this);
        session.saveAgentSession(mAuth.getCurrentUser().getEmail(), role);


        if (role != null) {
            Log.d("LoginActivity", "User Role Retrieved: " + role);

            // Navigate based on the retrieved role string
            if ("2".equals(role)) {
                // Role 2: Tenent
                Toast.makeText(this, "Tenants Login successful!", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, ApartmentTenantsActivity.class);

                if (apartmentId != null && !apartmentId.isEmpty()) {
                    intent.putExtra("EXTRA_APARTMENT_ID", apartmentId);
                    intent.putExtra("EXTRA_HOUSE_CODE", apartmentId);
                    // Optionally pass name details if available on the document
                    // intent.putExtra("EXTRA_BUILDING_NAME", doc.getString("buildingName"));
                    // intent.putExtra("EXTRA_APARTMENT_NAME", doc.getString("apartmentName"));
                } else {
                    Toast.makeText(this, "Apartment ID missing in user profile.", Toast.LENGTH_LONG).show();
                    Log.e("LoginActivity", "User with role 2 is missing apartmentId.");
                    // You might choose to stop here or send them to an error/setup screen
                }

                startActivity(intent);
                finish(); // Close login screen
            } else if ("1".equals(role)) {
                // Role 1: letting agent
                Toast.makeText(this, "Agent Login successful!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LettingAgentBuildingsActivity.class);
                //Intent intent = new Intent(this, LettingAgentTicketsActivity.class);
                startActivity(intent);
                finish();
            }
            else {
                // Role found, but it's an unrecognized value
                Toast.makeText(this, "Unrecognized user role.", Toast.LENGTH_LONG).show();
                // Optionally sign the user out if their role is invalid
                mAuth.signOut();
            }
        } else {
            // Handle case where the 'role' field is missing or null
            Toast.makeText(this, "Login failed: User role not defined.", Toast.LENGTH_LONG).show();
            Log.w("LoginActivity", "The 'role' field was not found or was null in the document.");
            mAuth.signOut(); // Force sign out
        }


        }


    private void resetPassword() {

        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Please enter your email");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }


}