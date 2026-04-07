package UniNest.Backend.flow;

import UniNest.Backend.config.SecurityConfig;
import UniNest.Backend.controller.BuildingController;
import UniNest.Backend.controller.GlobalExceptionHandler;
import UniNest.Backend.dto.BuildingRequest;
import UniNest.Backend.security.FirebaseTokenFilter;
import UniNest.Backend.service.BuildingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(controllers = BuildingController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = true)
@EnableMethodSecurity


public class BuildingSecurityFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BuildingService buildingService;


    private BuildingRequest validRequest;



    @BeforeEach
    public void setup() throws Exception {
        validRequest = new BuildingRequest();
        validRequest.setName("The Plaza");
        validRequest.setAddressLine1("123 Main St");
        validRequest.setCity("Galway");
        validRequest.setPostcode("H91");
        validRequest.setCountry("Ireland");
        validRequest.setLandlordId("LANDLORD_001");
        validRequest.setActive(true);

        when(buildingService.createBuilding(any(BuildingRequest.class))).thenReturn("SUCCESS_MOCK_CALLED");
    }

    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    public void createBuilding_AsAgent_Allowed() throws Exception {
        mockMvc.perform(post("/buildings/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("SUCCESS_MOCK_CALLED")); // Verifies controller was hit

        verify(buildingService, times(1)).createBuilding(any(BuildingRequest.class));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    public void createBuilding_AsTenant_Forbidden() throws Exception {
        mockMvc.perform(post("/buildings/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isForbidden()); // Should now return 403

        verify(buildingService, never()).createBuilding(any());
    }


    @Test
    public void createBuilding_NoAuth_Forbidden() throws Exception {
        mockMvc.perform(post("/buildings/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    public void createBuilding_InvalidInput_BadRequest() throws Exception {
        validRequest.setName(""); // invalid

        mockMvc.perform(post("/buildings/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("Security: Tenant attempting to delete a building returns 403 Forbidden")
    public void deleteBuilding_AsTenant_Forbidden() throws Exception {
        mockMvc.perform(delete("/buildings/BLDG-123")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        // Verify the destructive service method was never even called
        verify(buildingService, never()).deleteBuilding(anyString());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("Security: Tenant attempting to rename a building returns 403 Forbidden")
    public void updateName_AsTenant_Forbidden() throws Exception {
        UniNest.Backend.dto.NameUpdateRequest updateRequest = new UniNest.Backend.dto.NameUpdateRequest();
        updateRequest.setName("Malicious Rename");

        mockMvc.perform(put("/buildings/BLDG-123/name")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("Privacy: Tenant attempting to fetch all buildings returns 403 Forbidden")
    public void getAllBuildings_AsTenant_Forbidden() throws Exception {
        mockMvc.perform(get("/buildings/getAll"))
                .andExpect(status().isForbidden());
    }
}