package UniNest.Backend.security;

import UniNest.Backend.controller.TicketController;
import UniNest.Backend.dto.UpdateTicketAgentDataRequest;
import UniNest.Backend.dto.UpdateTicketPriorityRequest;
import UniNest.Backend.dto.UpdateTicketStatusRequest;
import UniNest.Backend.exception.TicketServiceException;
import UniNest.Backend.model.Ticket;
import UniNest.Backend.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Added Import

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TicketControllerObjectLevelTest {

    @Mock private TicketService ticketService;

    @Mock private Authentication mockAuth; // Added Mock for Authentication

    @InjectMocks private TicketController ticketController;

    private Ticket testTicket;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        // Configure mock authentication to return a default UID
        when(mockAuth.getName()).thenReturn("user1");
        when(mockAuth.getPrincipal()).thenReturn("user1@test.com");

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

        // Pass mockAuth as the second parameter
        ResponseEntity<String> response = ticketController.createTicket(testTicket, mockAuth);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Ticket created", response.getBody());
        verify(ticketService, times(1)).createTicket(any(Ticket.class));
    }

    @Test
    void createTicket_ServiceThrowsException() {
        when(ticketService.createTicket(any(Ticket.class))).thenThrow(new RuntimeException("DB error"));

        // Pass mockAuth as the second parameter
        ResponseEntity<String> response = ticketController.createTicket(testTicket, mockAuth);

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
    }

    @Test
    void getTicketsByApartment_Success() {
        when(ticketService.getTicketsByApartment("Apartment101")).thenReturn(List.of(testTicket));

        ResponseEntity<List<Ticket>> response = ticketController.getTicketsByApartment("Apartment101");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getTicketsByLandlord_Success() throws ExecutionException, InterruptedException {
        when(ticketService.getTicketsByLandlord("landlord123")).thenReturn(List.of(testTicket));

        ResponseEntity<List<Ticket>> response = ticketController.getTicketsByLandlord("landlord123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
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

    // ------------------- SANITIZATION -------------------
    @Test
    void sanitizationOnCreate() {
        testTicket.setDescription("<script>alert('XSS')</script>");

        // Pass mockAuth here as well
        ticketController.createTicket(testTicket, mockAuth);

        // The script content is deleted by Jsoup.clean()
        assertEquals("", testTicket.getDescription());
    }
}