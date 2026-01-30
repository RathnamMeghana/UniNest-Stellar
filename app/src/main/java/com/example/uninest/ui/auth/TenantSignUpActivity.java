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

    private EditText etFirstName, etLastName, etHouseCode,
            etTenantEmail, etTenantPassword, etTenantConfirmPassword;
    private Button btnTenantSignUp;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_sign_up);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etHouseCode = findViewById(R.id.etHouseCode);
        etTenantEmail = findViewById(R.id.etTenantEmail);
        etTenantPassword = findViewById(R.id.etTenantPassword);
        etTenantConfirmPassword = findViewById(R.id.etTenantConfirmPassword);
        btnTenantSignUp = findViewById(R.id.btnTenantSignUp);

        btnTenantSignUp.setOnClickListener(v -> attemptSignup());
    }

    private void attemptSignup() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String houseCode = etHouseCode.getText().toString().trim();
        String email = etTenantEmail.getText().toString().trim();
        String password = etTenantPassword.getText().toString();
        String confirm = etTenantConfirmPassword.getText().toString();
        String role = "2";

        if (firstName.isEmpty() || lastName.isEmpty() || houseCode.isEmpty()
                || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            toast("Please fill in all fields");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Please enter a valid email address");
            return;
        }

        if (!password.equals(confirm)) {
            toast("Passwords do not match");
            return;
        }

        setLoading(true);
        validateHouseCode(houseCode, email, password, firstName, lastName, role);
    }

    private void validateHouseCode(
            String houseCode,
            String email,
            String password,
            String firstName,
            String lastName,
            String role
    ) {
        db.collection("apartments")
                .whereEqualTo("code", houseCode)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        setLoading(false);
                        toast("Invalid apartment code");
                        return;
                    }

                    String apartmentId = snapshot.getDocuments().get(0).getId();
                    createAccount(email, password, firstName, lastName, role, houseCode, apartmentId);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    toast("Network error: " + e.getMessage());
                });
    }

    private void createAccount(
            String email,
            String password,
            String firstName,
            String lastName,
            String role,
            String houseCode,
            String apartmentId
    ) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        setLoading(false);
                        handleAuthError(task.getException());
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) {
                        setLoading(false);
                        toast("Signup succeeded but user is null");
                        return;
                    }

                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("email", email);
                    userMap.put("firstName", firstName);
                    userMap.put("lastName", lastName);
                    userMap.put("role", role);
                    userMap.put("houseCode", houseCode);
                    userMap.put("apartmentId", apartmentId);

                    db.collection("users").document(user.getUid())
                            .set(userMap)
                            .addOnSuccessListener(aVoid -> {
                                toast("Signup successful!");

                                // ALWAYS go to Tenant Login
                                startActivity(new Intent(this, TenantLoginActivity.class));
                                finish();

                                // Optional JWT (non-blocking)
                                user.getIdToken(true)
                                        .addOnSuccessListener(result ->
                                                sendTokenToBackend(result.getToken())
                                        );
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                toast("Failed to save profile");
                            });
                });
    }

    private void sendTokenToBackend(String idToken) {
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        RequestBody body = RequestBody.create(
                "{\"token\":\"" + idToken + "\"}",
                JSON
        );

        Request request = new Request.Builder()
                .url("http://192.168.1.70:8080/auth/firebase-login")
                .post(body)
                .addHeader("Authorization", "Bearer " + idToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                response.close();
            }
        });
    }

    private void handleAuthError(Exception e) {
        String message = "Signup failed";

        if (e instanceof FirebaseAuthUserCollisionException) {
            message = "Email already registered";
            etTenantEmail.setError(message);
        } else if (e instanceof FirebaseAuthWeakPasswordException) {
            message = "Weak password";
            etTenantPassword.setError(message);
        }

        toast(message);
    }

    private void setLoading(boolean loading) {
        btnTenantSignUp.setEnabled(!loading);
        btnTenantSignUp.setText(loading ? "Creating Account..." : "Sign Up");
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}

