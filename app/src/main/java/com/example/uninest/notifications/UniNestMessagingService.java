package com.example.uninest.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import com.example.uninest.SessionManager;
import com.example.uninest.ui.auth.LettingAgentBuildingsActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.uninest.R;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.RegisterTokenRequest;
import com.example.uninest.ui.auth.LettingAgentTicketsActivity;
import com.example.uninest.ui.auth.TenantHomeActivity;
import com.example.uninest.ui.auth.TenantTicketsActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UniNestMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCM";
    private static final String CHANNEL_ID = "uninest_notifications";
    private static final String CHANNEL_NAME = "UniNest Notifications";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        Log.d(TAG, "New FCM token: " + token);

        RegisterTokenRequest request = new RegisterTokenRequest(token);

        ApiClient.getNotificationApi()
                .registerToken(request)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        Log.d(TAG, "Token registered with backend: " + response.code());
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e(TAG, "Token registration failed", t);
                    }
                });
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);

        String title = "UniNest";
        String body = "You have a new notification";
        String targetScreen = "HOME";
        String entityId = "";

        Map<String, String> data = message.getData();

        if (data != null && !data.isEmpty()) {

            if (data.get("title") != null) {
                title = data.get("title");
            }

            if (data.get("body") != null) {
                body = data.get("body");
            }

            if (data.get("targetScreen") != null) {
                targetScreen = data.get("targetScreen");
            }

            if (data.get("entityId") != null) {
                entityId = data.get("entityId");
            }
        }

        Log.d(TAG, "Push received: " + title + " | " + body);

        showNotification(title, body, targetScreen, entityId);
    }

    private void showNotification(String title, String body, String targetScreen, String entityId) {

        createNotificationChannel();

        SessionManager sessionManager = new SessionManager(this);
        String role = sessionManager.getUserRole();
        boolean isAgent = "1".equals(role) || "LETTINGAGENT".equalsIgnoreCase(role);

        Intent intent;

        switch (targetScreen) {

            case "TICKETS":
                if (isAgent) {
                    intent = new Intent(this, LettingAgentTicketsActivity.class);
                } else {
                    intent = new Intent(this, TenantTicketsActivity.class);
                }
                intent.putExtra("highlight_ticket_id", entityId);
                break;

            case "HOME":
            default:
                if (isAgent) {
                    intent = new Intent(this, LettingAgentBuildingsActivity.class);
                } else {
                    intent = new Intent(this, TenantHomeActivity.class);
                }
                intent.putExtra("open_notifications", true);
                break;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        (int) System.currentTimeMillis(),
                        intent,
                        flags
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

                Log.w(TAG, "Notification permission not granted");
                return;
            }
        }

        NotificationManagerCompat.from(this)
                .notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = getSystemService(NotificationManager.class);

        if (manager == null) {
            return;
        }

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );

        channel.setDescription("Notifications from UniNest");

        manager.createNotificationChannel(channel);
    }
}