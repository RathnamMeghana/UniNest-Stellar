package UniNest.Backend.security;

import UniNest.Backend.config.SecurityConfig;
import UniNest.Backend.controller.BuildingController;
import UniNest.Backend.controller.GlobalExceptionHandler;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BuildingController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
public class BuildingSanitizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BuildingService buildingService;

    // 1. You MUST mock the filter to bypass the Firebase SDK check
    @MockBean
    private FirebaseTokenFilter firebaseTokenFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private String unsafeBuildingJson;

    @BeforeEach
    void setup() throws Exception {
        // 2. This logic tells the filter to just "pass the request through"
        // so that @WithMockUser or SecurityMockMvcRequestPostProcessors can work.
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(firebaseTokenFilter).doFilter(any(), any(), any());

        unsafeBuildingJson = """
                {
                    "name": "<b>Building A</b>",
                    "addressLine1": "<script>alert('xss')</script> 123 Street",
                    "city": "<i>CityName</i>",
                    "postcode": "12345",
                    "country": "Ireland",
                    "landlordId": "landlord123",
                    "active": true,
                    "imageUrl": "http://example.com/image.jpg"
                }
                """;

        Mockito.when(buildingService.createBuilding(any(BuildingRequest.class)))
                .thenReturn("Building created successfully");
    }

    @Test
    void createBuilding_BuildingSanitized() throws Exception {
        mockMvc.perform(post("/buildings/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unsafeBuildingJson)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()) // CSRF is often required for POST
                        .with(SecurityMockMvcRequestPostProcessors.user("lettingAgent")
                                .authorities(new SimpleGrantedAuthority("ROLE_LETTINGAGENT"))))
                .andExpect(status().isOk());

        ArgumentCaptor<BuildingRequest> captor = ArgumentCaptor.forClass(BuildingRequest.class);
        verify(buildingService).createBuilding(captor.capture());

        BuildingRequest sanitizedRequest = captor.getValue();

        // 3. Verify that Jsoup cleaned the fields
        assertFalse(sanitizedRequest.getName().contains("<b>"), "Name should not contain HTML tags");
        assertFalse(sanitizedRequest.getAddressLine1().contains("<script>"), "AddressLine1 should not contain scripts");
        assertFalse(sanitizedRequest.getCity().contains("<i>"), "City should not contain italic tags");
    }
}