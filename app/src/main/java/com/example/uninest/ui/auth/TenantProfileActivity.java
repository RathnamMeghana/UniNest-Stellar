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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.utils.ContactUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import android.net.Uri;
import android.provider.Settings;
import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.uninest.utils.ImageUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;


public class TenantProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;


    private ImageView ivProfileImage; // The big image in the header
    private TextView tvUserName;
    private TextView tvUserEmail;
    private Uri imageUri;
    private String loadedProfileImageValue;

    // 1. Define the Image Picker Launcher
    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    Glide.with(this).load(uri).dontAnimate().circleCrop().into(ivProfileImage);
                    uploadImageToFirebase();
                }
            }
    );

    private final ActivityResultLauncher<Intent> editNameLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> bindHeaderInfo()
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
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);

        initUI();
        loadExistingProfileImage();
    }
    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        if (bottomNav != null && bottomNav.getSelectedItemId() != R.id.nav_profile) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
        }
    }

    private void initUI() {
        bindHeaderInfo();

        // Setup Rows (Setting Labels and Icons)
        setupRow(R.id.rowEditName, "Change Account Name", R.drawable.ic_user);
        setupRow(R.id.rowEditPassword, "Change Password", R.drawable.ic_lock);
        setupRow(R.id.rowEditImage, "Change Profile Image", R.drawable.ic_camera);
        setupRow(R.id.rowNotifications, "Notification Settings", R.drawable.ic_notifications_outline);
        setupRow(R.id.rowAbout, "About Us", R.drawable.ic_info);
        setupRow(R.id.rowFaq, "FAQs", R.drawable.ic_help);
        setupRow(R.id.rowPrivacy, "Privacy Policy", R.drawable.ic_shield);
        setupRow(R.id.rowSupport, "Contact Support", R.drawable.ic_help);
        setupRow(R.id.rowDarkMode, "Dark Mode", R.drawable.ic_dark_mode);

        // Click Listeners
        // 2. Set listener for the Change Image row
        findViewById(R.id.rowEditImage).setOnClickListener(v -> mGetContent.launch("image/*"));
        findViewById(R.id.rowEditName).setOnClickListener(v ->
                editNameLauncher.launch(new Intent(this, ChangeAccountNameActivity.class)));

        findViewById(R.id.rowEditPassword).setOnClickListener(v ->
                startActivity(new Intent(this, ChangePasswordActivity.class)));
        findViewById(R.id.rowNotifications).setOnClickListener(v -> openNotificationSettings());

        findViewById(R.id.rowAbout).setOnClickListener(v ->
                startActivity(new Intent(this, AboutUsActivity.class)));

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

    private void bindHeaderInfo() {
        String fullName = sessionManager.getUserFullName();
        String email = sessionManager.getUserEmail();

        if (tvUserName != null && !String.valueOf(tvUserName.getText()).equals(fullName)) {
            tvUserName.setText(fullName);
        }

        if (tvUserEmail != null && !String.valueOf(tvUserEmail.getText()).equals(email)) {
            tvUserEmail.setText(email);
        }
    }

    private void loadExistingProfileImage() {
        String cachedImage = sessionManager.getUserImage();
        ImageUtils.loadProfileImageImmediate(ivProfileImage, cachedImage);
        loadedProfileImageValue = ImageUtils.normalizeImageSource(cachedImage);
        if (loadedProfileImageValue != null) {
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String imageStr = documentSnapshot.getString("profileImageUrl");
                if (imageStr != null && !imageStr.isEmpty()) {
                    sessionManager.saveTenantSession(
                            uid,
                            sessionManager.getUserEmail(),
                            sessionManager.getUserRole(),
                            sessionManager.fetchHouseCode(),
                            sessionManager.getUserFullName(),
                            imageStr
                    );
                    applyProfileImage(imageStr);
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
                        loadedProfileImageValue = ImageUtils.normalizeImageSource(base64Image);
                        Toast.makeText(this, "Profile Image Updated", Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Log.e("PROFILE_IMAGE", "Failed to encode image", e);
            Toast.makeText(this, "Encoding failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyProfileImage(String imageStr) {
        String normalizedImage = ImageUtils.normalizeImageSource(imageStr);
        if (normalizedImage == null) {
            return;
        }

        if (normalizedImage.equals(loadedProfileImageValue)) {
            return;
        }

        loadedProfileImageValue = normalizedImage;
        ImageUtils.loadProfileImageImmediate(ivProfileImage, imageStr);
    }

    private void openNotificationSettings() {
        Intent intent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", getPackageName(), null));
        }
        startActivity(intent);
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
