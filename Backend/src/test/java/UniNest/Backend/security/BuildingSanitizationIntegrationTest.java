package UniNest.Backend.security;

import UniNest.Backend.controller.BuildingController;
import UniNest.Backend.dto.BuildingRequest;
import UniNest.Backend.service.BuildingService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Import your security configuration so PreAuthorize works
import UniNest.Backend.config.SecurityConfig;

@WebMvcTest(BuildingController.class)
@Import(SecurityConfig.class) // ensures method security is loaded
@EnableMethodSecurity(prePostEnabled = true)
public class BuildingSanitizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BuildingService buildingService;

    @Autowired
    private ObjectMapper objectMapper;

    private String unsafeBuildingJson;

    @BeforeEach
    void setup() {
        unsafeBuildingJson = """
                {
                    "name": "<b>Building A</b>",
                    "addressLine1": "<script>alert('xss')</script> 123 Street",
                    "city": "<i>CityName</i>",
                    "postcode": "12345",
                    "country": "CountryName",
                    "landlordId": "landlord123",
                    "active": true,
                    "imageUrl": "http://example.com/image.jpg"
                }
                """;

        Mockito.when(buildingService.createBuilding(Mockito.any(BuildingRequest.class)))
                .thenReturn("Building created successfully via Admin API");
    }

    @Test
    void createBuilding_BuildingSanitized() throws Exception {
        mockMvc.perform(post("/buildings/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unsafeBuildingJson)
                        // mock user with correct role
                        .with(SecurityMockMvcRequestPostProcessors.user("lettingAgent")
                                .authorities(new SimpleGrantedAuthority("ROLE_LETTINGAGENT"))))
                .andExpect(status().isOk());

        // Capture the argument passed to the service
        ArgumentCaptor<BuildingRequest> captor = ArgumentCaptor.forClass(BuildingRequest.class);
        verify(buildingService).createBuilding(captor.capture());

        BuildingRequest sanitizedRequest = captor.getValue();

        // Verify sanitization
        assertFalse(sanitizedRequest.getName().contains("<"), "Name should be sanitized");
        assertFalse(sanitizedRequest.getAddressLine1().contains("<"), "AddressLine1 should be sanitized");
        assertFalse(sanitizedRequest.getCity().contains("<"), "City should be sanitized");
    }
}
