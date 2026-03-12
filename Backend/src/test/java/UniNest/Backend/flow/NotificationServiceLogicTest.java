package UniNest.Backend.flow;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import UniNest.Backend.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationServiceLogicTest {

    @Mock private Firestore firestore;
    @Mock private CollectionReference usersCollection;
    @Mock private DocumentReference userDocRef;
    @Mock private CollectionReference notificationsCollection;
    @Mock private Query duplicateQuery;
    @Mock private ApiFuture<QuerySnapshot> querySnapshotFuture;
    @Mock private QuerySnapshot querySnapshot;
    @Mock private DocumentReference newNotificationDocRef;
    @Mock private ApiFuture<WriteResult> writeResultFuture;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        // Setup the common Firestore chain:
        // firestore.collection("users").document(uid).collection("notifications")
        when(firestore.collection("users")).thenReturn(usersCollection);
        when(usersCollection.document(anyString())).thenReturn(userDocRef);
        when(userDocRef.collection("notifications")).thenReturn(notificationsCollection);

        // Setup the Query chain: notificationsRef.whereEqualTo(...).limit(5).get().get()
        lenient().when(notificationsCollection.whereEqualTo(anyString(), any())).thenReturn(duplicateQuery);
        lenient().when(duplicateQuery.whereEqualTo(anyString(), any())).thenReturn(duplicateQuery);
        lenient().when(duplicateQuery.limit(anyInt())).thenReturn(duplicateQuery);
        lenient().when(duplicateQuery.get()).thenReturn(querySnapshotFuture);
    }


    @Test
    @DisplayName("Should PROCEED with saving when the existing notification is older than 15s")
    void saveNotifications_OutsideWindow_ShouldSave() throws Exception {
        // Arrange
        long now = System.currentTimeMillis();
        long twentySecondsAgo = now - 20000L; // Outside the 15s window

        QueryDocumentSnapshot existingDoc = mock(QueryDocumentSnapshot.class);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(List.of(existingDoc));
        when(existingDoc.getLong("createdAt")).thenReturn(twentySecondsAgo);

        // Mock the creation of a new document
        when(notificationsCollection.document()).thenReturn(newNotificationDocRef);
        when(newNotificationDocRef.getId()).thenReturn("new-doc-id");
        when(newNotificationDocRef.set(anyMap())).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(mock(WriteResult.class));

        // Act
        notificationService.saveNotificationsForUsers(
                "Test Title", "Test Body", "CHORE", "SCREEN", "E1", 123L, List.of("user1")
        );

        // Assert: document.set() SHOULD be called
        verify(newNotificationDocRef, times(1)).set(anyMap());
    }

    @Test
    @DisplayName("Should PROCEED with saving when no previous notifications exist")
    void saveNotifications_NoExisting_ShouldSave() throws Exception {
        // Arrange
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(List.of()); // Empty results

        // Mock the creation of a new document
        when(notificationsCollection.document()).thenReturn(newNotificationDocRef);
        when(newNotificationDocRef.getId()).thenReturn("new-doc-id");
        when(newNotificationDocRef.set(anyMap())).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(mock(WriteResult.class));

        // Act
        notificationService.saveNotificationsForUsers(
                "Test Title", "Test Body", "CHORE", "SCREEN", "E1", 123L, List.of("user1")
        );

        // Assert
        verify(newNotificationDocRef, times(1)).set(anyMap());
    }
}