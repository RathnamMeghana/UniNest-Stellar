package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.google.android.material.button.MaterialButton;

public class LettingAgentLoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private android.widget.TextView tvSignUpLink, tvForgotPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    String BACKEND_URL = ApiClient.BASE_URL + "auth/firebase-login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letting_agent_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
        sessionManager.setFirstTimeSetupCompleted();

        // Initialize UI Elements
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSignUpLink = findViewById(R.id.tvSignUpLink);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Sign Up Link
        tvSignUpLink.setOnClickListener(v -> {
            startActivity(new Intent(LettingAgentLoginActivity.this, SignUpActivity.class));
            finish();
        });

        // FORGOT PASSWORD LOGIC (Matching Tenant Logic)
        tvForgotPassword.setOnClickListener(v -> {
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
                            Toast.makeText(LettingAgentLoginActivity.this, "Password reset email sent!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LettingAgentLoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnLogin.setOnClickListener(v -> login());
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
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            sendTokenToBackendAndSyncRoles(user);
                        }
                    } else {
                        resetLoginButton();
                        Toast.makeText(this, "Auth Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendTokenToBackendAndSyncRoles(FirebaseUser user) {
        user.getIdToken(true).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                resetLoginButton();
                return;
            }

            String idToken = task.getResult().getToken();
            OkHttpClient client = new OkHttpClient();
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            RequestBody body = RequestBody.create("{\"token\":\"" + idToken + "\"}", JSON);

            Request request = new Request.Builder()
                    .url(BACKEND_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        resetLoginButton();
                        Toast.makeText(LettingAgentLoginActivity.this, "Backend Unreachable", Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        user.getIdToken(true).addOnCompleteListener(refreshTask -> {
                            if (refreshTask.isSuccessful()) {
                                fetchUserProfile(user.getUid());
                            }
                        });
                    }
                    response.close();
                }
            });
        });
    }

    private void fetchUserProfile(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        resetLoginButton();
                        return;
                    }

                    String role = doc.getString("role");
                    String houseCode = doc.getString("houseCode");
                    String company = doc.getString("company");
                    String firstName = doc.getString("firstName");
                    String lastName = doc.getString("lastName");
                    String profileImg = doc.getString("profileImageUrl");

                    String fullName = ((firstName != null ? firstName : "") + " " +
                            (lastName != null ? lastName : "")).trim();

                    sessionManager.saveAgentSession(mAuth.getCurrentUser().getEmail(), role, company, fullName, profileImg);
                    runOnUiThread(() -> {
                        if ("1".equals(role)) {
                            startActivity(new Intent(this, LettingAgentBuildingsActivity.class));
                            finish();
                        } else if ("2".equals(role)) {
                            Intent intent = new Intent(this, ApartmentTenantsActivity.class);
                            intent.putExtra("EXTRA_APARTMENT_ID", houseCode);
                            startActivity(intent);
                            finish();
                        } else {
                            mAuth.signOut();
                            resetLoginButton();
                            Toast.makeText(this, "Unauthorized Role", Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .addOnFailureListener(e -> runOnUiThread(this::resetLoginButton));
    }

    private void resetLoginButton() {
        btnLogin.setEnabled(true);
        btnLogin.setText("Login");
    }
}