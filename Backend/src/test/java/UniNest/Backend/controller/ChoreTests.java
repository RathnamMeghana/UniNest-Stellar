package UniNest.Backend.controller;

import UniNest.Backend.dto.ChoreRequests;
import UniNest.Backend.exception.ChoreServiceException;
import UniNest.Backend.service.ChoreService;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ChoreServiceTest {

    @InjectMocks
    private ChoreService choreService;

    @Mock private Firestore firestore;
    @Mock private CollectionReference collectionReference;
    @Mock private DocumentReference documentReference;
    @Mock private ApiFuture<QuerySnapshot> querySnapshotFuture;
    @Mock private QuerySnapshot querySnapshot;
    @Mock private QueryDocumentSnapshot documentSnapshot;

    private MockedStatic<FirestoreClient> mockedFirestoreClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Mocking the static FirestoreClient
        mockedFirestoreClient = mockStatic(FirestoreClient.class);
        mockedFirestoreClient.when(FirestoreClient::getFirestore).thenReturn(firestore);
    }

    @AfterEach
    void tearDown() {
        mockedFirestoreClient.close();
    }

    @Test
    void getAllChoreByApartment_Success() throws Exception {
        // Arrange
        String houseCode = "HOUSE123";
        when(firestore.collection("apartments")).thenReturn(collectionReference);
        when(collectionReference.document(houseCode)).thenReturn(documentReference);
        when(documentReference.collection("chores")).thenReturn(collectionReference);
        when(collectionReference.get()).thenReturn(querySnapshotFuture);
        when(querySnapshotFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.getDocuments()).thenReturn(Collections.singletonList(documentSnapshot));

        ChoreRequests chore = new ChoreRequests();
        when(documentSnapshot.toObject(ChoreRequests.class)).thenReturn(chore);
        when(documentSnapshot.getId()).thenReturn("choreId123");

        // Act
        List<ChoreRequests> result = choreService.getAllChoreByApartment(houseCode);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("choreId123", result.get(0).getId());
    }

    @Test
    void getAllChoreByApartment_ThrowsException_WhenHouseCodeEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            choreService.getAllChoreByApartment("");
        });
    }

    @Test
    void addChore_Success() throws Exception {
        // Arrange
        String houseCode = "HOUSE123";
        ChoreRequests chore = new ChoreRequests();
        chore.setTaskName("Wash Dishes");

        ApiFuture<DocumentReference> docRefFuture = mock(ApiFuture.class);

        when(firestore.collection("apartments")).thenReturn(collectionReference);
        when(collectionReference.document(houseCode)).thenReturn(documentReference);
        when(documentReference.collection("chores")).thenReturn(collectionReference);
        when(collectionReference.add(any(ChoreRequests.class))).thenReturn(docRefFuture);
        when(docRefFuture.get()).thenReturn(documentReference);
        when(documentReference.getId()).thenReturn("newChoreId");

        // Act
        ChoreRequests result = choreService.addChore(houseCode, chore);

        // Assert
        assertEquals("newChoreId", result.getId());
        verify(collectionReference, times(1)).add(chore);
    }

    @Test
    void addChoreWithAssignment_UserNotFound_ThrowsException() throws Exception {
        // Arrange
        String houseCode = "HOUSE123";
        String email = "test@user.com";
        ChoreRequests chore = new ChoreRequests();

        Query userQuery = mock(Query.class);
        ApiFuture<QuerySnapshot> userQueryFuture = mock(ApiFuture.class);

        when(firestore.collection("users")).thenReturn(collectionReference);
        when(collectionReference.whereEqualTo("email", email)).thenReturn(userQuery);
        when(userQuery.whereEqualTo("houseCode", houseCode)).thenReturn(userQuery);
        when(userQuery.get()).thenReturn(userQueryFuture);
        when(userQueryFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.isEmpty()).thenReturn(true); // User not found

        // Act & Assert
        assertThrows(ChoreServiceException.class, () -> {
            choreService.addChoreWithAssignment(houseCode, email, chore);
        });
    }
}