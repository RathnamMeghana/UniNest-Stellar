package com.example.uninest;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start Python safely
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
        );
    }
}
        // Initialize Firebase with AppCheck debug provider (safe, no Google Sign-In)
       // FirebaseApp.initializeApp(this);
       // FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
       // firebaseAppCheck.installAppCheckProviderFactory(
        //        DebugAppCheckProviderFactory.getInstance()
        //);
   // }
//}
