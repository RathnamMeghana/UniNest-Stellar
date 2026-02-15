package UniNest.Backend.security;

import UniNest.Backend.dto.CalendarEventDTO;
import UniNest.Backend.dto.ChoreRequests;
import UniNest.Backend.service.ChoreService;
import UniNest.Backend.service.CalendarService;

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

    private ChoreService choreService;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        choreService = new ChoreService(calendarService, firestore);

        // ---------------- CalendarService ----------------
        when(calendarService.create(any(), anyString()))
                .thenAnswer(invocation -> mock(CalendarEventDTO.Response.class));

        // ---------------- Users collection helper ----------------
        mockCollectionWithUser("users", "user123");

        // ---------------- Apartments/Chores collection helper ----------------
        mockChoresCollection("apartments", "HOUSE1");
    }

    // ---------------- HELPERS ----------------

    private void mockCollectionWithUser(String collectionName, String userId) throws Exception {
        CollectionReference usersCollection = mock(CollectionReference.class);
        Query userQuery = mock(Query.class);
        QuerySnapshot userSnapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot userDoc = mock(QueryDocumentSnapshot.class);
        ApiFuture<QuerySnapshot> userFuture = mock(ApiFuture.class);

        when(firestore.collection(collectionName)).thenReturn(usersCollection);
        when(usersCollection.whereEqualTo(anyString(), anyString())).thenReturn(userQuery);
        when(userQuery.whereEqualTo(anyString(), anyString())).thenReturn(userQuery);

        when(userQuery.get()).thenReturn(userFuture);
        when(userFuture.get()).thenReturn(userSnapshot);
        when(userSnapshot.isEmpty()).thenReturn(false);

        when(userSnapshot.getDocuments()).thenReturn(List.of(userDoc));
        when(userDoc.getId()).thenReturn(userId);
    }

    private void mockChoresCollection(String collectionName, String houseId) throws Exception {
        CollectionReference apartmentsCollection = mock(CollectionReference.class);
        CollectionReference choresCollection = mock(CollectionReference.class);
        DocumentReference choreDocRef = mock(DocumentReference.class);
        Query choreQuery = mock(Query.class);
        QuerySnapshot choreSnapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot choreDoc = mock(QueryDocumentSnapshot.class);
        ApiFuture<WriteResult> writeResultFuture = mock(ApiFuture.class);
        ApiFuture<QuerySnapshot> queryFuture = mock(ApiFuture.class);
        ApiFuture<WriteResult> updateFuture = mock(ApiFuture.class);

        when(firestore.collection(collectionName)).thenReturn(apartmentsCollection);
        DocumentReference apartmentDocRef = mock(DocumentReference.class);
        when(apartmentsCollection.document(houseId)).thenReturn(apartmentDocRef);
        when(apartmentDocRef.collection("chores")).thenReturn(choresCollection);

        // Chore doc creation and set
        when(choresCollection.document()).thenReturn(choreDocRef);
        when(choreDocRef.getId()).thenReturn("chore123"); // <-- FIX for addChore_shouldPass
        when(choreDocRef.set(any(ChoreRequests.class))).thenReturn(writeResultFuture);
        when(writeResultFuture.get()).thenReturn(mock(WriteResult.class));

        // Chore query for updates
        when(choresCollection.whereEqualTo(anyString(), anyString())).thenReturn(choreQuery);
        when(choreQuery.get()).thenReturn(queryFuture);
        when(queryFuture.get()).thenReturn(choreSnapshot);

        when(choreSnapshot.getDocuments()).thenReturn(List.of(choreDoc));
        when(choreDoc.getReference()).thenReturn(choreDocRef);
        when(choreDoc.toObject(ChoreRequests.class)).thenReturn(new ChoreRequests());

        // Chore update returns ApiFuture
        when(choreDocRef.update(anyString(), any())).thenReturn(updateFuture);
        when(updateFuture.get()).thenReturn(mock(WriteResult.class));

        // ChoresCollection get()
        ApiFuture<QuerySnapshot> choresFuture = mock(ApiFuture.class);
        when(choresCollection.get()).thenReturn(choresFuture);
        when(choresFuture.get()).thenReturn(choreSnapshot);
    }

    // ---------------- TESTS ----------------

    @Test
    void addChore_shouldPass() {
        ChoreRequests chore = new ChoreRequests();
        chore.setTaskName("Clean kitchen");

        ChoreRequests result = choreService.addChore("HOUSE1", chore, "creator123");

        assertNotNull(result.getId()); // choreDocRef.getId() mocked to "chore123"
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

        CollectionReference usersCollection = firestore.collection("users");
        Query userQuery = usersCollection.whereEqualTo("email", "notfound@example.com");
        when(userQuery.get()).thenReturn(emptyFuture);
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
                "HOUSE1", "Clean kitchen", "test@example.com"
        );

        assertNotNull(result);
        // Verify update is called with assignedTo
        verify(firestore.collection("apartments")
                .document("HOUSE1").collection("chores")
                .document()).update("assignedTo", "user123");
    }

    @Test
    void getAllChoreByApartment_shouldPass() throws Exception {
        List<ChoreRequests> list = choreService.getAllChoreByApartment("HOUSE1");
        assertFalse(list.isEmpty());
    }
}
