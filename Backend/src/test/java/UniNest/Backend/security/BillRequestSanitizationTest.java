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
        // Jsoup removes <script> AND its internal content
        bill.setHouseCode("<script>alert('xss')</script>");
        bill.setTitle("My <b>Bill</b>");
        bill.setCreatorId("user<script>");
        // Note: use new ArrayList so it is mutable for the sanitize() method
        bill.setRoommateIds(new ArrayList<>(Arrays.asList("user1<script>", "user2<b>")));

        BillRequest.Split split1 = new BillRequest.Split();
        split1.setUserId("splitUser<script>");
        split1.setEmail("email@example.com<script>");
        split1.setBillTitle("Split <b>Title</b>");
        split1.setBillId("billId<script>");
        split1.setAmountOwed(100);

        bill.setSplits(new ArrayList<>(Arrays.asList(split1)));

        // Act
        bill.sanitize();

        // Assertions adjusted for Jsoup behavior
        assertEquals("", bill.getHouseCode()); // Script content is discarded
        assertEquals("My Bill", bill.getTitle()); // Tags removed, text kept
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
        BillRequest bill = new BillRequest();
        assertDoesNotThrow(bill::sanitize);
        assertNull(bill.getHouseCode());
        assertNull(bill.getRoommateIds());
    }
}