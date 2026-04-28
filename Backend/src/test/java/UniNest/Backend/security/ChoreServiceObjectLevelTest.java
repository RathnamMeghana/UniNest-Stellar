package UniNest.Backend.security;

import UniNest.Backend.dto.CalendarEventDTO;
import UniNest.Backend.dto.ChoreRequests;
import UniNest.Backend.service.ChoreService;
import UniNest.Backend.service.CalendarService;
import UniNest.Backend.service.NotificationService;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChoreServiceObjectLevelTest {

    @Mock private Firestore firestore;
    @Mock private CalendarService calendarService;
    @Mock private NotificationService notificationService;

    private ChoreService choreService;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        choreService = new ChoreService(calendarService, firestore, notificationService);

        when(calendarService.create(any(), anyString()))
                .thenReturn(mock(CalendarEventDTO.Response.class));

        mockUsers();
        mockChores();
    }

    // ---------- USERS ----------

    private void mockUsers() throws Exception {
        CollectionReference usersCollection = mock(CollectionReference.class);
        Query userQuery = mock(Query.class);
        QuerySnapshot userSnapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot userDoc = mock(QueryDocumentSnapshot.class);
        DocumentReference userDocRef = mock(DocumentReference.class);
        DocumentSnapshot userDocSnap = mock(DocumentSnapshot.class);

        ApiFuture<QuerySnapshot> future = mock(ApiFuture.class);
        ApiFuture<DocumentSnapshot> docFuture = mock(ApiFuture.class);

        when(firestore.collection("users")).thenReturn(usersCollection);

        // Query for email + houseCode
        when(usersCollection.whereEqualTo(anyString(), any())).thenReturn(userQuery);
        when(userQuery.whereEqualTo(anyString(), any())).thenReturn(userQuery);
        when(userQuery.get()).thenReturn(future);
        when(future.get()).thenReturn(userSnapshot);

        when(userSnapshot.isEmpty()).thenReturn(false);
        when(userSnapshot.getDocuments()).thenReturn(List.of(userDoc));
        when(userDoc.getId()).thenReturn("user123");

        // for DocumentReference.get() in getAllChoreByApartment
        when(usersCollection.document("user123")).thenReturn(userDocRef);
        when(userDocRef.get()).thenReturn(docFuture);
        when(docFuture.get()).thenReturn(userDocSnap);
        when(userDocSnap.exists()).thenReturn(true);
        when(userDocSnap.getString("email")).thenReturn("test@example.com");
    }

    // ---------- CHORES ----------

    private void mockChores() throws Exception {
        CollectionReference apartments = mock(CollectionReference.class);
        CollectionReference chores = mock(CollectionReference.class);
        DocumentReference apartmentDoc = mock(DocumentReference.class);
        DocumentReference choreDoc = mock(DocumentReference.class);
        DocumentSnapshot choreSnap = mock(DocumentSnapshot.class);
        Query choreQuery = mock(Query.class);
        QuerySnapshot choreSnapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot choreDocSnap = mock(QueryDocumentSnapshot.class);

        ApiFuture<WriteResult> writeFuture = mock(ApiFuture.class);
        ApiFuture<WriteResult> updateFuture = mock(ApiFuture.class);
        ApiFuture<QuerySnapshot> queryFuture = mock(ApiFuture.class);
        ApiFuture<QuerySnapshot> collectionFuture = mock(ApiFuture.class);
        ApiFuture<DocumentSnapshot> docFuture = mock(ApiFuture.class);

        // collection/document setup
        when(firestore.collection("apartments")).thenReturn(apartments);
        when(apartments.document("HOUSE1")).thenReturn(apartmentDoc);
        when(apartmentDoc.collection("chores")).thenReturn(chores);

        // create chore
        when(chores.document()).thenReturn(choreDoc);
        when(choreDoc.getId()).thenReturn("chore123");
        doAnswer(inv -> {
            ChoreRequests c = inv.getArgument(0);
            c.setId("chore123");
            return writeFuture;
        }).when(choreDoc).set(any(ChoreRequests.class));
        when(writeFuture.get()).thenReturn(mock(WriteResult.class));

        // chore object
        ChoreRequests chore = new ChoreRequests();
        chore.setId("chore123");
        chore.setTaskName("Clean kitchen");
        chore.setAssignedTo("user123");

        // chore snapshot for getAll & queries
        when(choreDocSnap.toObject(ChoreRequests.class)).thenReturn(chore);
        when(choreDocSnap.getId()).thenReturn("chore123");
        when(choreDocSnap.getReference()).thenReturn(choreDoc);

        when(choreDoc.get()).thenReturn(docFuture);
        when(docFuture.get()).thenReturn(choreSnap);
        when(choreSnap.toObject(ChoreRequests.class)).thenReturn(chore);
        when(choreSnap.getId()).thenReturn("chore123");

        // chore query
        when(chores.whereEqualTo(anyString(), any())).thenReturn(choreQuery);
        when(choreQuery.whereEqualTo(anyString(), any())).thenReturn(choreQuery);
        when(choreQuery.get()).thenReturn(queryFuture);
        when(queryFuture.get()).thenReturn(choreSnapshot);
        when(choreSnapshot.isEmpty()).thenReturn(false);
        when(choreSnapshot.getDocuments()).thenReturn(List.of(choreDocSnap));

        // chores get()
        when(chores.get()).thenReturn(collectionFuture);
        when(collectionFuture.get()).thenReturn(choreSnapshot);

        // update
        when(choreDoc.update(anyString(), any())).thenReturn(updateFuture);
        when(updateFuture.get()).thenReturn(mock(WriteResult.class));
    }

    // ---------- TESTS ----------

    @Test
    void addChore_shouldPass() {
        ChoreRequests chore = new ChoreRequests();
        chore.setTaskName("Clean kitchen");
        ChoreRequests result = choreService.addChore("HOUSE1", chore, "creator123");
        assertNotNull(result.getId());
        verify(calendarService).create(any(), eq("creator123"));
    }

    @Test
    void addChoreWithAssignment_shouldPass() {
        ChoreRequests chore = new ChoreRequests();
        chore.setTaskName("Vacuum living room");
        ChoreRequests result = choreService.addChoreWithAssignment("HOUSE1", "test@example.com", chore);
        assertEquals("user123", result.getAssignedTo());
        verify(calendarService).create(any(), eq("user123"));
    }

    @Test
    void addChoreWithAssignment_userNotFound_shouldThrow() throws Exception {
        ApiFuture<QuerySnapshot> emptyFuture = mock(ApiFuture.class);
        QuerySnapshot emptySnapshot = mock(QuerySnapshot.class);
        CollectionReference users = firestore.collection("users");
        Query query = users.whereEqualTo("email", "notfound@example.com");
        when(query.get()).thenReturn(emptyFuture);
        when(emptyFuture.get()).thenReturn(emptySnapshot);
        when(emptySnapshot.isEmpty()).thenReturn(true);
        ChoreRequests chore = new ChoreRequests();
        assertThrows(RuntimeException.class,
                () -> choreService.addChoreWithAssignment("HOUSE1", "notfound@example.com", chore));
    }

    @Test
    void addChore_invalidHouseCode_shouldThrow() {
        ChoreRequests chore = new ChoreRequests();
        assertThrows(IllegalArgumentException.class,
                () -> choreService.addChore(null, chore, "creator123"));
    }

    @Test
    void updateAssignmentByTaskNameAndUserEmail_shouldPass() throws Exception {
        ChoreRequests result = choreService.updateAssignmentByTaskNameAndUserEmail(
                "HOUSE1", "Clean kitchen", "test@example.com");
        assertNotNull(result);
        assertEquals("chore123", result.getId());
    }

    @Test
    void getAllChoreByApartment_shouldPass() throws Exception {
        List<ChoreRequests> list = choreService.getAllChoreByApartment("HOUSE1");
        assertFalse(list.isEmpty());
        assertEquals("chore123", list.get(0).getId());
    }
}