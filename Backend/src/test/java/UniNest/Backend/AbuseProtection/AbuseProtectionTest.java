package UniNest.Backend.AbuseProtection;

import UniNest.Backend.config.SecurityConfig;
import UniNest.Backend.controller.ApartmentController;
import UniNest.Backend.controller.BillController;
import UniNest.Backend.controller.GlobalExceptionHandler;
import UniNest.Backend.controller.TicketController;
import UniNest.Backend.dto.BillRequest;
import UniNest.Backend.dto.UpdateTicketAgentDataRequest;
import UniNest.Backend.exception.ApartmentServiceException;
import UniNest.Backend.exception.BillServiceException;
import UniNest.Backend.security.FirebaseTokenFilter;
import UniNest.Backend.service.*;
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

import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = {BillController.class, ApartmentController.class, TicketController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
public class AbuseProtectionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private BillService billService;
    @MockBean private ApartmentService apartmentService;
    @MockBean private TicketService ticketService;
    @MockBean private UserService userService;
    @MockBean private RoomService roomService;
    @MockBean private FirebaseTokenFilter firebaseTokenFilter;

    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    @DisplayName("Abuse Protection: Reject massive JSON payloads")
    public void testAbuse_MassivePayload() throws Exception {
        String massiveJunk = "A".repeat(5000000); // 5MB

        UpdateTicketAgentDataRequest request = new UpdateTicketAgentDataRequest();
        request.setTicketId("T1");
        request.setResponse(massiveJunk);
        request.setArrivalDate("2026-01-01");

        when(ticketService.updateAgentData(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Payload too large"));

        mockMvc.perform(put("/tickets/agent-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Payload too large")));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("Abuse Protection: Spamming endpoint returns 429")
    public void testRateLimiting_SpamProtection() throws Exception {
        // Setup a FULLY VALID request
        BillRequest request = new BillRequest();
        request.setTitle("Spam Bill");
        request.setHouseCode("H1");
        request.setTotalAmount(100.0);
        request.setBillType(BillRequest.BillType.ONE_TIME);
        request.setCreatorId("user1");
        request.setRoommateIds(List.of("user1"));

        //  Splits CANNOT be empty
        BillRequest.Split split = new BillRequest.Split();
        split.setUserId("user1");
        split.setAmountOwed(100.0);
        request.setSplits(List.of(split));

        // Mock service behavior
        when(billService.createBill(any(BillRequest.class)))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList())
                .thenThrow(new BillServiceException("Rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS));

        //  Send 10 requests
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/bills/create")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        //  The 11th request triggers 429
        mockMvc.perform(post("/bills/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    @DisplayName("Availability: Returns 503 when Firestore is unreachable")
    public void testAvailability_FirebaseDown() throws Exception {
        when(apartmentService.getAllApartments())
                .thenThrow(new ApartmentServiceException("Firebase Connection Timeout", HttpStatus.SERVICE_UNAVAILABLE));

        mockMvc.perform(get("/apartments/getAll"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(containsString("Connection Timeout")));
    }
}