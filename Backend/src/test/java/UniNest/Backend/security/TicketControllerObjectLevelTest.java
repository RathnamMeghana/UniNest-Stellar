package UniNest.Backend.security;

import UniNest.Backend.controller.TicketController;
import UniNest.Backend.dto.UpdateTicketAgentDataRequest;
import UniNest.Backend.dto.UpdateTicketPriorityRequest;
import UniNest.Backend.dto.UpdateTicketStatusRequest;
import UniNest.Backend.exception.TicketServiceException;
import UniNest.Backend.model.Ticket;
import UniNest.Backend.service.TicketService;
import UniNest.Backend.util.SanitizationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TicketControllerObjectLevelTest {

    @Mock private TicketService ticketService;

    @InjectMocks private TicketController ticketController;

    private Ticket testTicket;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        // Sample Ticket
        testTicket = new Ticket();
        testTicket.setId("ticket1");
        testTicket.setDescription("Test ticket");
        testTicket.setRoom("Room 101");
        testTicket.setBuilding("Building A");
        testTicket.setApartmentId("Apartment101");
        testTicket.setCategory("Maintenance");
        testTicket.setPriority("High");
        testTicket.setStatus("Open");
        testTicket.setUserId("user1");
        testTicket.setLandlordId("landlord123");
    }

    // ------------------- CREATE TICKET -------------------
    @Test
    void createTicket_Success() {
        when(ticketService.createTicket(any(Ticket.class))).thenReturn("Ticket created");

        ResponseEntity<String> response = ticketController.createTicket(testTicket);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Ticket created", response.getBody());
        verify(ticketService, times(1)).createTicket(any(Ticket.class));
    }

    @Test
    void createTicket_ServiceThrowsException() {
        when(ticketService.createTicket(any(Ticket.class))).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<String> response = ticketController.createTicket(testTicket);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Error creating ticket"));
    }

    // ------------------- GET TICKETS -------------------
    @Test
    void getTicketsByBuilding_Success() {
        when(ticketService.getTicketsByBuilding("Building A")).thenReturn(List.of(testTicket));

        ResponseEntity<List<Ticket>> response = ticketController.getTicketsByBuilding("Building A");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(ticketService, times(1)).getTicketsByBuilding("Building A");
    }

    @Test
    void getTicketsByBuilding_Exception() {
        when(ticketService.getTicketsByBuilding("Building A")).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<List<Ticket>> response = ticketController.getTicketsByBuilding("Building A");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getTicketsByApartment_Success() {
        when(ticketService.getTicketsByApartment("Apartment101")).thenReturn(List.of(testTicket));

        ResponseEntity<List<Ticket>> response = ticketController.getTicketsByApartment("Apartment101");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(ticketService, times(1)).getTicketsByApartment("Apartment101");
    }

    @Test
    void getTicketsByLandlord_Success() throws ExecutionException, InterruptedException {
        when(ticketService.getTicketsByLandlord("landlord123")).thenReturn(List.of(testTicket));

        ResponseEntity<List<Ticket>> response = ticketController.getTicketsByLandlord("landlord123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("landlord123", response.getBody().get(0).getLandlordId());
        verify(ticketService, times(1)).getTicketsByLandlord("landlord123");
    }

    @Test
    void getTicketsByLandlord_Exception() throws ExecutionException, InterruptedException {
        when(ticketService.getTicketsByLandlord("landlord123"))
                .thenThrow(new TicketServiceException("Service unavailable", null));

        ResponseEntity<List<Ticket>> response = ticketController.getTicketsByLandlord("landlord123");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    // ------------------- UPDATE STATUS -------------------
    @Test
    void updateTicketStatus_Success() {
        UpdateTicketStatusRequest request = new UpdateTicketStatusRequest();
        request.setTicketId("ticket1");
        request.setStatus("Closed");

        when(ticketService.updateTicketStatus("ticket1", "Closed")).thenReturn("Status updated");

        ResponseEntity<String> response = ticketController.updateTicketStatus(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Status updated", response.getBody());
    }

    @Test
    void updateTicketStatus_NotFound() {
        UpdateTicketStatusRequest request = new UpdateTicketStatusRequest();
        request.setTicketId("invalid");
        request.setStatus("Closed");

        when(ticketService.updateTicketStatus("invalid", "Closed"))
                .thenThrow(new IllegalArgumentException("Ticket not found"));

        ResponseEntity<String> response = ticketController.updateTicketStatus(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Ticket not found", response.getBody());
    }

    // ------------------- UPDATE PRIORITY -------------------
    @Test
    void updateTicketPriority_Success() {
        UpdateTicketPriorityRequest request = new UpdateTicketPriorityRequest();
        request.setTicketId("ticket1");
        request.setPriority("Low");

        when(ticketService.updateTicketPriority("ticket1", "Low")).thenReturn("Priority updated");

        ResponseEntity<String> response = ticketController.updateTicketPriority(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Priority updated", response.getBody());
    }

    @Test
    void updateTicketPriority_NotFound() {
        UpdateTicketPriorityRequest request = new UpdateTicketPriorityRequest();
        request.setTicketId("invalid");
        request.setPriority("High");

        when(ticketService.updateTicketPriority("invalid", "High"))
                .thenThrow(new IllegalArgumentException("Ticket not found"));

        ResponseEntity<String> response = ticketController.updateTicketPriority(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Ticket not found", response.getBody());
    }

    // ------------------- UPDATE AGENT DATA -------------------
    @Test
    void updateAgentData_Success() {
        UpdateTicketAgentDataRequest request = new UpdateTicketAgentDataRequest();
        request.setTicketId("ticket1");
        request.setResponse("Response");
        request.setArrivalDate("2026-02-15");

        when(ticketService.updateAgentData("ticket1", "Response", "2026-02-15"))
                .thenReturn("Agent data updated");

        ResponseEntity<String> response = ticketController.updateAgentData(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Agent data updated", response.getBody());
    }

    @Test
    void updateAgentData_Exception() {
        UpdateTicketAgentDataRequest request = new UpdateTicketAgentDataRequest();
        request.setTicketId("ticket1");
        request.setResponse("Response");
        request.setArrivalDate("2026-02-15");

        when(ticketService.updateAgentData("ticket1", "Response", "2026-02-15"))
                .thenThrow(new RuntimeException("DB error"));

        ResponseEntity<String> response = ticketController.updateAgentData(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("DB error"));
    }

    // ------------------- SANITIZATION -------------------
    @Test
    void sanitizationOnCreate() {
        testTicket.setDescription("<script>alert('XSS')</script>");
        ticketController.createTicket(testTicket);
        assertFalse(testTicket.getDescription().contains("<script>"));
    }
}
