package UniNest.Backend.security;

import UniNest.Backend.model.Ticket;
import UniNest.Backend.dto.UpdateTicketAgentDataRequest;
import UniNest.Backend.dto.UpdateTicketPriorityRequest;
import UniNest.Backend.dto.UpdateTicketStatusRequest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TicketSanitizationTest {

    @Test
    void sanitizeTicket_RemovesHtmlTags() {
        Ticket ticket = new Ticket();

        ticket.setId("<b>123</b>");
        // Jsoup removes <script> tags AND everything inside them
        ticket.setDescription("<script>alert('xss')</script>");
        ticket.setRoom("<i>101</i>");
        ticket.setBuilding("<div>MainBuilding</div>");
        ticket.setApartmentId("<span>A1</span>");
        ticket.setLandlordId("<p>L1</p>");
        ticket.setApartmentName("<h1>UniNest</h1>");
        ticket.setCategory("<b>Maintenance</b>");
        ticket.setPriority("<i>High</i>");
        ticket.setStatus("<div>Open</div>");
        ticket.setUserId("<span>User1</span>");
        ticket.setAgentResponse("<script>Fixing soon</script>");
        ticket.setArrivalDate("<b>2026-02-20</b>");
        ticket.setUserName("<i>John</i>");
        ticket.setPrioritySource("<img src=x onerror=alert('hack')>");

        ticket.sanitize();

        assertEquals("123", ticket.getId());
        assertEquals("", ticket.getDescription()); // Content inside <script> is deleted
        assertEquals("101", ticket.getRoom());
        assertEquals("MainBuilding", ticket.getBuilding());
        assertEquals("A1", ticket.getApartmentId());
        assertEquals("L1", ticket.getLandlordId());
        assertEquals("UniNest", ticket.getApartmentName());
        assertEquals("Maintenance", ticket.getCategory());
        assertEquals("High", ticket.getPriority());
        assertEquals("Open", ticket.getStatus());
        assertEquals("User1", ticket.getUserId());
        assertEquals("", ticket.getAgentResponse()); // Content inside <script> is deleted
        assertEquals("2026-02-20", ticket.getArrivalDate());
        assertEquals("John", ticket.getUserName());
        assertEquals("", ticket.getPrioritySource()); // <img> is a tag with no text content, so it becomes empty
    }

    @Test
    void sanitizeUpdateTicketAgentDataRequest_RemovesHtmlTags() {
        UpdateTicketAgentDataRequest request = new UpdateTicketAgentDataRequest();

        request.setTicketId("<b>123</b>");
        request.setResponse("<script>alert('xss')</script>");
        request.setArrivalDate("<i>2026-03-01</i>");

        request.sanitize();

        assertEquals("123", request.getTicketId());
        assertEquals("", request.getResponse()); // Content inside <script> is deleted
        assertEquals("2026-03-01", request.getArrivalDate());
    }

    @Test
    void sanitizeUpdateTicketPriorityRequest_RemovesHtmlTags() {
        UpdateTicketPriorityRequest request = new UpdateTicketPriorityRequest();

        request.setTicketId("<b>123</b>");
        request.setPriority("<i>High</i>");

        request.sanitize();

        assertEquals("123", request.getTicketId());
        assertEquals("High", request.getPriority());
    }

    @Test
    void sanitizeUpdateTicketStatusRequest_RemovesHtmlTags() {
        UpdateTicketStatusRequest request = new UpdateTicketStatusRequest();

        request.setTicketId("<b>123</b>");
        request.setStatus("<i>Open</i>");

        request.sanitize();

        assertEquals("123", request.getTicketId());
        assertEquals("Open", request.getStatus());
    }
}