package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TenantSignUpActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etHouseCode, etTenantEmail, etTenantPassword, etTenantConfirmPassword;
    private Button btnTenantSignUp;
    private android.widget.TextView tvLoginLink;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_sign_up);

        // Hook up views
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etHouseCode = findViewById(R.id.etHouseCode);
        etTenantEmail = findViewById(R.id.etTenantEmail);
        etTenantPassword = findViewById(R.id.etTenantPassword);
        etTenantConfirmPassword = findViewById(R.id.etTenantConfirmPassword);
        btnTenantSignUp = findViewById(R.id.btnTenantSignUp);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Mark first-time setup as done
        sessionManager.setFirstTimeSetupCompleted();

        tvLoginLink = findViewById(R.id.tvLoginLink);
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(TenantSignUpActivity.this, TenantLoginActivity.class));
            finish();
        });

        // Button click
        btnTenantSignUp.setOnClickListener(v -> {
            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String houseCode = etHouseCode.getText().toString().trim();
            String email = etTenantEmail.getText().toString().trim();
            String password = etTenantPassword.getText().toString();
            String confirm = etTenantConfirmPassword.getText().toString();
            int role = 2;

            if (firstName.isEmpty() || lastName.isEmpty() || houseCode.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
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

            btnTenantSignUp.setEnabled(false);
            btnTenantSignUp.setText("Creating Account...");

            validateHouseCode(houseCode, email, password, firstName, lastName, String.valueOf(role));
        });
    }

    private void signUp(String email, String password, String fName, String lName, String role, String houseCode, String apartmentId) {

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Save tenant info in Firestore
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("email", email);
                        userMap.put("firstName", fName);
                        userMap.put("lastName", lName);
                        userMap.put("role", role);
                        userMap.put("houseCode", houseCode);
                        userMap.put("apartmentId", apartmentId);
                        String profileImg = null;
                        String uid = user.getUid();

                        db.collection("users").document(user.getUid())
                                .set(userMap)
                                .addOnSuccessListener(aVoid -> {

                                    // Save session locally

                                    sessionManager.saveTenantSession(uid, email, role, houseCode, fName.trim(), profileImg);
                                    //sessionManager.saveTenantSession(email, houseCode);

                                    // ===== BACKEND SYNC SECTION =====
                                    user.getIdToken(true).addOnCompleteListener(tokenTask -> {
                                        if (!tokenTask.isSuccessful()) {
                                            Toast.makeText(this, "Failed to get token", Toast.LENGTH_SHORT).show();
                                            btnTenantSignUp.setEnabled(true);
                                            btnTenantSignUp.setText("Sign Up");
                                            return;
                                        }

                                        String idToken = tokenTask.getResult().getToken();

                                        OkHttpClient client = new OkHttpClient();
                                        MediaType JSON = MediaType.get("application/json; charset=utf-8");
                                        String jsonBody = "{\"token\":\"" + idToken + "\", \"houseCode\":\"" + houseCode + "\"}";
                                        RequestBody body = RequestBody.create(jsonBody, JSON);
                                        String backendUrl = ApiClient.BASE_URL + "auth/firebase-login";
                                        Request request = new Request.Builder()
                                                .url(backendUrl)
                                                .post(body)
                                                .addHeader("Authorization", "Bearer " + idToken)
                                                .build();

                                        client.newCall(request).enqueue(new Callback() {
                                            @Override
                                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                                runOnUiThread(() -> {
                                                    Toast.makeText(TenantSignUpActivity.this,
                                                            "Backend sync failed", Toast.LENGTH_SHORT).show();
                                                    btnTenantSignUp.setEnabled(true);
                                                    btnTenantSignUp.setText("Sign Up");
                                                });
                                            }

                                            @Override
                                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                                response.close();

                                                // Force refresh token to include backend claims
                                                user.getIdToken(true).addOnCompleteListener(refreshTask -> {
                                                    runOnUiThread(() -> {
                                                        if (refreshTask.isSuccessful()) {
                                                            Toast.makeText(TenantSignUpActivity.this,
                                                                    "Signup successful!", Toast.LENGTH_SHORT).show();
                                                            startActivity(new Intent(TenantSignUpActivity.this, TenantTicketsActivity.class));
                                                            finish();
                                                        } else {
                                                            Toast.makeText(TenantSignUpActivity.this,
                                                                    "Failed to refresh token", Toast.LENGTH_SHORT).show();
                                                            btnTenantSignUp.setEnabled(true);
                                                            btnTenantSignUp.setText("Sign Up");
                                                        }
                                                    });
                                                });
                                            }
                                        });
                                    });
                                    // ===== END BACKEND SYNC =====

                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Signup Failed: Could not save profile information.", Toast.LENGTH_LONG).show();
                                    btnTenantSignUp.setEnabled(true);
                                    btnTenantSignUp.setText("Sign Up");
                                });

                    } else {
                        btnTenantSignUp.setEnabled(true);
                        btnTenantSignUp.setText("Sign Up");

                        Exception exception = task.getException();
                        String errorMessage = "Authentication failed. Please try again.";

                        if (exception instanceof FirebaseAuthUserCollisionException) {
                            errorMessage = "That email address is already registered.";
                            etTenantEmail.setError(errorMessage);
                        } else if (exception instanceof FirebaseAuthWeakPasswordException) {
                            FirebaseAuthWeakPasswordException weakPasswordException = (FirebaseAuthWeakPasswordException) exception;
                            errorMessage = "Weak password: " + weakPasswordException.getReason();
                            etTenantPassword.setError(errorMessage);
                        } else {
                            errorMessage = "Signup failed: " + exception.getLocalizedMessage();
                        }

                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void validateHouseCode(String houseCode, String email, String password, String fName, String lName, String role) {
        db.collection("apartments")
                .whereEqualTo("code", houseCode)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Apartment exists, proceed with signup
                        String apartmentId = task.getResult().getDocuments().get(0).getId();
                        signUp(email, password, fName, lName, role, houseCode, apartmentId);
                    } else {
                        btnTenantSignUp.setEnabled(true);
                        btnTenantSignUp.setText("Sign Up");
                        Toast.makeText(TenantSignUpActivity.this,
                                "Invalid apartment code. Please check and try again.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    btnTenantSignUp.setEnabled(true);
                    btnTenantSignUp.setText("Sign Up");
                    Toast.makeText(TenantSignUpActivity.this, "Network Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
