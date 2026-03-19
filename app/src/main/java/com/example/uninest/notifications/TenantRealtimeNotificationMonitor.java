package com.example.uninest.notifications;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.uninest.SessionManager;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Locale;

/**
 * Falls back to Firestore-driven local banners for direct letting-agent messages
 * while the tenant app is running, so message pop-ups still appear even if FCM
 * delivery is delayed or unavailable.
 */
public final class TenantRealtimeNotificationMonitor implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "RealtimeNotif";
    private static final String PREF_NAME = "realtime_notification_seen";
    private static volatile boolean appVisible;

    private final Application application;
    private ListenerRegistration notificationListener;
    private String listeningUserId;
    private boolean initialSnapshotHandled;
    private int startedActivityCount;
    private long listenerAttachedAtMillis;

    public TenantRealtimeNotificationMonitor(Application application) {
        this.application = application;
    }

    public void register() {
        application.registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        startedActivityCount++;
        appVisible = startedActivityCount > 0;
        syncListener();
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        syncListener();
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        startedActivityCount = Math.max(0, startedActivityCount - 1);
        appVisible = startedActivityCount > 0;
        syncListener();
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }

    private void syncListener() {
        SessionManager sessionManager = new SessionManager(application);
        String userId = sessionManager.getUserId();
        String role = sessionManager.getUserRole();
        boolean isAgent = "1".equals(role) || "LETTINGAGENT".equalsIgnoreCase(role);

        if (!appVisible || isAgent || isBlank(userId)) {
            clearListener();
            return;
        }

        if (notificationListener != null && userId.equals(listeningUserId)) {
            return;
        }

        clearListener();
        listeningUserId = userId;
        initialSnapshotHandled = false;
        listenerAttachedAtMillis = System.currentTimeMillis();

        notificationListener = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener(this::handleSnapshot);
    }

    private void handleSnapshot(@Nullable QuerySnapshot snapshot, @Nullable Exception error) {
        if (error != null) {
            Log.w(TAG, "Unable to observe tenant notifications", error);
            return;
        }

        if (snapshot == null || isBlank(listeningUserId)) {
            return;
        }

        if (!initialSnapshotHandled) {
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                String documentId = document.getId();
                if (!isBlank(documentId) && !wasSeen(listeningUserId, documentId)) {
                    markSeen(listeningUserId, documentId);
                }
            }
            if (!snapshot.getMetadata().isFromCache()) {
                initialSnapshotHandled = true;
            }
            return;
        }

        for (DocumentChange change : snapshot.getDocumentChanges()) {
            if (change.getType() != DocumentChange.Type.ADDED) {
                continue;
            }
            handleAddedNotification(listeningUserId, change.getDocument());
        }
    }

    private void handleAddedNotification(String userId, DocumentSnapshot document) {
        String documentId = document.getId();
        if (isBlank(documentId) || wasSeen(userId, documentId)) {
            return;
        }

        long createdAt = parseMillis(document.get("createdAt"));
        if (createdAt <= 0L || createdAt <= listenerAttachedAtMillis) {
            markSeen(userId, documentId);
            return;
        }

        markSeen(userId, documentId);

        if (!shouldShowInstantMessage(document)) {
            return;
        }

        String title = firstNonBlank(document.getString("title"), "UniNest");
        String body = firstNonBlank(document.getString("body"), "You have a new message");
        LocalNotificationHelper.showNotification(
                application,
                title,
                body,
                "MESSAGE",
                documentId,
                null
        );
    }

    private boolean shouldShowInstantMessage(DocumentSnapshot document) {
        String type = normalize(document.getString("type"));
        String targetScreen = normalize(document.getString("targetScreen"));
        String entityId = document.getString("entityId");

        if (!isBlank(entityId)) {
            return false;
        }

        if ("MESSAGE".equals(type)) {
            return true;
        }

        if (!isBlank(type)) {
            return false;
        }

        return isBlank(targetScreen)
                || "HOME".equals(targetScreen)
                || "MESSAGE".equals(targetScreen);
    }

    private void clearListener() {
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
        listeningUserId = null;
        initialSnapshotHandled = false;
    }

    public static boolean isAppVisible() {
        return appVisible;
    }

    private long parseMillis(Object raw) {
        if (raw instanceof Long) {
            return (Long) raw;
        }
        if (raw instanceof Double) {
            return ((Double) raw).longValue();
        }
        if (raw instanceof Timestamp) {
            return ((Timestamp) raw).toDate().getTime();
        }
        if (raw instanceof String) {
            try {
                return Long.parseLong((String) raw);
            } catch (Exception ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private boolean wasSeen(String userId, String documentId) {
        SharedPreferences preferences = preferences();
        return preferences.getBoolean(buildSeenKey(userId, documentId), false);
    }

    private void markSeen(String userId, String documentId) {
        preferences().edit().putBoolean(buildSeenKey(userId, documentId), true).apply();
    }

    private String buildSeenKey(String userId, String documentId) {
        return firstNonBlank(userId, "unknown") + ":" + firstNonBlank(documentId, "missing");
    }

    private SharedPreferences preferences() {
        return application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private String normalize(String value) {
        return value != null ? value.trim().toUpperCase(Locale.getDefault()) : "";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
