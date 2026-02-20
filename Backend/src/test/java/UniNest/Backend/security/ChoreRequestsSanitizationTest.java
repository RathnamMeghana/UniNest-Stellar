package UniNest.Backend.security;

import UniNest.Backend.dto.ChoreRequests;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChoreRequestsSanitizationTest {

    @Test
    void sanitizeChoreRequests_RemovesUnsafeCharacters() {
        ChoreRequests chore = new ChoreRequests();

        // simulate malicious input
        chore.setTaskName("<script>alert('xss')</script>");
        chore.setRoom("<img src=x onerror=alert('xss')>");
        chore.setAssignedTo("user<script>alert('hack')</script>");

        // sanitize
        chore.sanitize();

        // ACTUAL Jsoup behavior:
        // 1. <script> content is deleted entirely
        assertEquals("", chore.getTaskName());

        // 2. <img> has no text content, so it becomes empty
        assertEquals("", chore.getRoom());

        // 3. "user" is kept, but the <script> block following it is deleted
        assertEquals("user", chore.getAssignedTo());
    }

    @Test
    void sanitizeChoreRequests_NullFields_DoesNotThrow() {
        ChoreRequests chore = new ChoreRequests();
        // Should not crash even if all fields are null
        chore.sanitize();
    }

    @Test
    void sanitizeChoreRequests_PreservesSafeText() {
        ChoreRequests chore = new ChoreRequests();
        chore.setTaskName("Clean Kitchen");
        chore.setRoom("Living Room");
        chore.setAssignedTo("user123");

        chore.sanitize();

        assertEquals("Clean Kitchen", chore.getTaskName());
        assertEquals("Living Room", chore.getRoom());
        assertEquals("user123", chore.getAssignedTo());
    }
}