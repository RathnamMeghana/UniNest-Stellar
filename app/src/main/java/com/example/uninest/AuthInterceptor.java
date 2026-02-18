package com.example.uninest;

import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private static final String TAG = "AuthInterceptor";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        // 1️⃣ Get Firebase token (try cached first)
        String token = getFirebaseToken(false);
        if (token == null) {
            // Force refresh if cached token is null
            token = getFirebaseToken(true);
        }

        if (token != null) {
            request = request.newBuilder()
                    .addHeader("Authorization", "Bearer " + token)
                    .build();
            Log.d(TAG, "Firebase Auth token attached: " + token.substring(0, 20) + "...");
        } else {
            Log.w(TAG, "No Firebase token available, sending request without auth");
        }

        Response response = chain.proceed(request);

        // 2️⃣ If 403, try refreshing token once and retry
        if (response.code() == 403) {
            Log.w(TAG, "Received 403, refreshing token and retrying...");
            response.close(); // Close previous response

            String newToken = getFirebaseToken(true); // force refresh
            if (newToken != null) {
                Request retryRequest = request.newBuilder()
                        .removeHeader("Authorization")
                        .addHeader("Authorization", "Bearer " + newToken)
                        .build();

                response = chain.proceed(retryRequest);
            }
        }

        return response;
    }

    private String getFirebaseToken(boolean forceRefresh) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return null;

        try {
            GetTokenResult result = Tasks.await(user.getIdToken(forceRefresh));
            return result.getToken();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Failed to get Firebase token", e);
            return null;
        }
    }
}
