package UniNest.Backend.Authentication;

import UniNest.Backend.dto.BillRequest;
import UniNest.Backend.service.ApartmentService;
import UniNest.Backend.service.RoomService;
import UniNest.Backend.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;


@SpringBootTest
@AutoConfigureMockMvc
public class AuthenticationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Mock the services so controller works
    @MockBean
    private ApartmentService apartmentService;
    @MockBean
    private UserService userService;
    @MockBean
    private RoomService roomService;
    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @DisplayName("Flow: Valid Tenant Token allows access to Tenant endpoints")
    @WithMockUser(username = "tenant-uid-123", roles = {"TENANT"})
    public void flow_ValidTenant_Allowed() throws Exception {
        mockMvc.perform(get("/apartments/getAll"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Flow: Valid Agent Token is FORBIDDEN from Tenant-only endpoints")
    @WithMockUser(username = "agent-uid-456", roles = {"LETTINGAGENT"})
    public void flow_AgentOnTenantEndpoint_Forbidden() throws Exception {

        // Create a VALID BillRequest so validation passes

        BillRequest.Split split = new BillRequest.Split();
        split.setUserId("user1");
        split.setAmountOwed(50);
        split.setPaid(false);

        BillRequest request = new BillRequest();
        request.setTitle("Electricity");
        request.setHouseCode("HOUSE123");
        request.setCreatorId("creator1");
        request.setBillType(BillRequest.BillType.ONE_TIME);
        request.setTotalAmount(50);
        request.setRoommateIds(List.of("user1"));
        request.setSplits(List.of(split));

        mockMvc.perform(post("/bills/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }


    @Test
    @DisplayName("Flow: Missing Token returns 403 Forbidden")
    public void flow_NoToken_Returns403() throws Exception {
        // No @WithMockUser → anonymous
        mockMvc.perform(get("/apartments/getAll"))
                .andExpect(status().isForbidden());
    }


    @Test
    @DisplayName("Flow: Expired/Invalid Token returns 401 Unauthorized")
    @WithMockUser(username = "tenant-uid-123", roles = {"TENANT"})
    public void flow_ExpiredToken_Returns401() throws Exception {
        // Simulate expired token by hitting a secured endpoint but overriding Security context
        // If your controller reads Authorization header, you can simulate like this:
        mockMvc.perform(get("/apartments/getAll")
                        .header("Authorization", "Bearer expired-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"LETTINGAGENT"})
    void agentCannotMarkBillAsPaid() throws Exception {
        mockMvc.perform(patch("/bills/1/user1/pay"))
                .andExpect(status().isForbidden());
    }


}
