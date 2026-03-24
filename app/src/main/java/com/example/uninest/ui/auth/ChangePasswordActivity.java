package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.utils.NetworkErrorDialog;
import com.example.uninest.utils.NetworkUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etCurrentPassword, etNewPassword, etConfirmNewPassword;
    private MaterialButton btnUpdatePassword;
    private TextView tvOfflineHint;
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
        tvOfflineHint = findViewById(R.id.tvOfflineHint);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnUpdatePassword.setOnClickListener(v -> performPasswordChange());
        updateConnectivityUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateConnectivityUi();
    }

    private void updateConnectivityUi() {
        boolean hasConnection = NetworkUtils.hasConnection(this);
        btnUpdatePassword.setEnabled(hasConnection);
        btnUpdatePassword.setAlpha(hasConnection ? 1f : 0.6f);
        tvOfflineHint.setVisibility(hasConnection ? View.GONE : View.VISIBLE);
    }

    private void showOfflineActionState(String title, String body) {
        updateConnectivityUi();
        NetworkErrorDialog.show(this, title, body, this::performPasswordChange);
    }

    private void resetButtonState() {
        updateConnectivityUi();
        btnUpdatePassword.setText("Update Password");
    }

    private void performPasswordChange() {
        String currentPass = etCurrentPassword.getText().toString();
        String newPass = etNewPassword.getText().toString();
        String confirmPass = etConfirmNewPassword.getText().toString();

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

        if (!NetworkUtils.hasConnection(this)) {
            showOfflineActionState(
                    "Couldn't update your password",
                    "You'll need an internet connection to change your password."
            );
            return;
        }

        btnUpdatePassword.setEnabled(false);
        btnUpdatePassword.setAlpha(1f);
        btnUpdatePassword.setText("Verifying...");

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            resetButtonState();
            Toast.makeText(this, "Please sign in again to update your password.", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);

        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        NetworkErrorDialog.dismiss(this);
                        Toast.makeText(this, "Password Updated!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        resetButtonState();
                        if (!NetworkUtils.hasConnection(this)) {
                            showOfflineActionState(
                                    "Couldn't update your password",
                                    "You'll need an internet connection to change your password."
                            );
                        } else {
                            Toast.makeText(this, "We couldn't update your password right now.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                return;
            }

            resetButtonState();
            if (!NetworkUtils.hasConnection(this)) {
                showOfflineActionState(
                        "Couldn't verify your password",
                        "You'll need an internet connection to change your password."
                );
            } else {
                etCurrentPassword.setError("Incorrect current password");
                Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
