package UniNest.Backend.security;

import UniNest.Backend.dto.CalendarEventDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CalendarEventSanitizationTest {

    @Test
    void sanitizeCreate_RemovesHtmlTags() {
        CalendarEventDTO.Create request = new CalendarEventDTO.Create();
        request.setHouseCode("<b>HOUSE123</b>");

        // Jsoup deletes the <script> block entirely
        request.setTitle("<script>alert('xss')</script>Party");

        request.setDescription("<div>Some description</div>");
        request.setAssignedTo("<i>user1</i>");
        request.setRelatedChoreId("<u>chore1</u>");
        request.setLocation("<p>Living Room</p>");

        request.sanitize();

        // Updated Assertions
        assertEquals("HOUSE123", request.getHouseCode()); // Tags removed, text remains
        assertEquals("Party", request.getTitle());        // <script> and its content GONE
        assertEquals("Some description", request.getDescription());
        assertEquals("user1", request.getAssignedTo());
        assertEquals("chore1", request.getRelatedChoreId());
    }

    @Test
    void sanitizeUpdate_RemovesHtmlTags() {
        CalendarEventDTO.Update request = new CalendarEventDTO.Update();
        request.setTitle("<h1>Meeting</h1>");
        request.setDescription("<div>Discuss chores</div>");
        request.setAssignedTo("<span>user2</span>");

        request.sanitize();

        assertEquals("Meeting", request.getTitle());
        assertEquals("Discuss chores", request.getDescription());
        assertEquals("user2", request.getAssignedTo());
    }
}