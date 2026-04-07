package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.google.firebase.auth.FirebaseAuth;
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

public class SignUpActivity extends AppCompatActivity {

    private AutoCompleteTextView actvCompanyName;
    private EditText etCompanyEmail, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private android.widget.TextView tvLoginLink;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        actvCompanyName = findViewById(R.id.actvCompanyName);
        etCompanyEmail = findViewById(R.id.etCompanyEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        tvLoginLink.setOnClickListener(v -> {
            sessionManager.setFirstTimeSetupCompleted();
            startActivity(new Intent(SignUpActivity.this, LettingAgentLoginActivity.class));
            finish();
        });

        setupCompanySpinner();
        setupSignUpButton();
    }

    private void setupCompanySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.letting_companies,
                R.layout.item_calendar_spinner_selected
        );
        adapter.setDropDownViewResource(R.layout.item_calendar_spinner_dropdown);
        actvCompanyName.setAdapter(adapter);
        actvCompanyName.setThreshold(0);
        actvCompanyName.setInputType(InputType.TYPE_NULL);
        actvCompanyName.setOnClickListener(v -> actvCompanyName.showDropDown());
        actvCompanyName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                actvCompanyName.showDropDown();
            }
        });
    }

    private void setupSignUpButton() {
        btnSignUp.setOnClickListener(v -> {
            String company = actvCompanyName.getText().toString().trim();
            String email = etCompanyEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirm = etConfirmPassword.getText().toString();
            int role = 1;

            if (company.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isPasswordStrong(password)) {
                etPassword.setError("Password too weak! Needs 8+ chars, Uppercase, Lowercase, Number and Special Char.");
                etPassword.requestFocus();
                return;
            }

            if (!password.equals(confirm)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            signUp(email, password, company, String.valueOf(role));
        });
    }

    private void signUp(String email, String password, String company, String role) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(this, "Authentication failed: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) {
                        Toast.makeText(this, "Signup succeeded but user is null", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Save user info in Firestore
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("company", company);
                    userMap.put("email", email);
                    userMap.put("role", role);

                    db.collection("users").document(user.getUid())
                            .set(userMap)
                            .addOnSuccessListener(aVoid -> {
                                sessionManager.setFirstTimeSetupCompleted();
                                Toast.makeText(this, "Signup Successful!", Toast.LENGTH_SHORT).show();

                                // Navigate immediately to login screen
                                Intent intent = new Intent(SignUpActivity.this, LettingAgentLoginActivity.class);
                                startActivity(intent);
                                finish();

                                // send token to backend asynchronously
                                user.getIdToken(true).addOnCompleteListener(tokenTask -> {
                                    if (tokenTask.isSuccessful()) {
                                        String idToken = tokenTask.getResult().getToken();
                                        sendTokenToBackend(idToken);
                                    }
                                });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error saving user info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });
    }

    // asynchronous backend call, does not block navigation
    private void sendTokenToBackend(String idToken) {
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String jsonBody = "{\"token\":\"" + idToken + "\"}";
        RequestBody body = RequestBody.create(jsonBody, JSON);

       //String backendUrl = "http://192.168.1.70:8080/auth/firebase-login";
        String backendUrl = ApiClient.BASE_URL + "auth/firebase-login";
        Request request = new Request.Builder()
                .url(backendUrl)
                .post(body)
                .addHeader("Authorization", "Bearer " + idToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(SignUpActivity.this, "Backend auth failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                response.close(); // no navigation needed here
            }
        });
    }
    private boolean isPasswordStrong(String password) {
        // Regex:
        // (?=.*[0-9])       # at least 1 number
        // (?=.*[a-z])       # at least 1 lower case letter
        // (?=.*[A-Z])       # at least 1 upper case letter
        // (?=.*[@#$%^&+=!]) # at least 1 special character
        // (?=\S+$)          # no whitespace allowed
        // .{8,}             # at least eight in length
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
        return password.matches(passwordPattern);
    }
}
