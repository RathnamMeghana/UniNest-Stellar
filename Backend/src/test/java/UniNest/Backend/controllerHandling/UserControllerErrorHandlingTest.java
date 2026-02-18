package UniNest.Backend.controllerHandling;

import UniNest.Backend.controller.ApartmentController;
import UniNest.Backend.controller.GlobalExceptionHandler;
import UniNest.Backend.exception.UserServiceException;
import UniNest.Backend.service.UserService;
import UniNest.Backend.service.ApartmentService; // Import this
import UniNest.Backend.service.RoomService;      // Import this
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ApartmentController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    // --- ADD THESE TWO MOCKBEANS TO FIX THE ERROR ---
    @MockBean
    private ApartmentService apartmentService;

    @MockBean
    private RoomService roomService;
    // ------------------------------------------------

    @Test
    @WithMockUser
    @DisplayName("GET /apartments/{houseCode}/users - Should return 500 when Firestore fails")
    public void getUsers_WhenServiceFails_Returns500() throws Exception {
        when(userService.getUsersForApartment("APT123"))
                .thenThrow(new UserServiceException("Firestore query failed", HttpStatus.INTERNAL_SERVER_ERROR));

        mockMvc.perform(get("/apartments/APT123/users"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Firestore query failed")));
    }
    @Test
    @WithMockUser
    @DisplayName("GET /apartments/{houseCode}/users - Should return 400 when code is invalid")
    public void getUsers_WhenCodeInvalid_Returns400() throws Exception {
        // The service throws the exception
        when(userService.getUsersForApartment(" "))
                .thenThrow(new IllegalArgumentException("houseCode cannot be null or empty"));

        mockMvc.perform(get("/apartments/ /users"))

                .andExpect(status().isBadRequest()) // Now this will catch the 400
                .andExpect(content().string(containsString("cannot be null or empty")));
    }}