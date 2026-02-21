package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.ui.auth.LettingAgentBuildingsActivity;
import com.example.uninest.ui.auth.ApartmentTenantsActivity;
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
import io.sentry.Sentry;

public class LettingAgentLoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    // Use 10.0.2.2 for Emulator, or your Local IP for physical device
    // Match exactly what worked in your browser!
    private static final String BACKEND_URL = "http://192.168.1.89:8081/auth/firebase-login";
    //private static final String BACKEND_URL = "http://192.168.1.90:8080/auth/firebase-login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    // waiting for view to draw to better represent a captured error with a screenshot
    findViewById(android.R.id.content).getViewTreeObserver().addOnGlobalLayoutListener(() -> {
     // try {
   //     throw new Exception("This app uses Sentry! :)");
     // } catch (Exception e) {
      //  Sentry.captureException(e);
     // }
    });

        setContentView(R.layout.activity_letting_agent_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

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
                            // First, get the current token to tell the backend who we are
                            sendTokenToBackendAndSyncRoles(user);
                        }
                    } else {
                        resetLoginButton();
                        Toast.makeText(this, "Auth Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendTokenToBackendAndSyncRoles(FirebaseUser user) {
        user.getIdToken(false).addOnCompleteListener(task -> {
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
                        // The backend just confirmed: "Successfully set role LETTINGAGENT"

                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // FORCE REFRESH = true (This downloads the role you just set)
                            user.getIdToken(true).addOnCompleteListener(refreshTask -> {
                                if (refreshTask.isSuccessful()) {
                                    Log.d("AUTH", "New token with LETTINGAGENT role acquired!");

                                    // ONLY NOW should you navigate to the next screen
                                    fetchUserProfile(user.getUid());
                                }
                            });
                        }
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
                    sessionManager.saveAgentSession(mAuth.getCurrentUser().getEmail(), role);

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