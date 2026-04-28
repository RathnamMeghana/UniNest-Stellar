package UniNest.Backend.controllerHandling;

import UniNest.Backend.controller.BillController;
import UniNest.Backend.controller.GlobalExceptionHandler;
import UniNest.Backend.dto.BillRequest;
import UniNest.Backend.exception.BillServiceException;
import UniNest.Backend.service.BillService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.AccessDeniedException;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {BillController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
public class BillControllerErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BillService billService;

    @Test
    @WithMockUser
    @DisplayName("POST /bills/create - Should return 400 when title is missing")
    public void createBill_WhenValidationFails_Returns400() throws Exception {
        BillRequest invalidRequest = new BillRequest();
        invalidRequest.setTitle(""); // Trigger @NotBlank

        mockMvc.perform(post("/bills/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /bills/create - Should return 400 when amount is zero or negative")
    public void createBill_WhenAmountInvalid_Returns400() throws Exception {
        BillRequest request = new BillRequest();
        request.setTitle("Electricity");
        request.setTotalAmount(-50.0); // Trigger @Positive

        mockMvc.perform(post("/bills/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.totalAmount").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /bills/id/user/pay - Should return 403 when user accesses another's bill")
    public void markAsPaid_WhenAccessDenied_Returns403() throws Exception {
        doThrow(new AccessDeniedException("Cannot access another user's bills"))
                .when(billService).markAsPaid("bill123", "wrongUser");

        mockMvc.perform(patch("/bills/bill123/wrongUser/pay"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("another user's bills")));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /bills/getBills/id - Should return 500 when service fails")
    public void getBills_WhenServiceFails_Returns500() throws Exception {
        // CHANGE THIS LINE: Throw BillServiceException instead of RuntimeException
        when(billService.getBillsByUserId(any()))
                .thenThrow(new BillServiceException("Firestore connection reset", HttpStatus.INTERNAL_SERVER_ERROR));

        mockMvc.perform(get("/bills/getBills/user123"))

                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Firestore connection reset")));
    }
}