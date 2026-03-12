package UniNest.Backend.controller;

import lombok.extern.slf4j.Slf4j; // 1. ADD THIS IMPORT
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import UniNest.Backend.dto.UpdateTicketAgentDataRequest;
import UniNest.Backend.model.Ticket;
import UniNest.Backend.service.TicketService;
import UniNest.Backend.dto.UpdateTicketPriorityRequest;
import UniNest.Backend.dto.UpdateTicketStatusRequest;
import UniNest.Backend.util.SanitizationUtil;
import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;

    @Autowired
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PreAuthorize("hasRole('TENANT')")
    @PostMapping("/create")
    public ResponseEntity<String> createTicket(@Valid @RequestBody Ticket ticket, Authentication auth) {
        try {
            ticket.sanitize();

            // Non-Repudiation: Stamp the authenticated UID on the ticket
            String uid = auth.getName();
            ticket.setUserId(uid);

            // Logging for audit trail (Uses the email/principal for readability)
            log.info(">>> API REQ: User {} is creating a ticket", auth.getPrincipal());

            String result = ticketService.createTicket(ticket);

            // Return 201 Created for better REST standards
            return new ResponseEntity<>(result, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("!!! API ERR: Ticket creation failed: {}", e.getMessage());
            return new ResponseEntity<>("Error creating ticket: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PreAuthorize("hasRole('LETTINGAGENT')")
    @GetMapping("/building")
    public ResponseEntity<List<Ticket>> getTicketsByBuilding(@RequestParam String name) {
        try {
            List<Ticket> tickets = ticketService.getTicketsByBuilding(name);
            return new ResponseEntity<>(tickets, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PreAuthorize("hasAnyRole('LETTINGAGENT', 'TENANT')")
   // @PreAuthorize("hasRole('LETTINGAGENT') or hasRole('TENANT')")
    @GetMapping("/apartment")
    public ResponseEntity<List<Ticket>> getTicketsByApartment(@RequestParam String name) {
        try {
            List<Ticket> tickets = ticketService.getTicketsByApartment(name);
            return new ResponseEntity<>(tickets, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PreAuthorize("hasRole('LETTINGAGENT')")
    @GetMapping("/landlord")
    public ResponseEntity<List<Ticket>> getTicketsByLandlord(@RequestParam String id) {
        try {
            List<Ticket> tickets = ticketService.getTicketsByLandlord(id);
            return new ResponseEntity<>(tickets, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PreAuthorize("hasRole('LETTINGAGENT')")
    @PutMapping("/status")
    public ResponseEntity<String> updateTicketStatus(@Valid @RequestBody UpdateTicketStatusRequest request) {
        try {
            request.setTicketId(SanitizationUtil.sanitize(request.getTicketId()));
            request.setStatus(SanitizationUtil.sanitize(request.getStatus()));
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

    @PreAuthorize("hasRole('LETTINGAGENT')")
    @PutMapping("/priority")
    public ResponseEntity<String> updateTicketPriority(
            @Valid @RequestBody UpdateTicketPriorityRequest request) {

        try {
            request.setTicketId(SanitizationUtil.sanitize(request.getTicketId()));
            request.setPriority(SanitizationUtil.sanitize(request.getPriority()));
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
    @PreAuthorize("hasRole('LETTINGAGENT')")
    @PutMapping("/agent-update")
    public ResponseEntity<String> updateAgentData(@RequestBody UpdateTicketAgentDataRequest request) {
        try {
            return new ResponseEntity<>(
                    ticketService.updateAgentData(request.getTicketId(), request.getResponse(), request.getArrivalDate()),
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('TENANT')")
    @DeleteMapping("/{ticketId}")
    public ResponseEntity<String> softDeleteTicket(@PathVariable String ticketId) {
        try {
            String result = ticketService.softDeleteTicket(ticketId);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error soft deleting ticket: {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
