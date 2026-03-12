package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.RegisterTokenRequest;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Response;

public class LettingAgentLoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private android.widget.TextView tvSignUpLink, tvForgotPassword;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    private final String BACKEND_URL = ApiClient.BASE_URL + "auth/firebase-login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letting_agent_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUpLink = findViewById(R.id.tvSignUpLink);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
        sessionManager.setFirstTimeSetupCompleted();

        tvSignUpLink.setOnClickListener(v -> {
            startActivity(new Intent(LettingAgentLoginActivity.this, SignUpActivity.class));
            finish();
        });

        tvForgotPassword.setOnClickListener(v -> resetPassword());
        btnLogin.setOnClickListener(v -> login());
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

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(
                                this,
                                "Error: " + (task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Unknown error"),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Signing In...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        resetLoginButton();
                        Toast.makeText(
                                this,
                                "Auth Failed: " + (task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Unknown error"),
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) {
                        resetLoginButton();
                        Toast.makeText(this, "Login failed: user not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    sendTokenToBackendAndSyncRoles(user);
                });
    }

    private void sendTokenToBackendAndSyncRoles(FirebaseUser user) {
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                resetLoginButton();
                Toast.makeText(this, "Failed to get auth token", Toast.LENGTH_SHORT).show();
                return;
            }

            String idToken = task.getResult().getToken();
            OkHttpClient client = new OkHttpClient();
            MediaType json = MediaType.get("application/json; charset=utf-8");
            RequestBody body = RequestBody.create("{\"token\":\"" + idToken + "\"}", json);

            Request request = new Request.Builder()
                    .url(BACKEND_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        resetLoginButton();
                        Toast.makeText(
                                LettingAgentLoginActivity.this,
                                "Backend Unreachable",
                                Toast.LENGTH_LONG
                        ).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            runOnUiThread(() -> {
                                resetLoginButton();
                                Toast.makeText(
                                        LettingAgentLoginActivity.this,
                                        "Backend login sync failed",
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                            return;
                        }

                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser == null) {
                            runOnUiThread(() -> {
                                resetLoginButton();
                                Toast.makeText(
                                        LettingAgentLoginActivity.this,
                                        "Current user missing after login",
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                            return;
                        }

                        currentUser.getIdToken(true).addOnCompleteListener(refreshTask -> {
                            if (!refreshTask.isSuccessful()) {
                                runOnUiThread(() -> {
                                    resetLoginButton();
                                    Toast.makeText(
                                            LettingAgentLoginActivity.this,
                                            "Failed to refresh user token",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                });
                                return;
                            }

                            Log.d("AUTH", "New token with refreshed claims acquired");
                            fetchUserProfile(currentUser.getUid());
                        });
                    } finally {
                        response.close();
                    }
                }
            });
        });
    }

    private void fetchUserProfile(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        resetLoginButton();
                        Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String role = doc.getString("role");
                    String houseCode = doc.getString("houseCode");
                    String company = doc.getString("company");
                    String firstName = doc.getString("firstName");
                    String lastName = doc.getString("lastName");
                    String profileImg = doc.getString("profileImageUrl");

                    String fullName = ((firstName != null ? firstName : "") + " "
                            + (lastName != null ? lastName : "")).trim();

                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if (currentUser == null) {
                        resetLoginButton();
                        Toast.makeText(this, "User session expired", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    sessionManager.saveAgentSession(
                            currentUser.getEmail(),
                            role,
                            company,
                            fullName,
                            profileImg
                    );

                    FirebaseMessaging.getInstance().getToken()
                            .addOnSuccessListener(token -> {
                                Log.d("FCM", "Agent login token: " + token);

                                ApiClient.getNotificationApi()
                                        .registerToken(new RegisterTokenRequest(token))
                                        .enqueue(new retrofit2.Callback<Void>() {
                                            @Override
                                            public void onResponse(retrofit2.Call<Void> call, Response<Void> response) {
                                                Log.d("FCM", "Agent token registered: " + response.code());
                                            }

                                            @Override
                                            public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                                                Log.e("FCM", "Agent token registration failed", t);
                                            }
                                        });
                            })
                            .addOnFailureListener(e ->
                                    Log.e("FCM", "Failed to fetch agent FCM token", e)
                            );

                    runOnUiThread(() -> {
                        if ("1".equals(role)) {
                            Toast.makeText(this, "Agent Login successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(this, LettingAgentBuildingsActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else if ("2".equals(role)) {
                            Toast.makeText(this, "Tenant Login successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(this, ApartmentTenantsActivity.class);
                            intent.putExtra("EXTRA_APARTMENT_ID", houseCode);
                            intent.putExtra("EXTRA_HOUSE_CODE", houseCode);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            mAuth.signOut();
                            resetLoginButton();
                            Toast.makeText(this, "Unauthorized Role", Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    resetLoginButton();
                    Toast.makeText(
                            this,
                            "Failed to fetch profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void resetLoginButton() {
        btnLogin.setEnabled(true);
        btnLogin.setText("Login");
    }
}