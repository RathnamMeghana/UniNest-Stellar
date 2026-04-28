package UniNest.Backend.logging;

import UniNest.Backend.config.LoggingAspect;
import UniNest.Backend.config.SecurityConfig;
import UniNest.Backend.controller.GlobalExceptionHandler;
import UniNest.Backend.controller.TicketController;
import UniNest.Backend.model.Ticket;
import UniNest.Backend.security.FirebaseTokenFilter;
import UniNest.Backend.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Logging and Non-Repudiation Integration Tests.
 * Verifies that actions are logged to the file system and tied to authenticated users.
 */
@WebMvcTest(TicketController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, LoggingAspect.class})
@EnableAspectJAutoProxy // Enables the AOP interceptors to run during the test
@AutoConfigureMockMvc(addFilters = true) // Enables Security Filters to inject Authentication
public class Logging {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TicketService ticketService;

    @MockBean
    private FirebaseTokenFilter firebaseTokenFilter;

    @BeforeEach
    void setup() throws Exception {
        // Bypass Firebase Filter logic to allow @WithMockUser to populate the Authentication object
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(firebaseTokenFilter).doFilter(any(), any(), any());

        // Stub successful response
        when(ticketService.createTicket(any(Ticket.class))).thenReturn("SUCCESS");
    }

    @Test
    @DisplayName("Logging: Verify that the audit log file exists and is writable")
    public void testLogFileExists() {
        // Check for the file specified in logback-spring.xml
        File logFile = new File("logs/uninest.log");
        assertTrue(logFile.exists(), "Log file should be created on startup. Ensure the 'logs' folder exists.");
        assertTrue(logFile.canRead(), "Log file should be readable for audit purposes.");
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("Logging: Verify AOP correctly logs Ticket API requests to the physical log file")
    public void testAOPLoggingContent() throws Exception {
        // 1. Setup valid request
        Ticket ticket = new Ticket();
        ticket.setDescription("AOP Audit Check");
        ticket.setApartmentId("APT-101");

        // 2. Perform request
        mockMvc.perform(post("/tickets/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ticket)))
                .andExpect(status().isCreated());

        // 3. WAIT AND RETRY logic (Max 3 seconds)
        // This handles the delay between Java code and the Mac file system
        File logFile = new File("logs/uninest.log");
        boolean found = false;

        for (int i = 0; i < 6; i++) {
            if (logFile.exists()) {
                List<String> lines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);
                found = lines.stream().anyMatch(line ->
                        line.contains(">>> API REQ") && line.contains("/tickets/create"));

                if (found) break;
            }
            Thread.sleep(500); // Wait 0.5s before checking again
        }

        assertTrue(found, "Logging Failure: Entry '>>> API REQ' not found in " + logFile.getAbsolutePath());
    }
    @Test
    @WithMockUser(username = "audit_uid_firebase_789", roles = "TENANT")
    @DisplayName("Non-Repudiation: Authenticated Firebase UID is permanently tied to the Ticket")
    public void testNonRepudiation_UserBinding() throws Exception {
        Ticket ticket = new Ticket();
        ticket.setDescription("Non-repudiation security test");
        ticket.setUserId("attempted_spoof_id"); // Attacker tries to impersonate another user

        // Perform the request
        mockMvc.perform(post("/tickets/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ticket)))
                .andExpect(status().isCreated());

        // Capture the object that reached the service layer
        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketService).createTicket(captor.capture());

        // PROOF OF NON-REPUDIATION:
        // The controller must have ignored the "attempted_spoof_id" from the JSON
        // and forced the "audit_uid_firebase_789" from the secure token.
        assertEquals("audit_uid_firebase_789", captor.getValue().getUserId(),
                "Non-Repudiation failure: The ticket was not stamped with the authenticated UID.");
    }
}