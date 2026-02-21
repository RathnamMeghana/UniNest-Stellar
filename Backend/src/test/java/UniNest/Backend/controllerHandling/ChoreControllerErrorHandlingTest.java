package UniNest.Backend.controllerHandling;

import UniNest.Backend.controller.ChoreController;
import UniNest.Backend.controller.GlobalExceptionHandler;
import UniNest.Backend.dto.ChoreRequests;
import UniNest.Backend.exception.ChoreServiceException;
import UniNest.Backend.exception.UserServiceException;
import UniNest.Backend.service.ChoreService;
import UniNest.Backend.service.ChoreSchedulingService; // Added
import UniNest.Backend.service.UserService;           // Added
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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ChoreController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
public class ChoreControllerErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // --- MUST MOCK ALL 3 SERVICES TO START THE CONTEXT ---
    @MockBean
    private ChoreService choreService;

    @MockBean
    private ChoreSchedulingService choreSchedulingService;

    @MockBean
    private UserService userService;
    // -----------------------------------------------------

    @Test
    @WithMockUser
    @DisplayName("GET /chores/getAll/{houseCode} - Should return 404 when chores do not exist")
    public void getAll_WhenDoesNotExist_Returns404() throws Exception {
        when(choreService.getAllChoreByApartment("123"))
                .thenThrow(new ChoreServiceException("No Chores found", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/chores/getAll/123"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("No Chores found")));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /chores/addWithSmartAssign - Should return 404 when no roommates found")
    public void addSmart_WhenNoRoommates_Returns404() throws Exception, UserServiceException {
        // This method in your controller calls userService.getUsersForApartment
        when(userService.getUsersForApartment("123")).thenReturn(Collections.emptyList());

        ChoreRequests request = new ChoreRequests();
        request.setTaskName("Clean");

        mockMvc.perform(post("/chores/addWithSmartAssign")
                        .param("houseCode", "123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("No roommates found")));
    }
}