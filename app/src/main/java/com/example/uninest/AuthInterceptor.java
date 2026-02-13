package com.example.uninest;

import android.util.Log; // Add this import
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.appcheck.AppCheckToken; // Add this import
import com.google.firebase.appcheck.FirebaseAppCheck; // Add this import
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        // Start with the original request
        Request.Builder requestBuilder = chain.request().newBuilder();

        // 1. Get the current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // 2. LOGIC FOR AUTH JWT (Identifying the User)
        if (user != null) {
            try {
                // Synchronously wait for the Auth Token
                GetTokenResult tokenResult = Tasks.await(user.getIdToken(false));
                String authToken = tokenResult.getToken();

                // Add the Authorization header to the builder
                requestBuilder.addHeader("Authorization", "Bearer " + authToken);
                Log.d("AuthInterceptor", "Auth JWT added to request");
            } catch (ExecutionException | InterruptedException e) {
                Log.e("AuthInterceptor", "Error getting Auth JWT", e);
            }
        }

        // 3. LOGIC FOR APP CHECK (Identifying the App)
        try {
            // Synchronously wait for the App Check Token
            AppCheckToken appCheckResult = Tasks.await(FirebaseAppCheck.getInstance().getAppCheckToken(false));
            String appCheckToken = appCheckResult.getToken();

            // Add the X-Firebase-AppCheck header to the builder
            requestBuilder.addHeader("X-Firebase-AppCheck", appCheckToken);
            Log.d("AuthInterceptor", "App Check Token added to request");
        } catch (Exception e) {
            // This is where your "App attestation failed" error will be caught
            Log.e("AuthInterceptor", "Error getting App Check Token: " + e.getMessage());
        }

        // 4. Build the final request and proceed
        return chain.proceed(requestBuilder.build());
    }
}