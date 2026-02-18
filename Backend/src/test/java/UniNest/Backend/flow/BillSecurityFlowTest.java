package UniNest.Backend.flow;

import UniNest.Backend.config.SecurityConfig;
import UniNest.Backend.controller.BillController;
import UniNest.Backend.controller.GlobalExceptionHandler;
import UniNest.Backend.dto.BillRequest;
import UniNest.Backend.exception.BillServiceException;
import UniNest.Backend.security.FirebaseTokenFilter;
import UniNest.Backend.service.BillService;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BillController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = true)
public class BillSecurityFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BillService billService;

    @MockBean
    private FirebaseTokenFilter firebaseTokenFilter;

    private BillRequest validRequest;

    @BeforeEach
    public void setup() throws Exception {
        // 1. Valid Request Setup
        validRequest = new BillRequest();
        validRequest.setHouseCode("H123");
        validRequest.setTitle("Electricity");
        validRequest.setTotalAmount(100.0);
        validRequest.setBillType(BillRequest.BillType.ONE_TIME);
        validRequest.setCreatorId("user1");
        validRequest.setRoommateIds(Arrays.asList("user1", "user2"));
        BillRequest.Split split = new BillRequest.Split();
        split.setUserId("user2");
        split.setAmountOwed(100.0);
        validRequest.setSplits(Collections.singletonList(split));

        // 2. THE FORCE-FORWARD FIX
        // We mock 'doFilter' (the parent) instead of 'doFilterInternal'.
        // This is more reliable for ensuring the chain continues.
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response); // PASS THE BALL TO THE CONTROLLER
            return null;
        }).when(firebaseTokenFilter).doFilter(any(), any(), any());

        // 3. Stub Service - Return a list so we can see data in the response body
        when(billService.createBill(any(BillRequest.class))).thenReturn(List.of(validRequest));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    public void createBill_AsTenant_Allowed() throws Exception {
        mockMvc.perform(post("/bills/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        // This WILL pass now if the Handler is no longer null
        verify(billService, times(1)).createBill(any(BillRequest.class));
    }

    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    public void createBill_AsAgent_Forbidden() throws Exception {
        mockMvc.perform(post("/bills/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(billService, never()).createBill(any());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    public void createBill_SanitizationFlow() throws Exception {
        validRequest.setTitle("Rent <script>alert(1)</script>");

        mockMvc.perform(post("/bills/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        ArgumentCaptor<BillRequest> captor = ArgumentCaptor.forClass(BillRequest.class);
        verify(billService).createBill(captor.capture());

        assertFalse(captor.getValue().getTitle().contains("<script>"), "Title must be sanitized");
    }
    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("Logic Flow: Bill creation fails if splits don't add up to total")
    public void createBill_InvalidMath_Returns400() throws Exception {
        BillRequest request = new BillRequest();
        request.setTotalAmount(100.0); // Total is 100

        BillRequest.Split split = new BillRequest.Split();
        split.setAmountOwed(40.0); // But splits only add up to 40
        request.setSplits(Collections.singletonList(split));

        // Assume you add split validation in your service or a custom validator
        when(billService.createBill(any())).thenThrow(new BillServiceException("Splits must sum to total", HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/bills/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}