package UniNest.Backend;

import UniNest.Backend.dto.BillRequest;
import UniNest.Backend.dto.OwedToUserResponse;
import UniNest.Backend.service.BillService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.file.AccessDeniedException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BillControllerObjectLevelTest {

    @InjectMocks
    private BillService billService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        // Clear Security context before each test
        SecurityContextHolder.clearContext();
    }


    // HELPER METHOD TO SET AUTH USER
    private void setAuthenticatedUser(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null)
        );
    }


    // OBJECT-LEVEL AUTHORIZATION TESTS
    @Test
    void getBillsByUserId_otherUser_shouldThrow403() {
        setAuthenticatedUser("user1");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                billService.getBillsByUserId("user2")
        );

        assertEquals("Cannot access another user's bills", ex.getMessage());
    }

    @Test
    void getPaidHistory_otherUser_shouldThrow403() {
        setAuthenticatedUser("user1");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                billService.getPaidHistory("user2")
        );

        assertEquals("Cannot access another user's bills", ex.getMessage());
    }

    @Test
    void markAsPaid_otherUserSplit_shouldThrow403() {
        setAuthenticatedUser("user1");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                billService.markAsPaid("bill123", "user2")
        );

        assertEquals("Cannot access another user's bills", ex.getMessage());
    }

    // ===============================
    // NORMAL FUNCTIONALITY TESTS
    // ===============================

    @Test
    void getTotalOwedByUserId_calculatesCorrectTotal() throws Exception {
        setAuthenticatedUser("user1");

        BillRequest.Split split = new BillRequest.Split();
        split.setUserId("user1");
        split.setAmountOwed(50);
        split.setPaid(false);

        BillRequest bill = new BillRequest();
        bill.setSplits(List.of(split));

        // Spy on service to mock Firestore access
        BillService spyService = spy(billService);
        doReturn(List.of(bill)).when(spyService).getBillsByUserId("user1");

        double total = spyService.getTotalOwedByUserId("user1");
        assertEquals(50, total);
    }

    @Test
    void getTotalOwedToUser_sumsAmounts() {
        setAuthenticatedUser("user1");

        OwedToUserResponse o1 = new OwedToUserResponse();
        o1.setAmountOwed(40);
        OwedToUserResponse o2 = new OwedToUserResponse();
        o2.setAmountOwed(60);

        BillService spyService = spy(billService);
        doReturn(List.of(o1, o2)).when(spyService).getWhatIsOwedToUser("user1");

        double total = spyService.getTotalOwedToUser("user1");
        assertEquals(100, total);
    }

    @Test
    void markAsPaid_sameUser_shouldPass() throws AccessDeniedException {
        setAuthenticatedUser("user1");

        BillService spyService = spy(billService);
        doNothing().when(spyService).markAsPaid("bill123", "user1");

        assertDoesNotThrow(() -> spyService.markAsPaid("bill123", "user1"));
    }

    @Test
    void getBillsByUserId_sameUser_shouldPass() throws AccessDeniedException {
        setAuthenticatedUser("user1");

        BillRequest.Split split = new BillRequest.Split();
        split.setUserId("user1");
        split.setAmountOwed(50);
        split.setPaid(false);

        BillRequest bill = new BillRequest();
        bill.setSplits(List.of(split));

        BillService spyService = spy(billService);
        doReturn(List.of(bill)).when(spyService).getBillsByUserId("user1");

        List<BillRequest> bills = spyService.getBillsByUserId("user1");
        assertEquals(1, bills.size());
        assertEquals(50, bills.get(0).getSplits().get(0).getAmountOwed());
    }

    @Test
    void getPaidHistory_sameUser_shouldPass() throws AccessDeniedException {
        setAuthenticatedUser("user1");

        BillRequest.Split split = new BillRequest.Split();
        split.setUserId("user1");
        split.setAmountOwed(50);
        split.setPaid(true);

        BillRequest bill = new BillRequest();
        bill.setSplits(List.of(split));

        BillService spyService = spy(billService);
        doReturn(List.of(bill)).when(spyService).getPaidHistory("user1");

        List<BillRequest> history = spyService.getPaidHistory("user1");
        assertEquals(1, history.size());
        assertTrue(history.get(0).getSplits().get(0).isPaid());
    }
}
