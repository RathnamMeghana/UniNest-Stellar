package UniNest.Backend.controller;

import UniNest.Backend.dto.BillRequest;
import UniNest.Backend.dto.OwedToUserResponse;
import UniNest.Backend.service.BillService;

import com.google.cloud.firestore.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BillServiceTest {

    @InjectMocks
    private BillService billService;

    @Mock
    private Firestore firestore;

    @Mock
    private CollectionReference collectionReference;

    @Mock
    private Query query;

    @Mock
    private QuerySnapshot querySnapshot;

    @Mock
    private QueryDocumentSnapshot documentSnapshot;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getTotalOwedByUserId_calculatesCorrectTotal() throws AccessDeniedException {
        BillRequest.Split split = new BillRequest.Split();
        split.setUserId("user1");
        split.setAmountOwed(50);
        split.setPaid(false);

        BillRequest bill = new BillRequest();
        bill.setSplits(List.of(split));

        BillService spyService = Mockito.spy(billService);
        Mockito.doReturn(List.of(bill))
                .when(spyService).getBillsByUserId("user1");

        double total = spyService.getTotalOwedByUserId("user1");

        assertEquals(50, total);
    }

    @Test
    void getTotalOwedToUser_sumsAmounts() {
        OwedToUserResponse o1 = new OwedToUserResponse();
        o1.setAmountOwed(40);

        OwedToUserResponse o2 = new OwedToUserResponse();
        o2.setAmountOwed(60);

        BillService spyService = Mockito.spy(billService);
        Mockito.doReturn(List.of(o1, o2))
                .when(spyService).getWhatIsOwedToUser("user1");

        double total = spyService.getTotalOwedToUser("user1");

        assertEquals(100, total);
    }
}
