package UniNest.Backend.controllerHandling;

import UniNest.Backend.controller.TicketController;
import UniNest.Backend.controller.GlobalExceptionHandler;
import UniNest.Backend.dto.UpdateTicketAgentDataRequest;
import UniNest.Backend.dto.UpdateTicketPriorityRequest;
import UniNest.Backend.dto.UpdateTicketStatusRequest;
import UniNest.Backend.exception.TicketServiceException;
import UniNest.Backend.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TicketController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
public class TicketControllerErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TicketService ticketService;

    // --- TICKET PRIORITY VALIDATION ---
    @Test
    @WithMockUser
    @DisplayName("PUT /tickets/priority - Should return 400 when priority is invalid")
    public void updatePriority_WhenInvalidPattern_Returns400() throws Exception {
        UpdateTicketPriorityRequest invalidRequest = new UpdateTicketPriorityRequest();
        invalidRequest.setTicketId("T123");
        invalidRequest.setPriority("Ultra-High");

        // FIX: Changed patch to put, and URL to /tickets/priority
        mockMvc.perform(put("/tickets/priority")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.priority").exists());
    }

    // --- TICKET STATUS VALIDATION ---
    @Test
    @WithMockUser
    @DisplayName("PUT /tickets/status - Should return 400 when status is invalid")
    public void updateStatus_WhenInvalidPattern_Returns400() throws Exception {
        UpdateTicketStatusRequest invalidRequest = new UpdateTicketStatusRequest();
        invalidRequest.setTicketId("T123");
        invalidRequest.setStatus("Deleted");

        // FIX: Changed patch to put, and URL to /tickets/status
        mockMvc.perform(put("/tickets/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists());
    }

    // --- SERVICE ERROR ---
    @Test
    @WithMockUser
    @DisplayName("PUT /tickets/agent-update - Should return 500 when Firestore fails")
    public void updateAgentData_WhenServiceFails_Returns500() throws Exception {
        UpdateTicketAgentDataRequest request = new UpdateTicketAgentDataRequest();
        request.setTicketId("T123");
        request.setResponse("Working on it.");
        request.setArrivalDate("2024-12-01");

        // FIX: Match the 3-parameter service signature
        when(ticketService.updateAgentData(anyString(), anyString(), anyString()))
                .thenThrow(new TicketServiceException("Failed to update Firestore", HttpStatus.INTERNAL_SERVER_ERROR));

        // FIX: Changed patch to put, and URL to /tickets/agent-update
        mockMvc.perform(put("/tickets/agent-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Failed to update Firestore")));
    }
}


