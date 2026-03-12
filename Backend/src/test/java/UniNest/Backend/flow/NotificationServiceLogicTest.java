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
import org.mockito.junit.jupiter.MockitoSettings; // Add this
import org.mockito.quality.Strictness; // Add this

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // This fixes the UnnecessaryStubbingException
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
}