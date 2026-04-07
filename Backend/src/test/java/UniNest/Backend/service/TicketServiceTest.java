package UniNest.Backend.service;

import UniNest.Backend.model.Ticket;
import UniNest.Backend.exception.TicketServiceException;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TicketServiceTest {

    @InjectMocks
    private TicketService ticketService;

    @Mock private Firestore firestore;
    @Mock private CollectionReference collectionReference;
    @Mock private Query query;
    @Mock private ApiFuture<QuerySnapshot> querySnapshotFuture;
    @Mock private QuerySnapshot querySnapshot;
    @Mock private QueryDocumentSnapshot documentSnapshot;
    @Mock private DocumentReference documentReference;
    @Mock private NotificationService notificationService;
    @Mock private ApiFuture<WriteResult> writeResultFuture;

    private void setupMockFirestoreChain(Ticket ticketToReturn) throws Exception {
        // mock the static client inside the test methods to ensure it's active
        when(firestore.collection("tickets")).thenReturn(collectionReference);
        when(collectionReference.whereEqualTo(eq("id"), anyString())).thenReturn(query);
        when(query.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);

        if (ticketToReturn != null) {
            when(querySnapshot.getDocuments()).thenReturn(Collections.singletonList(documentSnapshot));
            when(documentSnapshot.toObject(Ticket.class)).thenReturn(ticketToReturn);
            when(documentSnapshot.getReference()).thenReturn(documentReference);
            when(documentReference.update(anyString(), any(), anyString(), any())).thenReturn(writeResultFuture);
        } else {
            when(querySnapshot.getDocuments()).thenReturn(Collections.emptyList());
        }
    }

    @Test
    @DisplayName("Logic: confirmVisit fails if the Tenant does not own the ticket")
    void confirmVisit_WrongOwner_ThrowsException() throws Exception {
        try (MockedStatic<FirestoreClient> mockedClient = mockStatic(FirestoreClient.class)) {
            mockedClient.when(FirestoreClient::getFirestore).thenReturn(firestore);

            Ticket mockTicket = new Ticket();
            mockTicket.setUserId("user-creator-123"); // real owner

            setupMockFirestoreChain(mockTicket);

            // user-roommate-456 tries to mark it as done
            TicketServiceException ex = assertThrows(TicketServiceException.class, () -> {
                ticketService.confirmVisitResolution("T123", "user-roommate-456");
            });

            assertTrue(ex.getMessage().contains("Unauthorized"), "Should contain unauthorized message");
            verify(documentReference, never()).update(anyString(), any(), anyString(), any());
        }
    }

    @Test
    @DisplayName("confirmVisit successfully updates status and notifies agent")
    void confirmVisit_Success_TriggersNotification() throws Exception {
        try (MockedStatic<FirestoreClient> mockedClient = mockStatic(FirestoreClient.class)) {
            mockedClient.when(FirestoreClient::getFirestore).thenReturn(firestore);

            Ticket mockTicket = new Ticket();
            mockTicket.setId("T123");
            mockTicket.setUserId("tenant-123");
            mockTicket.setUserName("Dan");
            mockTicket.setLandlordId("agent-456");
            mockTicket.setCategory("Heating");

            setupMockFirestoreChain(mockTicket);

            ticketService.confirmVisitResolution("T123", "tenant-123");
            // Verify status changed to RESOLVED
            verify(documentReference).update(eq("status"), eq("RESOLVED"), eq("updatedAt"), any());

            // Verify the Agent Handshake Notification sent to Agent
            verify(notificationService).sendToUsers(
                    anyString(),
                    contains("Dan"),
                    eq(List.of("agent-456")),
                    eq("AGENT_TICKETS"),
                    eq("T123")
            );
        }
    }
}