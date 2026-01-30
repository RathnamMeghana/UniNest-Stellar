package com.example.uninest;

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
    @Override
    public Response intercept(Chain chain) throws IOException {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            try {
                // GetTokenResult(false) uses cache if not expired
                // GetTokenResult(true) forces refresh from server
                // We use Tasks.await to wait for the token synchronously here
                GetTokenResult tokenResult = Tasks.await(user.getIdToken(false));
                String token = tokenResult.getToken();

                Request newRequest = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer " + token)
                        .build();
                return chain.proceed(newRequest);

            } catch (ExecutionException | InterruptedException e) {
                return chain.proceed(chain.request());
            }
        }

        return chain.proceed(chain.request());
    }
}
