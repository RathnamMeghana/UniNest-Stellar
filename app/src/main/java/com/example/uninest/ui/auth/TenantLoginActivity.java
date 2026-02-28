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
        // Force refresh to get latest ID token
        user.getIdToken(true).addOnCompleteListener(tokenTask -> {
            if (!tokenTask.isSuccessful()) {
                btnTenantLogin.setEnabled(true);
                btnTenantLogin.setText("Login");
                Toast.makeText(this, "Failed to get Firebase token", Toast.LENGTH_SHORT).show();
                return;
            }

            String idToken = tokenTask.getResult().getToken();

            // Call backend to register token
            OkHttpClient client = new OkHttpClient();
            MediaType JSON = MediaType.get("application/json; charset=utf-8");

            // Get houseCode from Firestore to include in backend request
            db.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");
                            if (!"2".equals(role)) {
                                // Not a tenant
                                mAuth.signOut();
                                btnTenantLogin.setEnabled(true);
                                btnTenantLogin.setText("Login");
                                Toast.makeText(this, "Access Denied: Not a Tenant account", Toast.LENGTH_LONG).show();
                                return;
                            }

                            String houseCode = documentSnapshot.getString("houseCode");
                            String fName = documentSnapshot.getString("firstName");
                            String lName = documentSnapshot.getString("lastName");
                            String email = documentSnapshot.getString("email");
                            String uid = documentSnapshot.getString("userId");
                            String fullName = (fName != null ? fName : "") + " " + (lName != null ? lName : "");
                            String profileImg = documentSnapshot.getString("profileImageUrl");

                            // Save FULL session data
                            sessionManager.saveTenantSession(uid, email, role, houseCode, fullName.trim(), profileImg);

                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();

                            // Navigate to Raise Ticket Screen
                            Intent intent = new Intent(TenantLoginActivity.this, TenantHomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                            // Save tenant session locally



                            sessionManager.saveTenantSession(uid, email, role, houseCode, fullName.trim(), profileImg);
                           // sessionManager.saveTenantSession(user.getUid(), user.getEmail(), role, houseCode, fullName.trim());

                            // JSON body for backend
                            String jsonBody = "{\"token\":\"" + idToken + "\", \"houseCode\":\"" + houseCode + "\"}";
                            RequestBody body = RequestBody.create(jsonBody, JSON);
                            String BACKEND_URL = ApiClient.BASE_URL + "auth/firebase-login";
                            Request request = new Request.Builder()
                                    .url(BACKEND_URL) // backend endpoint
                                    .post(body)
                                    .addHeader("Authorization", "Bearer " + idToken)
                                    .build();

                            client.newCall(request).enqueue(new Callback() {
                                @Override
                                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                    runOnUiThread(() -> {
                                        btnTenantLogin.setEnabled(true);
                                        btnTenantLogin.setText("Login");
                                        Toast.makeText(TenantLoginActivity.this, "Backend sync failed", Toast.LENGTH_SHORT).show();
                                    });
                                }

                                @Override
                                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                    response.close();
                                    runOnUiThread(() -> {
                                        Toast.makeText(TenantLoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                                        // Navigate to Tenant Home
                                        Intent intent = new Intent(TenantLoginActivity.this, TenantHomeActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    });
                                }
                            });
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
        });
    }
}
