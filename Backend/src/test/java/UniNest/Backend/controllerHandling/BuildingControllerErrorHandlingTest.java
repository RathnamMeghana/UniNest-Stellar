package UniNest.Backend.controllerHandling;

import UniNest.Backend.controller.BuildingController;
import UniNest.Backend.controller.GlobalExceptionHandler;
import UniNest.Backend.dto.BuildingRequest;
import UniNest.Backend.exception.BuildingServiceException;
import UniNest.Backend.service.BuildingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {BuildingController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false) // Bypasses security for unit testing error logic
public class BuildingControllerErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BuildingService buildingService;

    @BeforeEach
    public void setup() {
        Mockito.reset(buildingService);
    }

    // --- Tests for BuildingServiceException (Business Logic) ---

    @Test
    @DisplayName("GET /buildings/{id} - Should return 404 when building does not exist")
    public void getBuilding_WhenDoesNotExist_Returns404() throws Exception {
        when(buildingService.getBuildingById(anyString()))
                .thenThrow(new BuildingServiceException("Building not found with ID: 123", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/buildings/123"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Building not found with ID: 123"));
    }

    @Test
    @DisplayName("GET /buildings/{id} - Should return 403 when user is not authorized")
    public void getBuilding_WhenForbidden_Returns403() throws Exception {
        when(buildingService.getBuildingById(anyString()))
                .thenThrow(new BuildingServiceException("User not authorized to view this building", HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/buildings/123"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("User not authorized to view this building"));
    }

    @Test
    @DisplayName("GET /buildings/getAll - Should return 500 when database fails")
    public void getAllBuildings_WhenServiceFails_Returns500() throws Exception {
        when(buildingService.getAllBuildings())
                .thenThrow(new BuildingServiceException("Firestore connection failed", HttpStatus.INTERNAL_SERVER_ERROR));

        mockMvc.perform(get("/buildings/getAll"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Firestore connection failed"));
    }

    // --- Tests for MethodArgumentNotValidException (Validation) ---

    @Test
    @DisplayName("POST /buildings/create - Should return 400 when request body is invalid")
    public void createBuilding_WhenValidationFails_Returns400() throws Exception {
        // Create an invalid request (e.g., missing required fields like name/address)
        BuildingRequest invalidRequest = new BuildingRequest();
        // Assuming your DTO has @NotBlank or @NotNull on fields, sending empty values triggers the error

        mockMvc.perform(post("/buildings/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                // Verify the handler returns a JSON map of errors as defined in your GlobalExceptionHandler
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.addressLine1").exists());
    }

    // --- Tests for Other API Endpoints Error Propagation ---

    @Test
    @DisplayName("GET /buildings/byLandlord - Should return 400 when landlordId is missing")
    public void getByLandlord_WhenParamMissing_Returns400() throws Exception {
        // Missing the required @RequestParam "landlordId"
        mockMvc.perform(get("/buildings/byLandlord"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /buildings/create - Should return 500 when Firestore write fails")
    public void createBuilding_WhenExecutionFails_Returns500() throws Exception {
        // Create a request so it passes the @Valid check
        BuildingRequest validRequest = new BuildingRequest();
        validRequest.setName("Grand Heights");
        validRequest.setAddressLine1("123 Park Lane");
        validRequest.setCity("London");
        validRequest.setPostcode("W1K 7AA");
        validRequest.setCountry("UK");
        validRequest.setLandlordId("LAND-001");
        validRequest.setActive(true);


        when(buildingService.createBuilding(any(BuildingRequest.class)))
                .thenThrow(new BuildingServiceException("Firestore rejected the write", HttpStatus.INTERNAL_SERVER_ERROR));

        // Perform the request
        mockMvc.perform(post("/buildings/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError()) // Now it will correctly receive the 500
                .andExpect(content().string("Firestore rejected the write"));
    }
}