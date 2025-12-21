package UniNest.Backend.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import UniNest.Backend.model.Ticket;
import UniNest.Backend.service.TicketService;
import UniNest.Backend.dto.UpdateTicketPriorityRequest;
import UniNest.Backend.dto.UpdateTicketStatusRequest;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;

    // Use constructor injection for the service
    @Autowired
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/create")
    public ResponseEntity<String> createTicket(@RequestBody Ticket ticket) {
        try {
            String result = ticketService.createTicket(ticket);
            return new ResponseEntity<>(result, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("Error creating ticket: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/building")
    public ResponseEntity<List<Ticket>> getTicketsByBuilding(@RequestParam String name) {
        try {
            List<Ticket> tickets = ticketService.getTicketsByBuilding(name);
            return new ResponseEntity<>(tickets, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/apartment")
    public ResponseEntity<List<Ticket>> getTicketsByApartment(@RequestParam String name) {
        try {
            List<Ticket> tickets = ticketService.getTicketsByApartment(name);
            return new ResponseEntity<>(tickets, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/status")
    public ResponseEntity<String> updateTicketStatus( @RequestBody UpdateTicketStatusRequest request) {
        try {
            String result = ticketService.updateTicketStatus(
                    request.getTicketId(),
                    request.getStatus()
            );
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    "Error updating ticket status: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    @PutMapping("/priority")
    public ResponseEntity<String> updateTicketPriority(
            @RequestBody UpdateTicketPriorityRequest request) {

        try {
            return new ResponseEntity<>(
                    ticketService.updateTicketPriority(
                            request.getTicketId(),
                            request.getPriority()
                    ),
                    HttpStatus.OK
            );
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    "Error updating ticket priority: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

}
