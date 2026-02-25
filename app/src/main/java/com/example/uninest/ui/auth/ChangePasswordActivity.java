package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.uninest.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etCurrentPassword, etNewPassword, etConfirmNewPassword;
    private MaterialButton btnUpdatePassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        mAuth = FirebaseAuth.getInstance();

        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnUpdatePassword.setOnClickListener(v -> performPasswordChange());
    }

    private void performPasswordChange() {
        String currentPass = etCurrentPassword.getText().toString();
        String newPass = etNewPassword.getText().toString();
        String confirmPass = etConfirmNewPassword.getText().toString();

        // 1. Basic Validations
        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPass.length() < 6) {
            etNewPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            etConfirmNewPassword.setError("Passwords do not match");
            return;
        }

        btnUpdatePassword.setEnabled(false);
        btnUpdatePassword.setText("Verifying...");

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {

            // 2. Re-authenticate the user first (Security requirement)
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // 3. Update the password
                    user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(this, "Password Updated!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            btnUpdatePassword.setEnabled(true);
                            btnUpdatePassword.setText("Update Password");
                            Toast.makeText(this, "Update Failed: " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    btnUpdatePassword.setEnabled(true);
                    btnUpdatePassword.setText("Update Password");
                    etCurrentPassword.setError("Incorrect current password");
                    Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}