package UniNest.Backend.security;

import UniNest.Backend.dto.BillRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class BillRequestSanitizationTest {

    @Test
    void sanitizeBillRequest_RemovesUnsafeCharacters() {

        BillRequest bill = new BillRequest();
        bill.setHouseCode("<script>alert('xss')</script>");
        bill.setTitle("My <b>Bill</b>");
        bill.setCreatorId("user<script>");
        bill.setRoommateIds(new ArrayList<>(Arrays.asList("user1<script>", "user2<b>")));

        BillRequest.Split split1 = new BillRequest.Split();
        split1.setUserId("splitUser<script>");
        split1.setEmail("email@example.com<script>");
        split1.setBillTitle("Split <b>Title</b>");
        split1.setBillId("billId<script>");
        split1.setAmountOwed(100);

        bill.setSplits(new ArrayList<>(Arrays.asList(split1)));

        // Act: sanitize the bill
        bill.sanitize();
        assertEquals("alert('xss')", bill.getHouseCode());
        assertEquals("My Bill", bill.getTitle());
        assertEquals("user", bill.getCreatorId());

        assertEquals("user1", bill.getRoommateIds().get(0));
        assertEquals("user2", bill.getRoommateIds().get(1));

        BillRequest.Split sanitizedSplit = bill.getSplits().get(0);
        assertEquals("splitUser", sanitizedSplit.getUserId());
        assertEquals("email@example.com", sanitizedSplit.getEmail());
        assertEquals("Split Title", sanitizedSplit.getBillTitle());
        assertEquals("billId", sanitizedSplit.getBillId());

    }

    @Test
    void sanitizeBillRequest_DoesNotThrowOnNullFields() {
        // Arrange: BillRequest with null optional fields
        BillRequest bill = new BillRequest();
        bill.setHouseCode(null);
        bill.setTitle(null);
        bill.setCreatorId(null);
        bill.setRoommateIds(null);
        bill.setSplits(null);

        assertDoesNotThrow(bill::sanitize);
        assertNull(bill.getHouseCode());
        assertNull(bill.getTitle());
        assertNull(bill.getCreatorId());
        assertNull(bill.getRoommateIds());
        assertNull(bill.getSplits());
    }
}
