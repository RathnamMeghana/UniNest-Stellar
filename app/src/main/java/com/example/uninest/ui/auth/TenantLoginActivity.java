package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TenantLoginActivity extends AppCompatActivity {

    private EditText etTenantEmail, etTenantPassword;
    private MaterialButton btnTenantLogin;
    private android.widget.TextView tvForgotPassword, tvSignUpLink;

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


        sessionManager.setFirstTimeSetupCompleted();

        etTenantEmail = findViewById(R.id.etTenantEmail);
        etTenantPassword = findViewById(R.id.etTenantPassword);
        btnTenantLogin = findViewById(R.id.btnTenantLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignUpLink = findViewById(R.id.tvSignUpLink);

        tvSignUpLink.setOnClickListener(v -> {
            startActivity(new Intent(TenantLoginActivity.this, TenantSignUpActivity.class));
            finish();
        });

        tvForgotPassword.setOnClickListener(v -> {
            String email = etTenantEmail.getText().toString().trim();

            if (email.isEmpty()) {
                etTenantEmail.setError("Please enter your email");
                etTenantEmail.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etTenantEmail.setError("Please enter a valid email");
                etTenantEmail.requestFocus();
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(TenantLoginActivity.this, "Password reset email sent!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(TenantLoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

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
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Automatically sync token with backend
                            syncTokenWithBackend(user);
                        }
                    } else {
                        btnTenantLogin.setEnabled(true);
                        btnTenantLogin.setText("Login");
                        Toast.makeText(this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void syncTokenWithBackend(FirebaseUser user) {
        // 1. Get the UID directly from the FirebaseUser object (Guaranteed not null)
        final String firebaseUid = user.getUid();

        user.getIdToken(true).addOnCompleteListener(tokenTask -> {
            if (!tokenTask.isSuccessful()) {
                resetUI();
                Toast.makeText(this, "Failed to get auth token", Toast.LENGTH_SHORT).show();
                return;
            }

            String idToken = tokenTask.getResult().getToken();

            // 2. Get user profile from Firestore
            db.collection("users").document(firebaseUid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");

                            if (!"2".equals(role)) {
                                mAuth.signOut();
                                resetUI();
                                Toast.makeText(this, "Access Denied: Not a Tenant account", Toast.LENGTH_LONG).show();
                                return;
                            }

                            String houseCode = documentSnapshot.getString("houseCode");
                            String fName = documentSnapshot.getString("firstName");
                            String lName = documentSnapshot.getString("lastName");
                            String email = documentSnapshot.getString("email");
                            String fullName = (fName != null ? fName : "") + " " + (lName != null ? lName : "");
                            String profileImg = documentSnapshot.getString("profileImageUrl");

                            // 3. Save session using the verified firebaseUid
                            sessionManager.saveTenantSession(firebaseUid, email, role, houseCode, fullName.trim(), profileImg);

                            // 4. Sync with Backend
                            OkHttpClient client = new OkHttpClient();
                            MediaType JSON = MediaType.get("application/json; charset=utf-8");
                            String jsonBody = "{\"token\":\"" + idToken + "\", \"houseCode\":\"" + houseCode + "\"}";
                            RequestBody body = RequestBody.create(jsonBody, JSON);

                            String BACKEND_URL = ApiClient.BASE_URL + "auth/firebase-login";
                            Request request = new Request.Builder()
                                    .url(BACKEND_URL)
                                    .post(body)
                                    .addHeader("Authorization", "Bearer " + idToken)
                                    .build();

                            client.newCall(request).enqueue(new Callback() {
                                @Override
                                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                    runOnUiThread(() -> {
                                        resetUI();
                                        Toast.makeText(TenantLoginActivity.this, "Backend sync failed", Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                    response.close();
                                    if (response.isSuccessful()) {
                                        runOnUiThread(() -> {
                                            Toast.makeText(TenantLoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                                            // 5. Navigate ONLY after successful sync
                                            Intent intent = new Intent(TenantLoginActivity.this, TenantHomeActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        });
                                    } else {
                                        runOnUiThread(() -> {
                                            resetUI();
                                            Toast.makeText(TenantLoginActivity.this, "Server error during sync", Toast.LENGTH_SHORT).show();
                                        });
                                    }
                                }
                            });
                        } else {
                            resetUI();
                            Toast.makeText(this, "User profile not found in database", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        resetUI();
                        Toast.makeText(this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void resetUI() {
        btnTenantLogin.setEnabled(true);
        btnTenantLogin.setText("Login");
    }
}
