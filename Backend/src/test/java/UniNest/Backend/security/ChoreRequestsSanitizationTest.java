package UniNest.Backend.security;

import UniNest.Backend.dto.ChoreRequests;
import UniNest.Backend.util.SanitizationUtil;
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

        // expected: HTML stripped completely
        assertEquals("alert('xss')", chore.getTaskName());
        assertEquals("", chore.getRoom());
        assertEquals("useralert('hack')", chore.getAssignedTo());
    }

    @Test
    void sanitizeChoreRequests_NullFields_DoesNotThrow() {
        ChoreRequests chore = new ChoreRequests();
        // all fields null
        chore.sanitize();
        // nothing to assert, test passes if no exception
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

