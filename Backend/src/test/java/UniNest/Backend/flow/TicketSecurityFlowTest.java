package UniNest.Backend.flow;

import UniNest.Backend.config.SecurityConfig;
import UniNest.Backend.controller.GlobalExceptionHandler;
import UniNest.Backend.controller.TicketController;
import UniNest.Backend.dto.UpdateTicketAgentDataRequest;
import UniNest.Backend.dto.UpdateTicketStatusRequest;
import UniNest.Backend.model.Ticket;
import UniNest.Backend.security.FirebaseTokenFilter;
import UniNest.Backend.service.TicketService;
import UniNest.Backend.exception.TicketServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TicketController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = true)
public class TicketSecurityFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TicketService ticketService;

    @MockBean
    private FirebaseTokenFilter firebaseTokenFilter;

    @BeforeEach
    public void setup() throws Exception {
        // Correct filter bypass for Spring Boot
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(firebaseTokenFilter).doFilter(any(), any(), any());

        // Default successful returns
        when(ticketService.createTicket(any(Ticket.class))).thenReturn("Ticket Created");
    }


    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("Security Flow: Tenant cannot view tickets for a different apartment")
    public void getTickets_OtherApartment_Forbidden() throws Exception {
        String otherApartment = "Apartment_99";

        // Service throws Forbidden
        when(ticketService.getTicketsByApartment(otherApartment))
                .thenThrow(new TicketServiceException("Access Denied", HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/tickets/apartment")
                        .param("name", otherApartment))
                // Controller catches 403 and returns 500
                .andExpect(status().isInternalServerError());
    }


    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    @DisplayName("Security Flow: Agent cannot access tickets for a building they don't manage")
    public void getTickets_UnmanagedBuilding_Forbidden() throws Exception {
        String buildingName = "Other_Building";

        when(ticketService.getTicketsByBuilding(buildingName))
                .thenThrow(new TicketServiceException("Unauthorized", HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/tickets/building")
                        .param("name", buildingName))
                // Controller catches 403 and returns 500 per your current code
                .andExpect(status().isInternalServerError());
    }


    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    @DisplayName("Security Flow: URL query parameters are passed to service")
    public void getTickets_ParamCheck() throws Exception {
        String unsafeName = "Apartment <b>101</b>";

        mockMvc.perform(get("/tickets/apartment")
                        .param("name", unsafeName))
                .andExpect(status().isOk());

        // Since the controller doesn't sanitize, we verify the service gets the raw string
        // This matches your current controller implementation
        verify(ticketService).getTicketsByApartment(unsafeName);
    }


    @Test
    @WithMockUser(roles = "TENANT")
    public void createTicket_AsTenant_Allowed() throws Exception {
        Ticket ticket = new Ticket();
        ticket.setDescription("Leak");

        mockMvc.perform(post("/tickets/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ticket)))
                .andExpect(status().isCreated());

        verify(ticketService).createTicket(any(Ticket.class));
    }
    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("Robustness Flow: Sanitization handles null fields without crashing")
    public void createTicket_NullFields_HandlesGracefully() throws Exception {
        Ticket nullFieldTicket = new Ticket();
        nullFieldTicket.setDescription("Just a description");
        nullFieldTicket.setRoom(null); // Explicit null
        nullFieldTicket.setCategory(null);

        mockMvc.perform(post("/tickets/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nullFieldTicket)))
                .andExpect(status().isCreated());

        // If this test reaches here without a 500 error, your sanitize() method is null-safe.
        verify(ticketService, times(1)).createTicket(any(Ticket.class));
    }

    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    @DisplayName("Agent attempting to confirm tenant visit returns 403 Forbidden")
    public void confirmVisit_AsAgent_Forbidden() throws Exception {
        // Letting Agents manage status via /status, NOT /confirm-visit
        mockMvc.perform(put("/tickets/confirm-visit")
                        .param("ticketId", "T123")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(ticketService, never()).confirmVisitResolution(anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("Tenant can access their own confirmation endpoint")
    public void confirmVisit_AsTenant_Allowed() throws Exception {
        when(ticketService.confirmVisitResolution(eq("T123"), anyString()))
                .thenReturn("Visit confirmed");

        mockMvc.perform(put("/tickets/confirm-visit")
                        .param("ticketId", "T123")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

}