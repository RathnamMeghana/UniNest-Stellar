package com.example.uninest.ui.auth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.utils.ContactUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;


public class TenantProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;


    private ImageView ivProfileImage; // The big image in the header
    private Uri imageUri;

    // 1. Define the Image Picker Launcher
    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    // Update UI immediately
                    ivProfileImage.setImageURI(uri);
                    // Upload to Firebase
                    uploadImageToFirebase();
                }
            }
    );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_profile);

        sessionManager = new SessionManager(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        ivProfileImage = findViewById(R.id.ivProfileImage);

        initUI();
    }
    @Override
    protected void onResume() {
        super.onResume();

        refreshUserData();
    }

    private void refreshUserData() {
        // 1. Update Name and Email from SessionManager
        TextView tvName = findViewById(R.id.tvUserName);
        TextView tvEmail = findViewById(R.id.tvUserEmail);

        tvName.setText(sessionManager.getUserFullName());
        tvEmail.setText(sessionManager.getUserEmail());

        // 2. Load the latest Image (from Firestore or Session)
        loadExistingProfileImage();
    }

    private void initUI() {
        // Populate Header Info
        TextView tvName = findViewById(R.id.tvUserName);
        TextView tvEmail = findViewById(R.id.tvUserEmail);
        tvName.setText(sessionManager.getUserFullName());
        tvEmail.setText(sessionManager.getUserEmail());

        // Setup Rows (Setting Labels and Icons)
        setupRow(R.id.rowEditName, "Change Account Name", R.drawable.ic_user);
        setupRow(R.id.rowEditPassword, "Change Password", R.drawable.ic_lock);
        setupRow(R.id.rowEditImage, "Change Profile Image", R.drawable.ic_camera);
        setupRow(R.id.rowAbout, "About Us", R.drawable.ic_info);
        setupRow(R.id.rowFaq, "FAQs", R.drawable.ic_help);
        setupRow(R.id.rowPrivacy, "Privacy Policy", R.drawable.ic_shield);
        setupRow(R.id.rowSupport, "Contact Support", R.drawable.ic_help);
        setupRow(R.id.rowDarkMode, "Dark Mode", R.drawable.ic_dark_mode);

        // Click Listeners
        // 2. Set listener for the Change Image row
        findViewById(R.id.rowEditImage).setOnClickListener(v -> mGetContent.launch("image/*"));
        findViewById(R.id.rowEditName).setOnClickListener(v ->
                startActivity(new Intent(this, ChangeAccountNameActivity.class)));

        findViewById(R.id.rowEditPassword).setOnClickListener(v ->
                startActivity(new Intent(this, ChangePasswordActivity.class)));

        findViewById(R.id.rowFaq).setOnClickListener(v ->
                startActivity(new Intent(this, FAQActivity.class)));

        findViewById(R.id.rowPrivacy).setOnClickListener(v ->
                startActivity(new Intent(this, PrivacyPolicyActivity.class)));

        findViewById(R.id.rowSupport).setOnClickListener(v -> ContactUtils.emailSupport(this));
        findViewById(R.id.rowEmergency).setOnClickListener(v -> ContactUtils.dialEmergency(this));
        findViewById(R.id.btnCallEmergency).setOnClickListener(v -> ContactUtils.dialEmergency(this));

        findViewById(R.id.btnLogout).setOnClickListener(v -> handleLogout());

        // Dark Mode Toggle Logic
// We find the switch INSIDE the rowDarkMode include
        View darkModeRow = findViewById(R.id.rowDarkMode);
        SwitchMaterial switchDark = darkModeRow.findViewById(R.id.itemSwitch);

// Check current theme to set switch state initially
        switchDark.setChecked(sessionManager.isDarkModeEnabled());

        switchDark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sessionManager.setDarkModeEnabled(isChecked);
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

// Click listener to toggle switch when row is clicked
        darkModeRow.setOnClickListener(v -> switchDark.setChecked(!switchDark.isChecked()));

        setupBottomNav();
    }

    private void loadExistingProfileImage() {
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String imageStr = documentSnapshot.getString("profileImageUrl");
                if (imageStr != null && !imageStr.isEmpty()) {

                    // If the string starts with "http", it's a URL. If not, it's Base64.
                    if (imageStr.startsWith("http")) {
                        Glide.with(this).load(imageStr).into(ivProfileImage);
                    } else {
                        // Decode Base64 string to image
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
            // 1. Convert Image to Base64 (Same as your AddBuilding logic)
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Compress to 25% to keep the string small enough for Firestore
            bitmap.compress(Bitmap.CompressFormat.JPEG, 25, baos);
            byte[] bytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(bytes, Base64.DEFAULT);

            // 2. Save the Base64 string directly to Firestore
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("users").document(uid)
                    .update("profileImageUrl", base64Image)
                    .addOnSuccessListener(aVoid -> {
                        // Update the session so the new image is available everywhere immediately
                        sessionManager.saveTenantSession(
                                uid,
                                sessionManager.getUserEmail(),
                                sessionManager.getUserRole(),
                                sessionManager.fetchHouseCode(),
                                sessionManager.getUserFullName(),
                                base64Image // Save the new Base64 string here
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
        TextView tvLabel = row.findViewById(R.id.tvRowLabel);
        ImageView ivIcon = row.findViewById(R.id.ivRowIcon);

        tvLabel.setText(label);
        ivIcon.setImageResource(iconRes);
    }

    private void handleLogout() {
        mAuth.signOut();
        sessionManager.logout();
        Intent intent = new Intent(this, TenantLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) return true;

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, TenantHomeActivity.class));
            } else if (id == R.id.nav_bills) {
                startActivity(new Intent(this, TenantBillsActivity.class));
            } else if (id == R.id.nav_calendar) {
                startActivity(new Intent(this, TenantCalendarActivity.class));
            } else if (id == R.id.nav_tickets) {
                startActivity(new Intent(this, TenantTicketsActivity.class));
            }
            overridePendingTransition(0, 0);
            finish();
            return true;
        });
    }
}
