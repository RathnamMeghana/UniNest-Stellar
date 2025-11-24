package com.example.uninest.ui.auth;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;



public class DeleteUserActivity extends AppCompatActivity {
    private static final String TAG = "DeleteUserActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private Button btnConfirmDelete;
    private Button btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_user);

        // Initialize Firebase services
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnConfirmDelete = findViewById(R.id.btnConfirmDelete);
        btnCancel = findViewById(R.id.btnCancel);

        // Set up the listener for the deletion confirmation
        btnConfirmDelete.setOnClickListener(v -> {
            // Disable button to prevent multiple clicks
            btnConfirmDelete.setEnabled(false);
            Toast.makeText(this, "Processing deletion...", Toast.LENGTH_SHORT).show();
            // Call the local method to execute the deletion process
            deleteCurrentUserAccount();
        });

        // Set up the listener for cancellation
        btnCancel.setOnClickListener(v -> {
            finish(); // Close this confirmation screen and return to the previous activity
        });
    }

    /**
     * Executes the two-step user deletion process:
     * Deletes the user's Firestore data
     * Deletes the user's Auth account and redirects to the sign-up screen.
     */
    private void deleteCurrentUserAccount() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            handleFailure("No user is currently logged in.");
            return;
        }

        final String userId = user.getUid();
        Log.d(TAG, "Starting account deletion for user: " + userId);

        //  Delete Firestore Data
        db.collection("users").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore document deleted successfully for user: " + userId);
                    // Proceed to Step 2: Delete Auth User
                    deleteAuthUser(user, userId);
                })
                .addOnFailureListener(e -> {
                    handleFailure("Failed to delete user data: " + e.getMessage());
                });
    }

    /**
     * Deletes the Firebase Authentication user record and redirects.
     */
    private void deleteAuthUser(FirebaseUser user, String userId) {
        user.delete()
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "Auth user deleted successfully: " + userId);
                    handleSuccess();
                })
                .addOnFailureListener(e -> {
                    //  If this fails, the user needs to re-authenticate (log out and log back in)

                    handleFailure("Error deleting account. Please log in again and retry: " + e.getMessage());
                });
    }

    /**
     * Handles successful deletion by showing a Toast and redirecting the user.
     */
    private void handleSuccess() {
        Toast.makeText(this, "Account successfully closed and data erased.", Toast.LENGTH_LONG).show();
        Log.d(TAG, "Account deletion successful. Redirecting to signup.");

        // Redirect user to the main signup screen and clear activity history
        Intent intent = new Intent(this, TenantSignUpActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close this activity
    }

    /**
     * Handles deletion failure by showing a Toast and re-enabling the button.
     * @param errorMessage The error message to display.
     */
    private void handleFailure(String errorMessage) {
        Toast.makeText(this, "Deletion Failed: " + errorMessage, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Deletion failed: " + errorMessage);
        // Re-enable the button in case of failure so the user can retry
        btnConfirmDelete.setEnabled(true);
    }
}