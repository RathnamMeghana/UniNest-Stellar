package UniNest.Backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
// CRITICAL: Import csrf to handle POST/DELETE requests in tests
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import UniNest.Backend.dto.BulkApartmentWithRoomsRequest;
import UniNest.Backend.exception.ApartmentServiceException;
import UniNest.Backend.exception.UserServiceException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

// YOUR IMPORTS
import UniNest.Backend.config.SecurityConfig;
import UniNest.Backend.dto.ApartmentRequests;
import UniNest.Backend.dto.RoomRequests;
import UniNest.Backend.model.Apartment;
import UniNest.Backend.model.User;
import UniNest.Backend.service.ApartmentService;
import UniNest.Backend.service.RoomService;
import UniNest.Backend.service.UserService;
import UniNest.Backend.security.FirebaseTokenFilter;

@WebMvcTest(ApartmentController.class)
@Import(SecurityConfig.class) // Load  Security Logic
class ApartmentControllerTest {

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

    // Mock the Filter so we don't need real Firebase credentials
    @MockBean
    private FirebaseTokenFilter firebaseTokenFilter;


    // SETUP: BYPASS THE FILTER
    @BeforeEach
    void setup() throws Exception {
        // This tells the Mock Filter to "allow" the request to pass through to the Controller
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response); // Continue the chain
            return null;
        }).when(firebaseTokenFilter).doFilter(any(), any(), any());
    }


    // HELPER: CREATE VALID DATA
    // this to avoid "400 Bad Request" errors caused by missing fields
    private ApartmentRequests getValidApartmentRequest() {
        ApartmentRequests request = new ApartmentRequests();
        request.setName("Luxury Apartment");
        request.setBuildingId("bldg-001");
        request.setCode("APT-101");
        request.setLandlordId("landlord-xyz");
        request.setDescription("A beautiful place to live");
        request.setTotalRooms("3");
        request.setRentPrice(1500.00);
        request.setActive(true);
        return request;
    }

    // Agent creates an Apartment
    @Test
    @WithMockUser(username = "agentUser", roles = {"LETTINGAGENT"})

    void createApartment_asAgent_success() throws Exception {
        ApartmentRequests request = getValidApartmentRequest(); // Use helper!

        when(apartmentService.createApartment(any(ApartmentRequests.class)))
                .thenReturn("Apartment created");

        mockMvc.perform(post("/apartments/create")
                        .with(csrf()) // Required for POST
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Apartment created"));
    }

    // Tenant tries to create Apartment (Should Fail)
    @Test
    @WithMockUser(username = "sneakyTenant", roles = {"TENANT"})
    void createApartment_asTenant_shouldFail() throws Exception {
        //  MUST send valid data, otherwise we get 400 (Bad Request) instead of 403 (Forbidden)
        ApartmentRequests request = getValidApartmentRequest();

        mockMvc.perform(post("/apartments/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // 403
    }

    //  Missing fields (Should be Bad Request)
    @Test
    @WithMockUser(username = "agentUser", roles = {"LETTINGAGENT"})
    void createApartment_validationError() throws Exception {
        ApartmentRequests request = new ApartmentRequests(); // Empty object

        mockMvc.perform(post("/apartments/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // 400
    }

    //  Tenant Gets All Apartments
    @Test
    @WithMockUser(username = "tenantUser", roles = {"TENANT"})
    void getAllApartments_asTenant_success() throws Exception {

        Apartment apartment = new Apartment();
        apartment.setName("Apartment A");

        when(apartmentService.getAllApartments())
                .thenReturn(List.of(apartment));

        mockMvc.perform(get("/apartments/getAll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Apartment A"));
    }

    // Agent Adds Room
    @Test
    @WithMockUser(username = "agentUser", roles = {"LETTINGAGENT"})
    void addRoom_asAgent_success() throws Exception {
        RoomRequests request = new RoomRequests();
        request.setType("Single");
        request.setLabel("Room 101");
        // Add other required fields if RoomRequests has validation annotations

        when(roomService.addRoom(eq("A1"), any(RoomRequests.class)))
                .thenReturn("room123");

        mockMvc.perform(post("/apartments/A1/addRoom")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Room added with ID: room123"));
    }

    // Tenant Adds Room Should Fail
    @Test
    @WithMockUser(username = "sneakyTenant", roles = {"TENANT"})
    void addRoom_asTenant_shouldFail() throws Exception {
        RoomRequests request = new RoomRequests();
        request.setType("Single");
        request.setLabel("Room 101");

        mockMvc.perform(post("/apartments/A1/addRoom")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    //  Get Users
    @Test
    @WithMockUser(username = "agentUser", roles = {"LETTINGAGENT"})
    void getUsersByApartment_success() throws Exception, UserServiceException {
        User user = new User();
        user.setEmail("test@uninest.com");

        when(userService.getUsersForApartment("A1"))
                .thenReturn(List.of(user));

        mockMvc.perform(get("/apartments/A1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("test@uninest.com"));
    }

    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    @DisplayName("Logic Flow: Bulk apartment creation with zero count returns 400")
    public void bulkCreate_ZeroCount_Returns400() throws Exception {
        BulkApartmentWithRoomsRequest request = new BulkApartmentWithRoomsRequest();
        request.setApartmentCount(0); // Logically invalid

        // Assuming you add @Min(1) to your DTO
        mockMvc.perform(post("/apartments/bulkWithRooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    @DisplayName("POST /apartments/bulkWithRooms - Should return 403 when landlord is invalid")
    public void bulkCreate_WhenLandlordInvalid_Returns403() throws Exception {
        //  Create a request that is VALID at the DTO level
        // (passes @NotBlank and @NotEmpty)
        BulkApartmentWithRoomsRequest request = new BulkApartmentWithRoomsRequest();
        request.setLandlordId("INVALID_LANDLORD_ID");
        request.setBuildingId("BUILDING_123");
        request.setApartmentCount(5);

        // Required field: roomTemplate
        java.util.Map<String, Integer> template = new java.util.HashMap<>();
        template.put("Bedroom", 2);
        request.setRoomTemplate(template);

        // Mock the service to throw the 403 error
        // only be reached if the DTO passes validation
        doThrow(new ApartmentServiceException("User exists but is not authorized as a Landlord.", HttpStatus.FORBIDDEN))
                .when(apartmentService).createApartmentsWithRooms(any(BulkApartmentWithRoomsRequest.class));
        mockMvc.perform(post("/apartments/bulkWithRooms")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"LETTINGAGENT"})
    @DisplayName("DELETE /apartments/{houseCode} - Success as Agent")
    void deleteApartment_asAgent_success() throws Exception {
        mockMvc.perform(delete("/apartments/APT-123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Apartment deleted and tenants unassigned successfully"));

        verify(apartmentService, times(1)).deleteApartment("APT-123");
    }
}