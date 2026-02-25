package UniNest.Backend.security;

import UniNest.Backend.controller.ApartmentController;
import UniNest.Backend.controller.GlobalExceptionHandler;
import UniNest.Backend.dto.ApartmentRequests;
import UniNest.Backend.service.ApartmentService;
import UniNest.Backend.service.RoomService;
import UniNest.Backend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ApartmentController.class)
// addFilters = false is the "nuclear option" to ensure security doesn't block the test
@AutoConfigureMockMvc(addFilters = false)
public class ApartmentSanitizationIntegrationTest {

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

    // Even with addFilters=false, we keep the mock to satisfy dependency injection
    @MockBean
    private FirebaseTokenFilter firebaseTokenFilter;

    @MockBean
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    void createApartment_ApartmentSanitized() throws Exception {
        // 1. Prepare Unsafe Request
        ApartmentRequests request = new ApartmentRequests();
        request.setBuildingId("build<script>alert('xss')</script>");
        request.setName("Apt <b>101</b>");
        request.setTotalRooms("3");
        request.setLandlordId("land456");
        request.setDescription("Clean <img src=x onerror=alert(1)>");
        request.setRentPrice(500.0);
        request.setActive(true);

        // Stub service
        when(apartmentService.createApartment(any(ApartmentRequests.class)))
                .thenReturn("Success");

        // 2. Perform Post
        mockMvc.perform(post("/apartments/create")
                        .with(csrf()) // Still include CSRF for good measure
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        // 3. Capture the Argument
        ArgumentCaptor<ApartmentRequests> captor = ArgumentCaptor.forClass(ApartmentRequests.class);

        // This is where it was failing (Line 93)
        verify(apartmentService, times(1)).createApartment(captor.capture());

        ApartmentRequests result = captor.getValue();

        // 4. Verify Sanitization Logic
        // These rely on your SanitizationUtil stripping HTML tags
        assertFalse(result.getName().contains("<b>"), "HTML tags should be stripped from Name");
        assertFalse(result.getBuildingId().contains("<script>"), "Scripts should be stripped from BuildingID");
        assertFalse(result.getDescription().contains("<img"), "Images should be stripped from Description");

        // Verify text content remains
        assertTrue(result.getName().contains("Apt 101"));

        // Verify the logic inside request.sanitize() ran
        assertNotNull(result.getCreatedAt(), "CreatedAt should be initialized by sanitize()");
    }
}