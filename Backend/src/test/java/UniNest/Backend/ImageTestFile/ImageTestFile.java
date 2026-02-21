package UniNest.Backend.ImageTestFile;

import UniNest.Backend.config.SecurityConfig;
import UniNest.Backend.controller.BuildingController;
import UniNest.Backend.controller.GlobalExceptionHandler;
import UniNest.Backend.dto.BuildingRequest;
import UniNest.Backend.exception.BuildingServiceException;
import UniNest.Backend.security.FirebaseTokenFilter;
import UniNest.Backend.service.BuildingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(controllers = BuildingController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = true)
public class ImageTestFile {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BuildingService buildingService;

    @MockBean
    private FirebaseTokenFilter firebaseTokenFilter;

    @BeforeEach
    public void setup() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(firebaseTokenFilter).doFilter(any(), any(), any());
    }

    private BuildingRequest getValidRequest() {
        BuildingRequest request = new BuildingRequest();
        request.setName("Test Building");
        request.setLandlordId("AGENT_123");
        request.setAddressLine1("123 Test Lane");
        request.setCity("Galway");
        request.setPostcode("H91");
        request.setCountry("Ireland");
        request.setActive(true); // Ensure Boolean fields are not null
        return request;
    }

    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    @DisplayName("Image Flow: Base64 string integrity is preserved")
    public void createBuilding_ImageIntegrity_Check() throws Exception {
        String validBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+ip1sAAAAASUVORK5CYII=";

        BuildingRequest request = getValidRequest();
        request.setImageUrl(validBase64);

        when(buildingService.createBuilding(any())).thenReturn("Success");

        mockMvc.perform(post("/buildings/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print()) // If this still returns 400, look at the "Resolved Exception" in the console
                .andExpect(status().isOk());

        ArgumentCaptor<BuildingRequest> captor = ArgumentCaptor.forClass(BuildingRequest.class);
        verify(buildingService).createBuilding(captor.capture());

        assertEquals(validBase64, captor.getValue().getImageUrl());
    }

    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    @DisplayName("Image Flow: Returns 500 error when image string is too large")
    public void createBuilding_ImageTooLarge_Returns500() throws Exception {
        BuildingRequest request = getValidRequest();
        request.setImageUrl("a".repeat(1100000));

        when(buildingService.createBuilding(any()))
                .thenThrow(new BuildingServiceException("Firestore rejected the write. Check if image is too large.", HttpStatus.INTERNAL_SERVER_ERROR));

        mockMvc.perform(post("/buildings/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("too large")));
    }

    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    @DisplayName("Image Flow: Building creation succeeds when image is null")
    public void createBuilding_NullImage_Allowed() throws Exception {
        BuildingRequest request = getValidRequest();
        request.setImageUrl(null); // Explicitly null

        when(buildingService.createBuilding(any())).thenReturn("Success");

        mockMvc.perform(post("/buildings/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(buildingService).createBuilding(argThat(r -> r.getImageUrl() == null));
    }

    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    @DisplayName("Image Flow: GET building returns the full Base64 string")
    public void getBuilding_ReturnsBase64Data() throws Exception {
        String expectedBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+ip1sAAAAASUVORK5CYII=";

        UniNest.Backend.model.Building mockBuilding = new UniNest.Backend.model.Building();
        mockBuilding.setName("Image Return Test");
        mockBuilding.setImageUrl(expectedBase64);

        when(buildingService.getBuildingById("B123")).thenReturn(mockBuilding);

        mockMvc.perform(get("/buildings/B123"))
                .andExpect(status().isOk())
                // Verify the JSON contains the exact same Base64 string
                .andExpect(jsonPath("$.imageUrl").value(expectedBase64));
    }

    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    @DisplayName("Image Flow: Dangerous tags in image field are still blocked")
    public void createBuilding_XssInImageField_IsCleaned() throws Exception {
        BuildingRequest request = getValidRequest();
        // A string that looks like Base64 but has a script tag at the end
        String dangerousInput = "iVBORw0KGgoAAAAN<script>alert('xss')</script>";
        request.setImageUrl(dangerousInput);

        when(buildingService.createBuilding(any())).thenReturn("Success");

        mockMvc.perform(post("/buildings/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<BuildingRequest> captor = ArgumentCaptor.forClass(BuildingRequest.class);
        verify(buildingService).createBuilding(captor.capture());

        // Verify that the <script> part was removed
        assertFalse(captor.getValue().getImageUrl().contains("<script>"),
                "The image field should still strip HTML tags even if it allows Base64 characters.");
    }
}