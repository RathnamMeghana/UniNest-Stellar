package UniNest.Backend.controller;

import UniNest.Backend.exception.TicketServiceException;
import UniNest.Backend.model.Ticket;
import UniNest.Backend.service.TicketService;
import UniNest.Backend.dto.UpdateTicketStatusRequest;
import UniNest.Backend.dto.UpdateTicketPriorityRequest;
import UniNest.Backend.util.SanitizationUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.security.core.Authentication;

public class TicketControllerTest {

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private TicketController ticketController;

    private Ticket testTicket;

    @Mock
    private Authentication mockAuth;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        Mockito.when(mockAuth.getName()).thenReturn("test-user");

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
    }

    // Create Ticket Success
    @Test
    public void testCreateTicket_Success()  {
        when(ticketService.createTicket(any(Ticket.class))).thenReturn("Ticket created");

        // Tell the mock what "user ID" to return when the controller asks
        Mockito.when(mockAuth.getName()).thenReturn("test-user-id");
        Mockito.when(mockAuth.getPrincipal()).thenReturn("test-user-email@example.com");

        // 3. Update the call to include mockAuth as the second parameter
        // Change this line:
        ResponseEntity<String> response = ticketController.createTicket(testTicket, mockAuth);
       // ResponseEntity<String> response = ticketController.createTicket(testTicket);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Ticket created", response.getBody());

        verify(ticketService, times(1)).createTicket(any(Ticket.class));
    }

    @Test
    public void testCreateTicket_ServiceThrowsException() {
        when(ticketService.createTicket(any(Ticket.class))).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<String> response = ticketController.createTicket(testTicket, mockAuth);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().contains("Error creating ticket"));
    }

    //  Get Tickets by Building Success
    @Test
    public void testGetTicketsByBuilding_Success()  {
        List<Ticket> tickets = List.of(testTicket);
        when(ticketService.getTicketsByBuilding("Building A")).thenReturn(tickets);

        ResponseEntity<List<Ticket>> response = ticketController.getTicketsByBuilding("Building A");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(ticketService, times(1)).getTicketsByBuilding("Building A");
    }

    //  Get Tickets by Building Exception
    @Test
    public void testGetTicketsByBuilding_Exception()  {
        when(ticketService.getTicketsByBuilding("Building A")).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<List<Ticket>> response = ticketController.getTicketsByBuilding("Building A");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    // Get Tickets by Apartment Success
    @Test
    public void testGetTicketsByApartment_Success()  {
        List<Ticket> tickets = List.of(testTicket);
        when(ticketService.getTicketsByApartment("Apartment101")).thenReturn(tickets);

        ResponseEntity<List<Ticket>> response = ticketController.getTicketsByApartment("Apartment101");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(ticketService, times(1)).getTicketsByApartment("Apartment101");
    }

    // Update Ticket Status Success
    @Test
    public void testUpdateTicketStatus_Success()  {
        UpdateTicketStatusRequest request = new UpdateTicketStatusRequest();
        request.setTicketId("ticket1");
        request.setStatus("Closed");

        when(ticketService.updateTicketStatus("ticket1", "Closed")).thenReturn("Status updated");

        ResponseEntity<String> response = ticketController.updateTicketStatus(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Status updated", response.getBody());
    }

    // Update Ticket Status not found
    @Test
    public void testUpdateTicketStatus_NotFound()  {
        UpdateTicketStatusRequest request = new UpdateTicketStatusRequest();
        request.setTicketId("invalid");
        request.setStatus("Closed");

        when(ticketService.updateTicketStatus("invalid", "Closed"))
                .thenThrow(new IllegalArgumentException("Ticket not found"));

        ResponseEntity<String> response = ticketController.updateTicketStatus(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Ticket not found", response.getBody());
    }

    //  Update Ticket Priority Success
    @Test
    public void testUpdateTicketPriority_Success()  {
        UpdateTicketPriorityRequest request = new UpdateTicketPriorityRequest();
        request.setTicketId("ticket1");
        request.setPriority("Low");

        when(ticketService.updateTicketPriority("ticket1", "Low")).thenReturn("Priority updated");

        ResponseEntity<String> response = ticketController.updateTicketPriority(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Priority updated", response.getBody());
    }

    //  Update Ticket Priority Not Found
    @Test
    public void testUpdateTicketPriority_NotFound()  {
        UpdateTicketPriorityRequest request = new UpdateTicketPriorityRequest();
        request.setTicketId("invalid");
        request.setPriority("High");

        when(ticketService.updateTicketPriority("invalid", "High"))
                .thenThrow(new IllegalArgumentException("Ticket not found"));

        ResponseEntity<String> response = ticketController.updateTicketPriority(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Ticket not found", response.getBody());
    }

    // Sanitization Check
    @Test
    public void testSanitizationOnCreate() {
        testTicket.setDescription("<script>alert('XSS');</script>");
        ticketController.createTicket(testTicket, mockAuth);

        // make sure description was sanitized
        assertFalse(testTicket.getDescription().contains("<script>"));
    }

    @Test
    public void TestGetTicketsByLandlord_Success() throws ExecutionException, InterruptedException {
        // set up
        String landlordId = "landlord123";
        boolean includeDeleted = true; // Define the new parameter
        testTicket.setLandlordId(landlordId);
        List<Ticket> tickets = List.of(testTicket);

        // Update Mockito stub (added second argument)
        when(ticketService.getTicketsByLandlord(landlordId, includeDeleted)).thenReturn(tickets);

        // Update Controller call (added second argument)
        ResponseEntity<List<Ticket>> response = ticketController.getTicketsByLandlord(landlordId, includeDeleted);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(landlordId, response.getBody().get(0).getLandlordId());

        // Update verification (added second argument)
        verify(ticketService, times(1)).getTicketsByLandlord(landlordId, includeDeleted);
    }


    @Test
    public void TestGetTicketsByLandlord_NotFound() throws ExecutionException, InterruptedException {
        // set up
        String landlordId = "empty_landlord";
        boolean includeDeleted = true;

        // Update Mockito stub added second argument
        when(ticketService.getTicketsByLandlord(landlordId, includeDeleted)).thenReturn(List.of());

        // Update Controller call added second argument
        ResponseEntity<List<Ticket>> response = ticketController.getTicketsByLandlord(landlordId, includeDeleted);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        // Update Verification added second argument
        verify(ticketService).getTicketsByLandlord(landlordId, includeDeleted);
    }

    @Test
    public void TestGetTicketsByLandlord_NotLandLord() throws ExecutionException, InterruptedException {
        //set up
        String landlordId = "landlord123";
        when(ticketService.getTicketsByLandlord(landlordId))
                .thenThrow(new TicketServiceException("Firestore unavailable", null));

        ResponseEntity<List<Ticket>> response = ticketController.getTicketsByLandlord(landlordId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }
}
