package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ChangeAccountNameActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName;
    private MaterialButton btnSaveName;
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

        // Pre-fill names from Firestore
        loadCurrentName();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnSaveName.setOnClickListener(v -> saveNameChanges());
    }

    private void loadCurrentName() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                etFirstName.setText(documentSnapshot.getString("firstName"));
                etLastName.setText(documentSnapshot.getString("lastName"));
            }
        });
    }

    private void saveNameChanges() {
        String fName = etFirstName.getText().toString().trim();
        String lName = etLastName.getText().toString().trim();

        if (fName.isEmpty() || lName.isEmpty()) {
            Toast.makeText(this, "Please fill in both names", Toast.LENGTH_SHORT).show();
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
                    // Update the local session so headers refresh
                    String fullName = fName + " " + lName;
                    String currentImage = sessionManager.getUserImage();

                    // Re-save session with new name (keep existing email, role, etc)
                    sessionManager.saveTenantSession(
                            uid,
                            sessionManager.getUserEmail(),
                            sessionManager.getUserRole(),
                            sessionManager.fetchHouseCode(),
                            fullName,
                            currentImage
                    );

                    Toast.makeText(this, "Name updated successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to Profile
                })
                .addOnFailureListener(e -> {
                    btnSaveName.setEnabled(true);
                    btnSaveName.setText("Save Changes");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}