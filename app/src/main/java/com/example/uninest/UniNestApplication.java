package com.example.uninest;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.BuildConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthSettings;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class UniNestApplication extends Application {

    private static final String TAG = "UniNestApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Debug-only: Connect to emulators if using emulators
        if (BuildConfig.DEBUG) {
            setupEmulators();
        }

        Log.d(TAG, "Firebase initialized and emulators configured if debug.");
    }

    private void setupEmulators() {
        try {
            // Firestore emulator
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setHost("10.0.2.2:8080") // Emulator IP for Android emulator
                    .setSslEnabled(false)
                    .setPersistenceEnabled(false)
                    .build();
            firestore.setFirestoreSettings(settings);
            Log.d(TAG, "Connected Firestore to emulator");

            // Auth emulator
            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.useEmulator("10.0.2.2", 9099);
            Log.d(TAG, "Connected Auth to emulator");


        } catch (Exception e) {
            Log.e(TAG, "Failed to configure Firebase emulators", e);
        }
    }
}
