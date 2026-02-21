package UniNest.Backend.controller;

import UniNest.Backend.config.SecurityConfig;
import UniNest.Backend.dto.BillRequest;
import UniNest.Backend.dto.OwedToUserResponse;
import UniNest.Backend.security.FirebaseTokenFilter;
import UniNest.Backend.service.BillService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BillController.class)
@Import(SecurityConfig.class)
class BillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BillService billService;

    @MockBean
    private FirebaseTokenFilter firebaseTokenFilter;

    @Autowired
    private ObjectMapper objectMapper;

    // ------------------------------------------------------------------------
    // BYPASS FIREBASE FILTER
    // ------------------------------------------------------------------------
    @BeforeEach
    void setup() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(firebaseTokenFilter).doFilter(any(), any(), any());
    }

    // ------------------------------------------------------------------------
    // HELPER METHOD
    // ------------------------------------------------------------------------
    private BillRequest getValidBillRequest() {

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

        return request;
    }

    // ========================================================================
    // CREATE BILL
    // ========================================================================

    @Test
    @WithMockUser(username = "tenantUser", roles = {"TENANT"})
    void createBill_success() throws Exception {

        BillRequest request = getValidBillRequest();

        Mockito.when(billService.createBill(any()))
                .thenReturn(List.of(request));

        mockMvc.perform(post("/bills/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Electricity"));
    }

    @Test
    @WithMockUser(username = "tenantUser", roles = {"TENANT"})
    void createBill_validationError() throws Exception {

        BillRequest invalidRequest = new BillRequest();

        mockMvc.perform(post("/bills/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "unauthorisedUser", roles = {"LETTINGAGENT"})
    void createBill_wrongRole_shouldFail() throws Exception {

        BillRequest request = getValidBillRequest();

        mockMvc.perform(post("/bills/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ========================================================================
    // MARK AS PAID
    // ========================================================================

    @Test
    @WithMockUser(username = "tenantUser", roles = {"TENANT"})
    void markAsPaid_success() throws Exception {

        mockMvc.perform(patch("/bills/123/user1/pay")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Split marked as paid successfully"));
    }

    // ========================================================================
    // GETTERS
    // ========================================================================

    @Test
    @WithMockUser(username = "tenantUser", roles = {"TENANT"})
    void getBillsByUserId_success() throws Exception {

        Mockito.when(billService.getBillsByUserId("user1"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/bills/getBills/user1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "tenantUser", roles = {"TENANT"})
    void getPaidHistory_success() throws Exception {

        Mockito.when(billService.getPaidHistory("user1"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/bills/paidHistory/user1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "tenantUser", roles = {"TENANT"})
    void getTotalOwed_success() throws Exception {

        Mockito.when(billService.getTotalOwedByUserId("user1"))
                .thenReturn(120.50);

        mockMvc.perform(get("/bills/totalOwed/user1"))
                .andExpect(status().isOk())
                .andExpect(content().string("120.5"));
    }

    @Test
    @WithMockUser(username = "tenantUser", roles = {"TENANT"})
    void getOwedToUser_success() throws Exception {

        Mockito.when(billService.getWhatIsOwedToUser("user1"))
                .thenReturn(List.of(new OwedToUserResponse()));

        mockMvc.perform(get("/bills/owedToMe/user1"))
                .andExpect(status().isOk());
    }
}
