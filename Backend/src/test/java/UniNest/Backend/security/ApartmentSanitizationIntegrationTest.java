package UniNest.Backend.security;

import UniNest.Backend.controller.ApartmentController;
import UniNest.Backend.dto.ApartmentRequests;
import UniNest.Backend.service.ApartmentService;
import UniNest.Backend.service.RoomService;
import UniNest.Backend.service.UserService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.Timestamp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApartmentController.class)
public class ApartmentSanitizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApartmentService apartmentService;

    @MockBean
    private UserService userService;

    @MockBean
    private RoomService roomService;

    @Autowired
    private ObjectMapper objectMapper;

    private String unsafeApartmentJson;

    @BeforeEach
    void setup() {
        // Unsafe apartment input for sanitization testing
        unsafeApartmentJson = """
{
  "buildingId": "building123",
  "name": "Test Apartment <script>alert('xss')</script>",
  "totalRooms": "3",
  "landlordId": "landlord456",
  "description": "Nice apartment <b>bold</b>",
  "active": true,
  "rentPrice": 1200.0
}
""";


        // Mock the service call to return a fixed string
        Mockito.when(apartmentService.createApartment(Mockito.any(ApartmentRequests.class)))
                .thenReturn("Apartment Created");
    }

    @Test
    void createApartment_ApartmentSanitized() throws Exception {
        mockMvc.perform(post("/apartments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unsafeApartmentJson)
                        .with(SecurityMockMvcRequestPostProcessors.user("lettingAgent")
                                .roles("LETTINGAGENT")) // automatically adds ROLE_ prefix
                        .with(SecurityMockMvcRequestPostProcessors.csrf())) // required for POST
                .andExpect(status().isOk());

        ArgumentCaptor<ApartmentRequests> captor = ArgumentCaptor.forClass(ApartmentRequests.class);
        verify(apartmentService).createApartment(captor.capture());

        ApartmentRequests sanitizedRequest = captor.getValue();
        assertFalse(sanitizedRequest.getName().contains("<"));
        assertFalse(sanitizedRequest.getDescription().contains("<"));
        assertFalse(sanitizedRequest.getBuildingId().contains("<"));
        assert sanitizedRequest.getCreatedAt() != null;
    }

}
