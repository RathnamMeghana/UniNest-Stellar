package UniNest.Backend.service;

import UniNest.Backend.model.AppNotification;
import com.google.cloud.firestore.*;
import com.google.firebase.messaging.*;
import com.google.firebase.messaging.SendResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class NotificationService {

    private final Firestore firestore;

    public NotificationService(Firestore firestore) {
        this.firestore = firestore;
    }

    // Store token on the user doc: users/{userId}.tokens = [ ... ]
    public void registerToken(String userId, String token) throws ExecutionException, InterruptedException {
        DocumentReference doc = firestore.collection("users").document(userId);

        Map<String, Object> update = new HashMap<>();
        update.put("tokens", FieldValue.arrayUnion(token));
        update.put("tokensUpdatedAt", Instant.now().toString());

        doc.set(update, SetOptions.merge()).get();
    }

    public void saveNotificationsForUsers(
            String title,
            String body,
            String type,
            String targetScreen,
            String entityId,
            Long eventTime,
            List<String> userIds
    ) throws ExecutionException, InterruptedException {

        long now = System.currentTimeMillis();
        long dedupeWindowMs = 15_000L;

        for (String uid : userIds) {
            CollectionReference notificationsRef = firestore.collection("users")
                    .document(uid)
                    .collection("notifications");

            Query duplicateQuery = notificationsRef
                    .whereEqualTo("title", title)
                    .whereEqualTo("body", body)
                    .whereEqualTo("type", type)
                    .whereEqualTo("targetScreen", targetScreen)
                    .whereEqualTo("entityId", entityId)
                    .whereEqualTo("eventTime", eventTime)
                    .limit(5);

            QuerySnapshot existing = duplicateQuery.get().get();

            boolean duplicateExists = false;
            for (DocumentSnapshot existingDoc : existing.getDocuments()) {
                Long createdAt = existingDoc.getLong("createdAt");
                if (createdAt != null && (now - createdAt) <= dedupeWindowMs) {
                    duplicateExists = true;
                    break;
                }
            }

            if (duplicateExists) {
                continue;
            }

            DocumentReference doc = notificationsRef.document();

            Map<String, Object> notification = new HashMap<>();
            notification.put("id", doc.getId());
            notification.put("userId", uid);
            notification.put("title", title);
            notification.put("body", body);
            notification.put("type", type);
            notification.put("targetScreen", targetScreen);
            notification.put("entityId", entityId);
            notification.put("createdAt", now);
            notification.put("eventTime", eventTime);
            notification.put("read", false);

            doc.set(notification).get();
        }
    }

    public void createChoreNotification(List<String> userIds, String title, String body, String entityId, Long eventTime)
            throws ExecutionException, InterruptedException {
        saveNotificationsForUsers(title, body, "CHORE", "CHORES", entityId, eventTime, userIds);
    }

    public void createRentNotification(List<String> userIds, String title, String body, String entityId, Long eventTime)
            throws ExecutionException, InterruptedException {
        saveNotificationsForUsers(title, body, "RENT", "BILLS", entityId, eventTime, userIds);
    }

    public void createMaintenanceNotification(List<String> userIds, String title, String body, String entityId, Long eventTime)
            throws ExecutionException, InterruptedException {
        saveNotificationsForUsers(title, body, "MAINTENANCE", "TICKETS", entityId, eventTime, userIds);
    }

    public void createCalendarNotification(List<String> userIds, String title, String body, String entityId, Long eventTime)
            throws ExecutionException, InterruptedException {
        saveNotificationsForUsers(title, body, "CALENDAR", "CALENDAR", entityId, eventTime, userIds);
    }

    public void createMessageNotification(List<String> userIds, String title, String body)
            throws ExecutionException, InterruptedException {
        saveNotificationsForUsers(title, body, "MESSAGE", "HOME", null, null, userIds);
    }

    public List<String> getTenantUserIdsByHouseCode(String houseCode)
            throws ExecutionException, InterruptedException {

        QuerySnapshot snapshot = firestore.collection("users")
                .whereEqualTo("houseCode", houseCode)
                .get()
                .get();

        List<String> tenantIds = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            String role = doc.getString("role");
            if ("2".equals(role) || "TENANT".equalsIgnoreCase(role)) {
                tenantIds.add(doc.getId());
            }
        }
        return tenantIds;
    }

    public List<String> getAgentUserIdsByHouseCode(String houseCode)
            throws ExecutionException, InterruptedException {

        QuerySnapshot snapshot = firestore.collection("users")
                .whereEqualTo("houseCode", houseCode)
                .get()
                .get();

        List<String> agentIds = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            String role = doc.getString("role");
            if ("1".equals(role) || "LETTINGAGENT".equalsIgnoreCase(role)) {
                agentIds.add(doc.getId());
            }
        }
        return agentIds;
    }

    // Fetch notifications for logged-in tenant
    public List<AppNotification> getNotificationsForUser(String userId)
            throws ExecutionException, InterruptedException {

        QuerySnapshot snapshot = firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .get();

        List<AppNotification> notifications = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            AppNotification notification = doc.toObject(AppNotification.class);
            if (notification != null) {
                notifications.add(notification);
            }
        }
        return notifications;
    }

    public int sendToUsers(
            String title,
            String body,
            List<String> userIds,
            String targetScreen,
            String entityId
    ) throws ExecutionException, InterruptedException {

        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }

        List<String> tokens = getTokensForUsers(userIds);
        if (tokens.isEmpty()) {
            return 0;
        }

        MulticastMessage msg = MulticastMessage.builder()
                .putData("title", title != null ? title : "UniNest")
                .putData("body", body != null ? body : "")
                .putData("targetScreen", targetScreen != null ? targetScreen : "HOME")
                .putData("entityId", entityId != null ? entityId : "")
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .addAllTokens(tokens)
                .build();

        try {
            BatchResponse resp = FirebaseMessaging.getInstance().sendEachForMulticast(msg);
            cleanupInvalidTokens(tokens, resp, userIds);
            return resp.getSuccessCount();
        } catch (FirebaseMessagingException e) {
            throw new RuntimeException("FCM send failed: " + e.getMessage(), e);
        }
    }

    private void cleanupInvalidTokens(List<String> sentTokens, BatchResponse response, List<String> userIds)
            throws ExecutionException, InterruptedException {

        if (sentTokens == null || sentTokens.isEmpty() || response == null) {
            return;
        }

        List<SendResponse> responses = response.getResponses();
        if (responses == null || responses.isEmpty()) {
            return;
        }

        Set<String> invalidTokens = new HashSet<>();

        for (int i = 0; i < responses.size() && i < sentTokens.size(); i++) {
            SendResponse sendResponse = responses.get(i);

            if (sendResponse.isSuccessful()) {
                continue;
            }

            FirebaseMessagingException exception = sendResponse.getException();
            if (exception == null) {
                continue;
            }

            String errorCode = exception.getMessagingErrorCode() != null
                    ? exception.getMessagingErrorCode().name()
                    : "";

            boolean shouldRemove =
                    "UNREGISTERED".equals(errorCode) ||
                            "INVALID_ARGUMENT".equals(errorCode);

            if (shouldRemove) {
                invalidTokens.add(sentTokens.get(i));
            }
        }

        if (invalidTokens.isEmpty()) {
            return;
        }

        removeTokensFromUsers(invalidTokens, userIds);
    }

    public void createAgentMaintenanceNotification(
            List<String> userIds,
            String title,
            String body,
            String entityId,
            Long eventTime
    ) throws ExecutionException, InterruptedException {
        saveNotificationsForUsers(title, body, "MAINTENANCE", "AGENT_TICKETS", entityId, eventTime, userIds);
    }

    private void removeTokensFromUsers(Set<String> invalidTokens, List<String> userIds)
            throws ExecutionException, InterruptedException {

        if (invalidTokens == null || invalidTokens.isEmpty() || userIds == null || userIds.isEmpty()) {
            return;
        }

        for (String uid : userIds) {
            DocumentReference userRef = firestore.collection("users").document(uid);
            DocumentSnapshot snap = userRef.get().get();

            if (!snap.exists()) {
                continue;
            }

            Object raw = snap.get("tokens");
            if (!(raw instanceof List<?>)) {
                continue;
            }

            List<String> updatedTokens = new ArrayList<>();
            for (Object tokenObj : (List<?>) raw) {
                if (tokenObj == null) {
                    continue;
                }

                String token = String.valueOf(tokenObj).trim();
                if (!token.isEmpty() && !invalidTokens.contains(token)) {
                    updatedTokens.add(token);
                }
            }

            userRef.update("tokens", updatedTokens).get();
        }
    }

    private List<String> getTokensForUsers(List<String> userIds)
            throws ExecutionException, InterruptedException {

        Set<String> uniqueTokens = new LinkedHashSet<>();

        for (String uid : userIds) {
            DocumentSnapshot snap = firestore.collection("users").document(uid).get().get();
            if (!snap.exists()) {
                continue;
            }

            Object raw = snap.get("tokens");
            if (raw instanceof List<?>) {
                for (Object t : (List<?>) raw) {
                    if (t != null) {
                        String token = String.valueOf(t).trim();
                        if (!token.isEmpty()) {
                            uniqueTokens.add(token);
                        }
                    }
                }
            }
        }

        return new ArrayList<>(uniqueTokens);
    }
}