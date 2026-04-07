package UniNest.Backend.controllerHandling;

import UniNest.Backend.controller.ApartmentController;
import UniNest.Backend.controller.GlobalExceptionHandler;
import UniNest.Backend.dto.ApartmentRequests;
import UniNest.Backend.dto.BulkApartmentWithRoomsRequest;
import UniNest.Backend.dto.RoomRequests;
import UniNest.Backend.exception.ApartmentServiceException;
import UniNest.Backend.exception.TenantNotFoundException;
import UniNest.Backend.exception.UserServiceException;
import UniNest.Backend.service.ApartmentService;
import UniNest.Backend.service.UserService;
import UniNest.Backend.service.RoomService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ApartmentController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
public class ApartmentControllerErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApartmentService apartmentService;

    @MockBean
    private UserService userService;

    @MockBean
    private RoomService roomService;



    // --- 500 ERROR TEST ---
    @Test
    @WithMockUser
    @DisplayName("GET /apartments/getAll - Should return 500 when Firestore fails")
    public void getAllApartments_WhenServiceFails_Returns500() throws Exception {
        when(apartmentService.getAllApartments())
                .thenThrow(new ApartmentServiceException("Firestore unavailable", HttpStatus.INTERNAL_SERVER_ERROR));

        mockMvc.perform(get("/apartments/getAll"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Firestore unavailable")));
    }

    // --- 400 VALIDATION TEST ---
    @Test
    @WithMockUser
    @DisplayName("POST /apartments/create - Should return 400 when name is blank")
    public void createApartment_WhenValidationFails_Returns400() throws Exception {
        ApartmentRequests invalidRequest = new ApartmentRequests();
        invalidRequest.setName(""); // Invalid

        mockMvc.perform(post("/apartments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());
    }

    // --- 403 AUTHORIZATION TEST ---
    @Test
    @WithMockUser
    @DisplayName("POST /apartments/create - Should return 403 when user is not a landlord")
    public void createApartment_WhenNotAuthorized_Returns403() throws Exception {
        ApartmentRequests request = new ApartmentRequests();
        request.setName("Valid Name");
        request.setBuildingId("B1");
        request.setTotalRooms("10");
        request.setLandlordId("L1");
        request.setDescription("Valid Desc");
        request.setActive(true);

        when(apartmentService.createApartment(any(ApartmentRequests.class)))
                .thenThrow(new ApartmentServiceException("User exists but is not authorized as a Landlord.", HttpStatus.FORBIDDEN));

        mockMvc.perform(post("/apartments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("authorized as a Landlord")));
    }

    // --- 404 TENANT NOT FOUND TEST ---
    @Test
    @WithMockUser
    @DisplayName("DELETE /apartments/tenants/{email}/remove - Should return 404 when tenant missing")
    public void removeTenant_WhenNotFound_Returns404() throws Exception {
        String email = "missing@test.com";
        doThrow(new TenantNotFoundException("User not found with email: " + email))
                .when(apartmentService).removeTenantFromApartment(email);

        mockMvc.perform(delete("/apartments/tenants/" + email + "/remove"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("User not found")));
    }

    // --- 404 EMPTY USER LIST TEST ---
    @Test
    @WithMockUser
    @DisplayName("GET /apartments/{houseCode}/users - Should return 404 when list empty")
    public void getUsers_WhenEmpty_Returns404() throws Exception, UserServiceException {
        when(userService.getUsersForApartment("EMPTY-CODE")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/apartments/EMPTY-CODE/users"))
                .andExpect(status().isNotFound());
    }

    // --- 400 NEGATIVE RENT TEST ---
    @Test
    @WithMockUser
    @DisplayName("POST /apartments/create - Should return 400 when rent is negative")
    public void createApartment_WhenRentNegative_Returns400() throws Exception {
        ApartmentRequests request = new ApartmentRequests();
        request.setName("Valid Name");
        request.setBuildingId("B1");
        request.setTotalRooms("10");
        request.setLandlordId("L1");
        request.setDescription("Valid Desc");
        request.setActive(true);
        request.setRentPrice(-50.0); // Invalid

        mockMvc.perform(post("/apartments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.rentPrice").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /apartments/{houseCode}/addRoom - Should return 400 when label is empty")
    public void addRoom_WhenValidationFails_Returns400() throws Exception {
        RoomRequests invalidRoom = new RoomRequests();
        invalidRoom.setType("Bedroom");
        invalidRoom.setLabel("");

        mockMvc.perform(post("/apartments/APT123/addRoom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRoom)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.label").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /apartments/bulkWithRooms - Should return 404 when landlord missing")
    public void bulkWithRooms_WhenLandlordNotFound_Returns404() throws Exception {
        BulkApartmentWithRoomsRequest request = new BulkApartmentWithRoomsRequest();
        request.setLandlordId("ID");
        request.setBuildingId("B1");
        request.setApartmentCount(5);
        request.setRoomTemplate(Collections.singletonMap("Kitchen", 1));

        doThrow(new ApartmentServiceException("Landlord not found", HttpStatus.NOT_FOUND))
                .when(apartmentService).createApartmentsWithRooms(any(BulkApartmentWithRoomsRequest.class));

        mockMvc.perform(post("/apartments/bulkWithRooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Landlord not found")));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /apartments/{houseCode}/addRoom - Should return 500 on unexpected exception")
    public void addRoom_WhenServiceThrowsUnexpected_Returns500() throws Exception {
        RoomRequests validRoom = new RoomRequests();
        validRoom.setType("Studio");
        validRoom.setLabel("Room 1");

        when(roomService.addRoom(anyString(), any(RoomRequests.class)))
                .thenThrow(new RuntimeException("Database timeout"));

        mockMvc.perform(post("/apartments/APT123/addRoom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRoom)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Error adding room")));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /apartments/{houseCode} - Should return 500 when deletion fails")
    public void deleteApartment_WhenFailure_Returns500() throws Exception {
        doThrow(new ApartmentServiceException("Failed to delete apartment", HttpStatus.INTERNAL_SERVER_ERROR))
                .when(apartmentService).deleteApartment(anyString());

        mockMvc.perform(delete("/apartments/APT-123"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Failed to delete apartment")));
    }
}