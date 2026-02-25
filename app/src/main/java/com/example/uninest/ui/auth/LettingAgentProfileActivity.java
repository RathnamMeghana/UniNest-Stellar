package com.example.uninest.ui.auth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class LettingAgentProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ImageView ivProfileImage;
    private Uri imageUri;

    // Image Picker Launcher
    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    ivProfileImage.setImageURI(uri);
                    uploadImageToFirebase();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letting_agent_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        ivProfileImage = findViewById(R.id.ivProfileImage);
        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvUserEmail = findViewById(R.id.tvUserEmail);

        // Fetch company and email from session
        String email = sessionManager.getUserEmail();
        tvUserEmail.setText(email != null && !email.isEmpty() ? email : "agent@uninest.com");

        String company = sessionManager.getCompany();
        if (company == null || company.isEmpty()) {
            company = "Letting Agent";
        }
        tvUserName.setText(company);

        // Set up Settings Rows using exact IDs from XML
        setupRow(R.id.rowEditPassword, "Change Password", R.drawable.ic_lock);
        setupRow(R.id.rowEditImage, "Change Profile Image", R.drawable.ic_camera);

        setupRow(R.id.rowAbout, "About UniNest", R.drawable.ic_info);
        setupRow(R.id.rowFaq, "FAQ", R.drawable.ic_help);
        setupRow(R.id.rowPrivacy, "Privacy Policy", R.drawable.ic_shield);

        // Load profile picture
        loadUserProfile();

        // Bind clicks
        findViewById(R.id.rowEditImage).setOnClickListener(v -> mGetContent.launch("image/*"));
        ivProfileImage.setOnClickListener(v -> mGetContent.launch("image/*"));
        
        findViewById(R.id.rowFaq).setOnClickListener(v -> {
            startActivity(new Intent(this, AgentFaqActivity.class));
        });

        findViewById(R.id.rowPrivacy).setOnClickListener(v -> {
            startActivity(new Intent(this, AgentPrivacyActivity.class));
        });

        findViewById(R.id.rowEditPassword).setOnClickListener(v -> {
            startActivity(new Intent(this, AgentChangePasswordActivity.class));
        });

        // Logout
        findViewById(R.id.btnLogout).setOnClickListener(v -> handleLogout());
        
        setupBottomNav(R.id.nav_profile);
    }

    private void loadUserProfile() {
        if (mAuth.getCurrentUser() == null) return;

        // Sync with Session FIRST to avoid wait
        String cachedImage = sessionManager.getUserImage();
        if (cachedImage != null && !cachedImage.isEmpty()) {
            if (cachedImage.startsWith("http")) {
                Glide.with(this).load(cachedImage).placeholder(R.drawable.ic_profile_tenant).into(ivProfileImage);
            } else {
                try {
                    byte[] decodedString = Base64.decode(cachedImage, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    ivProfileImage.setImageBitmap(decodedByte);
                } catch (Exception e) {}
            }
        }

        db.collection("users").document(mAuth.getCurrentUser().getUid()).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String imageStr = documentSnapshot.getString("profileImageUrl");
                if (imageStr != null && !imageStr.isEmpty()) {
                    if (imageStr.startsWith("http")) {
                        Glide.with(this).load(imageStr).into(ivProfileImage);
                    } else {
                        byte[] decodedString = Base64.decode(imageStr, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        ivProfileImage.setImageBitmap(decodedByte);
                    }
                }
            }
        });
    }

    private void uploadImageToFirebase() {
        if (imageUri == null) return;

        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 25, baos);
            byte[] bytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(bytes, Base64.DEFAULT);

            String uid = mAuth.getCurrentUser().getUid();
            db.collection("users").document(uid)
                    .update("profileImageUrl", base64Image)
                    .addOnSuccessListener(aVoid -> {
                        sessionManager.saveAgentSession(
                                sessionManager.getUserEmail(),
                                sessionManager.getUserRole(),
                                sessionManager.getCompany(),
                                sessionManager.getUserFullName(),
                                base64Image
                        );
                        Toast.makeText(this, "Profile Image Updated", Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Log.e("PROFILE_IMAGE", "Failed to encode image", e);
            Toast.makeText(this, "Encoding failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRow(int layoutId, String label, int iconRes) {
        View row = findViewById(layoutId);
        if (row == null) return;
        TextView tvLabel = row.findViewById(R.id.tvRowLabel);
        ImageView ivIcon = row.findViewById(R.id.ivRowIcon);
        if (tvLabel != null) tvLabel.setText(label);
        if (ivIcon != null) ivIcon.setImageResource(iconRes);
    }

    private void handleLogout() {
        mAuth.signOut();
        sessionManager.logout();
        Intent intent = new Intent(this, LettingAgentLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNav(int selectedId) {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        if (bottomNav == null) return;
        
        bottomNav.setSelectedItemId(selectedId);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == selectedId) return true;

            if (itemId == R.id.nav_tickets) {
                startActivity(new Intent(this, LettingAgentTicketsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_buildings) {
                startActivity(new Intent(this, LettingAgentBuildingsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }
}
