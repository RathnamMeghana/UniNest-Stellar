package UniNest.Backend.flow;

import UniNest.Backend.config.SecurityConfig;
import UniNest.Backend.controller.ApartmentController;
import UniNest.Backend.controller.GlobalExceptionHandler;
import UniNest.Backend.dto.ApartmentRequests;
import UniNest.Backend.service.ApartmentService;
import UniNest.Backend.service.RoomService;
import UniNest.Backend.service.UserService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
@WebMvcTest(controllers = ApartmentController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = true)
class ApartmentSecurityFlowTest {

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

    private ApartmentRequests validRequest;

    @BeforeEach
    void setup() {
        validRequest = new ApartmentRequests();
        validRequest.setBuildingId("BUILDING_001");
        validRequest.setName("Sunset Apartments");
        validRequest.setTotalRooms("5");
        validRequest.setLandlordId("LANDLORD_001");
        validRequest.setDescription("Luxury apartment");
        validRequest.setRentPrice(1200.0);
        validRequest.setActive(true);

        when(apartmentService.createApartment(any(ApartmentRequests.class))).thenReturn("SUCCESS_MOCK_CALLED");
    }


    // Happy path — LETTINGAGENT
    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    void createApartment_AsAgent_Allowed() throws Exception {
        mockMvc.perform(post("/apartments/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("SUCCESS_MOCK_CALLED"));

        verify(apartmentService, times(1)).createApartment(any(ApartmentRequests.class));
    }

    // Wrong role — TENANT
    @Test
    @WithMockUser(roles = "TENANT")
    void createApartment_AsTenant_Forbidden() throws Exception {
        mockMvc.perform(post("/apartments/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());

        verify(apartmentService, never()).createApartment(any());
    }

    // Wrong role — USER
    @Test
    @WithMockUser(roles = "USER")
    void createApartment_NoRole_Forbidden() throws Exception {
        mockMvc.perform(post("/apartments/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());

        verify(apartmentService, never()).createApartment(any());
    }

    // No authentication
    @Test
    void createApartment_NoAuth_Forbidden() throws Exception {
        mockMvc.perform(post("/apartments/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    // Invalid input — missing required field
    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    void createApartment_InvalidInput_BadRequest() throws Exception {
        validRequest.setName(""); // violates @NotBlank

        mockMvc.perform(post("/apartments/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }
}
