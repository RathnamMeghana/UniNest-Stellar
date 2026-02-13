package UniNest.Backend.controller;

import UniNest.Backend.dto.CalendarEventDTO;
import UniNest.Backend.dto.ChoreRequests;
import UniNest.Backend.exception.ChoreServiceException;
import UniNest.Backend.service.CalendarService;
import UniNest.Backend.service.ChoreService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ChoreServiceTest {

    @InjectMocks
    private ChoreService choreService;

    @Mock private Firestore firestore;
    @Mock private CollectionReference collectionReference;
    @Mock private DocumentReference documentReference;
    @Mock private CalendarService calendarService;

    private MockedStatic<FirestoreClient> mockedFirestoreClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockedFirestoreClient = mockStatic(FirestoreClient.class);
        mockedFirestoreClient.when(FirestoreClient::getFirestore).thenReturn(firestore);
    }

    @AfterEach
    void tearDown() {
        mockedFirestoreClient.close();
    }

    @Test
    void addChore_Success() throws Exception {
        String houseCode = "HOUSE123";
        String userId = "user123";

        ChoreRequests chore = new ChoreRequests();
        chore.setTaskName("Wash Dishes");

        // Mock Firestore chain
        when(firestore.collection("apartments")).thenReturn(collectionReference);
        when(collectionReference.document(houseCode)).thenReturn(documentReference);
        when(documentReference.collection("chores")).thenReturn(collectionReference);
        when(collectionReference.document()).thenReturn(documentReference);
        when(documentReference.getId()).thenReturn("newChoreId");

        // Mock set() to return ApiFuture<WriteResult>
        ApiFuture<WriteResult> writeFuture = mock(ApiFuture.class);
        when(documentReference.set(any(ChoreRequests.class))).thenReturn(writeFuture);
        when(writeFuture.get()).thenReturn(mock(WriteResult.class));
        // Create a dummy response for CalendarService.create
        CalendarEventDTO.Response mockResponse = new CalendarEventDTO.Response();
        mockResponse.setTitle("Wash Dishes");
        mockResponse.setHouseCode("HOUSE123");
        mockResponse.setAssignedTo("user123");
        // Mock CalendarService

        when(calendarService.create(any(CalendarEventDTO.Create.class), anyString()))
                .thenReturn(mockResponse);

        // Act
        ChoreRequests result = choreService.addChore(houseCode, chore, userId);

        // Assert
        assertNotNull(result);
        assertEquals("newChoreId", result.getId());
        assertEquals(houseCode, result.getHouseCode());
        assertEquals(userId, result.getCreatedBy());

        verify(calendarService, times(1)).create(any(), eq(userId));
        verify(documentReference, times(1)).set(any(ChoreRequests.class));
    }
}
