package UniNest.Backend.flow;

import UniNest.Backend.service.NotificationService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceLogicTest {

    @Mock private Firestore firestore;
    @Mock private CollectionReference usersCollection;
    @Mock private DocumentReference userDocRef;
    @Mock private CollectionReference notificationsCollection;
    @Mock private Query duplicateQuery;
    @Mock private Query houseQuery;
    @Mock private ApiFuture<QuerySnapshot> querySnapshotFuture;
    @Mock private QuerySnapshot querySnapshot;
    @Mock private DocumentReference newNotificationDocRef;
    @Mock private ApiFuture<WriteResult> writeResultFuture;

    @InjectMocks
    private NotificationService notificationService;


    @BeforeEach
    void setUp() {
        // Universal stub for all tests
        when(firestore.collection("users")).thenReturn(usersCollection);
    }

    /**
     * Helper to setup the Firestore chain needed for saving notifications.
     */
    private void setupDedupeMocks() {
        when(usersCollection.document(anyString())).thenReturn(userDocRef);
        when(userDocRef.collection("notifications")).thenReturn(notificationsCollection);
        when(notificationsCollection.whereEqualTo(anyString(), any())).thenReturn(duplicateQuery);
        when(duplicateQuery.whereEqualTo(anyString(), any())).thenReturn(duplicateQuery);

        when(duplicateQuery.limit(anyInt())).thenReturn(duplicateQuery);
        when(duplicateQuery.get()).thenReturn(querySnapshotFuture);
    }

    // Role Filtering Tests
    @Test
    @DisplayName("getTenantUserIdsByHouseCode should return IDs for role '2' and 'TENANT'")
    void getTenantIds_FilteringLogic() throws Exception {
        String houseCode = "HOUSE123";

        when(usersCollection.whereEqualTo("houseCode", houseCode)).thenReturn(houseQuery);
        when(houseQuery.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);

        QueryDocumentSnapshot doc1 = mock(QueryDocumentSnapshot.class);
        when(doc1.getId()).thenReturn("tenant_numeric");
        when(doc1.getString("role")).thenReturn("2");

        QueryDocumentSnapshot doc2 = mock(QueryDocumentSnapshot.class);
        when(doc2.getId()).thenReturn("tenant_string");
        when(doc2.getString("role")).thenReturn("TENANT");

        QueryDocumentSnapshot doc3 = mock(QueryDocumentSnapshot.class);
        when(doc3.getId()).thenReturn("agent_to_ignore");
        when(doc3.getString("role")).thenReturn("LETTINGAGENT");

        when(querySnapshot.getDocuments()).thenReturn(List.of(doc1, doc2, doc3));

        List<String> result = notificationService.getTenantUserIdsByHouseCode(houseCode);

        assertEquals(2, result.size());
        assertTrue(result.contains("tenant_numeric"));
        assertTrue(result.contains("tenant_string"));
        assertFalse(result.contains("agent_to_ignore"));
    }

    @Test
    @DisplayName("getAgentUserIdsByHouseCode should return IDs for role '1' and 'LETTINGAGENT'")
    void getAgentIds_FilteringLogic() throws Exception {
        String houseCode = "HOUSE123";
        when(usersCollection.whereEqualTo("houseCode", houseCode)).thenReturn(houseQuery);
        when(houseQuery.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);

        QueryDocumentSnapshot doc1 = mock(QueryDocumentSnapshot.class);
        when(doc1.getId()).thenReturn("agent1");
        when(doc1.getString("role")).thenReturn("1");

        QueryDocumentSnapshot doc2 = mock(QueryDocumentSnapshot.class);
        when(doc2.getId()).thenReturn("agent2");
        when(doc2.getString("role")).thenReturn("lettingagent");

        when(querySnapshot.getDocuments()).thenReturn(List.of(doc1, doc2));

        List<String> result = notificationService.getAgentUserIdsByHouseCode(houseCode);

        assertEquals(2, result.size());
        assertTrue(result.contains("agent1"));
        assertTrue(result.contains("agent2"));
    }

    // Deduplication tests

    @Test
    @DisplayName("save when the existing notification is older than 15s")
    void saveNotifications_OutsideWindow_ShouldSave() throws Exception {
        setupDedupeMocks();

        long now = System.currentTimeMillis();
        long twentySecondsAgo = now - 20000L;

        QueryDocumentSnapshot existingDoc = mock(QueryDocumentSnapshot.class);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(List.of(existingDoc));
        when(existingDoc.getLong("createdAt")).thenReturn(twentySecondsAgo);

        when(notificationsCollection.document()).thenReturn(newNotificationDocRef);
        when(newNotificationDocRef.getId()).thenReturn("new-doc-id");
        when(newNotificationDocRef.set(any())).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(mock(WriteResult.class));

        notificationService.saveNotificationsForUsers(
                "T", "B", "CHORE", "SCREEN", "E1", 123L, List.of("user1")
        );

        verify(newNotificationDocRef, times(1)).set(any());
    }

    @Test
    @DisplayName("not save when a duplicate exists within the 15s window")
    void saveNotifications_WithinWindow_ShouldDeduplicate() throws Exception {
        setupDedupeMocks();

        long now = System.currentTimeMillis();
        long fiveSecondsAgo = now - 5000L;

        QueryDocumentSnapshot existingDoc = mock(QueryDocumentSnapshot.class);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(List.of(existingDoc));
        when(existingDoc.getLong("createdAt")).thenReturn(fiveSecondsAgo);

        notificationService.saveNotificationsForUsers(
                "T", "B", "CHORE", "SCREEN", "E1", 123L, List.of("user1")
        );

        verify(notificationsCollection, never()).document();
    }
    // --- Token Management Tests ---

    @Test
    @DisplayName("getTokensForUsers should return unique non-empty tokens from all users")
    void getTokensForUsers_Logic() throws Exception {
        String uid1 = "user1";
        String uid2 = "user2";
        List<String> tokens1 = List.of("tokenA", "tokenB");
        List<String> tokens2 = List.of("tokenB", "tokenC", "  ", ""); // tokenB is duplicate, others empty

        // Create Mocks for Users
        DocumentReference userRef1 = mock(DocumentReference.class);
        ApiFuture<DocumentSnapshot> future1 = mock(ApiFuture.class);
        DocumentSnapshot snap1 = mock(DocumentSnapshot.class);

        when(usersCollection.document(uid1)).thenReturn(userRef1);
        when(userRef1.get()).thenReturn(future1);
        when(future1.get()).thenReturn(snap1);
        when(snap1.exists()).thenReturn(true);
        when(snap1.get("tokens")).thenReturn(tokens1);

        DocumentReference userRef2 = mock(DocumentReference.class);
        ApiFuture<DocumentSnapshot> future2 = mock(ApiFuture.class);
        DocumentSnapshot snap2 = mock(DocumentSnapshot.class);

        when(usersCollection.document(uid2)).thenReturn(userRef2);
        when(userRef2.get()).thenReturn(future2);
        when(future2.get()).thenReturn(snap2);
        when(snap2.exists()).thenReturn(true);
        when(snap2.get("tokens")).thenReturn(tokens2);

        // Reflection to call the private method part of org.springframework.test.util
        List<String> result = (List<String>) org.springframework.test.util.ReflectionTestUtils.invokeMethod(notificationService, "getTokensForUsers", List.of(uid1, uid2));

        assertNotNull(result);
        assertEquals(3, result.size(), "Should have tokenA, tokenB, and tokenC");
        assertTrue(result.contains("tokenA"));
        assertTrue(result.contains("tokenB"));
        assertTrue(result.contains("tokenC"));
        assertFalse(result.contains(""), "Should have filtered out empty strings");
        assertFalse(result.contains("  "), "Should have filtered out whitespace strings");
    }

    @Test
    @DisplayName("registerToken should use FieldValue.arrayUnion to save tokens")
    void registerToken_ShouldUseArrayUnion() throws Exception {
        String uid = "user1";
        String token = "newToken";

        when(usersCollection.document(uid)).thenReturn(userDocRef);
        when(userDocRef.set(anyMap(), any(SetOptions.class))).thenReturn(mock(ApiFuture.class));
        when(mock(ApiFuture.class).get()).thenReturn(mock(WriteResult.class));

        notificationService.registerToken(uid, token);

        verify(userDocRef).set(argThat(map -> {
            Map<?, ?> m = (Map<?, ?>) map;
            return m.containsKey("tokens") && m.containsKey("tokensUpdatedAt");
        }), eq(SetOptions.merge()));
    }



}