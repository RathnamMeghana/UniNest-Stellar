package UniNest.Backend.service;

import UniNest.Backend.model.Ticket;
import UniNest.Backend.exception.TicketServiceException;
import UniNest.Backend.exception.TicketNotFoundException;
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
import org.springframework.http.HttpStatus;

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
    @Mock private DocumentReference documentReference; // Target Document
    @Mock private DocumentSnapshot documentSnapshot;   // Target Snapshot
    @Mock private ApiFuture<DocumentSnapshot> documentSnapshotFuture;
    @Mock private NotificationService notificationService;
    @Mock private ApiFuture<WriteResult> writeResultFuture;

    /**
     * Updated Helper to mock Direct Document Access (.document(id).get())
     */
    /**
     * Updated Helper to mock Direct Document Access (.document(id).get())
     */
    private void setupMockFirestoreDirect(Ticket ticketToReturn, boolean exists) throws Exception {
        when(firestore.collection(anyString())).thenReturn(collectionReference);
        when(collectionReference.document(anyString())).thenReturn(documentReference);
        when(documentReference.get()).thenReturn(documentSnapshotFuture);
        when(documentSnapshotFuture.get()).thenReturn(documentSnapshot);
        when(documentSnapshot.exists()).thenReturn(exists);

        if (exists && ticketToReturn != null) {
            when(documentSnapshot.toObject(Ticket.class)).thenReturn(ticketToReturn);

            // Mock the  update for Ticket
            when(documentReference.update(anyString(), any(), anyString(), any()))
                    .thenReturn(writeResultFuture);

            //  Mock the  update for Calendar
            when(documentReference.update(anyString(), any()))
                    .thenReturn(writeResultFuture);

            // Ensure .get() on the update doesn't crash
            when(writeResultFuture.get()).thenReturn(mock(WriteResult.class));
        }
    }

    @Test
    @DisplayName("Error: confirmVisit throws 404 if ticket ID is missing")
    void confirmVisit_NotFound_ThrowsException() throws Exception {
        try (MockedStatic<FirestoreClient> mockedClient = mockStatic(FirestoreClient.class)) {
            mockedClient.when(FirestoreClient::getFirestore).thenReturn(firestore);

            setupMockFirestoreDirect(null, false);

            // This will now catch the TicketNotFoundException properly
            assertThrows(TicketNotFoundException.class, () -> {
                ticketService.confirmVisitResolution("MISSING_ID", "user-123");
            });
        }
    }

    @Test
    @DisplayName("Logic: confirmVisit fails if the Tenant does not own the ticket")
    void confirmVisit_WrongOwner_ThrowsException() throws Exception {
        try (MockedStatic<FirestoreClient> mockedClient = mockStatic(FirestoreClient.class)) {
            mockedClient.when(FirestoreClient::getFirestore).thenReturn(firestore);

            Ticket mockTicket = new Ticket();
            mockTicket.setUserId("real-owner-123");

            // Setup: Ticket exists but user is different
            setupMockFirestoreDirect(mockTicket, true);

            // Execute as "hacker-456"
            TicketServiceException ex = assertThrows(TicketServiceException.class, () -> {
                ticketService.confirmVisitResolution("T123", "hacker-456");
            });

            assertTrue(ex.getMessage().contains("Unauthorized"));
            verify(documentReference, never()).update(anyString(), any(), anyString(), any());
        }
    }

    @Test
    @DisplayName("Handshake: confirmVisit successfully updates status and notifies agent")
    void confirmVisit_Success_TriggersNotification() throws Exception {
        try (MockedStatic<FirestoreClient> mockedClient = mockStatic(FirestoreClient.class)) {
            mockedClient.when(FirestoreClient::getFirestore).thenReturn(firestore);
            Ticket mockTicket = new Ticket();
            mockTicket.setId("T123");
            mockTicket.setUserId("tenant-123");
            mockTicket.setUserName("Aoife");
            mockTicket.setLandlordId("agent-456");
            mockTicket.setCategory("Leaks");
            setupMockFirestoreDirect(mockTicket, true);

            // Execute
            ticketService.confirmVisitResolution("T123", "tenant-123");

            verify(documentReference).update(eq("status"), eq("RESOLVED"), eq("updatedAt"), any());

            verify(notificationService).sendToUsers(
                    anyString(),
                    contains("Leaks"),
                    eq(List.of("agent-456")),
                    eq("AGENT_TICKETS"),
                    eq("T123")
            );
        }
    }

}