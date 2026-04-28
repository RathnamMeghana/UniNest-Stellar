package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.utils.NetworkErrorDialog;
import com.example.uninest.utils.NetworkUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ChangeAccountNameActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName;
    private MaterialButton btnSaveName;
    private TextView tvOfflineHint;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_account_name);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        btnSaveName = findViewById(R.id.btnSaveName);
        tvOfflineHint = findViewById(R.id.tvOfflineHint);

        prefillCachedName();
        loadCurrentName();
        updateConnectivityUi();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSaveName.setOnClickListener(v -> saveNameChanges());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateConnectivityUi();
    }

    private void prefillCachedName() {
        String fullName = sessionManager.getUserFullName();
        if (fullName == null || fullName.trim().isEmpty()) {
            return;
        }

        String[] parts = fullName.trim().split("\\s+", 2);
        etFirstName.setText(parts[0]);
        etLastName.setText(parts.length > 1 ? parts[1] : "");
    }

    private void loadCurrentName() {
        if (mAuth.getCurrentUser() == null || !NetworkUtils.hasConnection(this)) {
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        etFirstName.setText(documentSnapshot.getString("firstName"));
                        etLastName.setText(documentSnapshot.getString("lastName"));
                    }
                })
                .addOnFailureListener(e -> showOfflineActionState(
                        "Couldn't refresh your name",
                        "Showing your saved details for now. Check your connection and try again.",
                        this::loadCurrentName
                ));
    }

    private void updateConnectivityUi() {
        boolean hasConnection = NetworkUtils.hasConnection(this);
        btnSaveName.setEnabled(hasConnection);
        btnSaveName.setAlpha(hasConnection ? 1f : 0.6f);
        tvOfflineHint.setVisibility(hasConnection ? View.GONE : View.VISIBLE);
    }

    private void showOfflineActionState(String title, String body, Runnable retryAction) {
        updateConnectivityUi();
        NetworkErrorDialog.show(this, title, body, retryAction);
    }

    private void saveNameChanges() {
        String fName = etFirstName.getText().toString().trim();
        String lName = etLastName.getText().toString().trim();

        if (fName.isEmpty() || lName.isEmpty()) {
            Toast.makeText(this, "Please fill in both names", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!NetworkUtils.hasConnection(this)) {
            showOfflineActionState(
                    "Couldn't update your name",
                    "You'll need an internet connection to save name changes.",
                    this::saveNameChanges
            );
            return;
        }

        btnSaveName.setEnabled(false);
        btnSaveName.setText("Updating...");

        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", fName);
        updates.put("lastName", lName);

        db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    String fullName = fName + " " + lName;
                    String currentImage = sessionManager.getUserImage();

                    sessionManager.saveTenantSession(
                            uid,
                            sessionManager.getUserEmail(),
                            sessionManager.getUserRole(),
                            sessionManager.fetchHouseCode(),
                            fullName,
                            currentImage
                    );

                    NetworkErrorDialog.dismiss(this);
                    Toast.makeText(this, "Name updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSaveName.setText("Save Changes");
                    if (!NetworkUtils.hasConnection(this)) {
                        showOfflineActionState(
                                "Couldn't update your name",
                                "You'll need an internet connection to save name changes.",
                                this::saveNameChanges
                        );
                    } else {
                        updateConnectivityUi();
                        Toast.makeText(this, "We couldn't update your name right now.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
