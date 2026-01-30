package com.example.uninest;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

public class UniNestApplication extends Application {

    private static final String TAG = "UniNestApp";

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);

        // Install Debug App Check provider
        FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                );

        Log.d(TAG, " Debug App Check provider installed");

        FirebaseApp.initializeApp(this);

        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
        );

    }
}
