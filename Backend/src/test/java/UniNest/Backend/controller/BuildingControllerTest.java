package UniNest.Backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import UniNest.Backend.config.SecurityConfig;
import UniNest.Backend.dto.BuildingRequest;
import UniNest.Backend.model.Building;
import UniNest.Backend.security.FirebaseTokenFilter;
import UniNest.Backend.service.BuildingService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import UniNest.Backend.dto.NameUpdateRequest;

@WebMvcTest(BuildingController.class)
@Import(SecurityConfig.class)
class BuildingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BuildingService buildingService;

    @MockBean
    private FirebaseTokenFilter firebaseTokenFilter;

    // ------------------------------------------------------------------------
    // BYPASS FIREBASE FILTER
    // ------------------------------------------------------------------------
    @BeforeEach
    void setup() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(firebaseTokenFilter).doFilter(any(), any(), any());
    }

    // ------------------------------------------------------------------------
    // HELPER METHOD - VALID REQUEST
    // ------------------------------------------------------------------------
    private BuildingRequest getValidBuildingRequest() {
        BuildingRequest request = new BuildingRequest();
        request.setName("Building A");
        request.setAddressLine1("123 Main St");
        request.setCity("Dundalk");
        request.setPostcode("A92 333");
        request.setCountry("Ireland");
        request.setLandlordId("landlord123");
        request.setActive(true);
        return request;
    }

    // ========================================================================
    // TESTS
    // ========================================================================

    // Letting Agent Creates Building
    @Test
    @WithMockUser(username = "agentUser", roles = {"LETTINGAGENT"})
    void createBuilding_asAgent_success() throws Exception {

        BuildingRequest request = getValidBuildingRequest();

        when(buildingService.createBuilding(any(BuildingRequest.class)))
                .thenReturn("Building created");

        mockMvc.perform(post("/buildings/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Building created"));
    }

    // Tenant Tries to Create Building (Should Fail)
    @Test
    @WithMockUser(username = "tenantUser", roles = {"TENANT"})
    void createBuilding_asTenant_shouldFail() throws Exception {

        BuildingRequest request = getValidBuildingRequest();

        mockMvc.perform(post("/buildings/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // 403
    }

    // Missing Fields (Should Return 400)
    @Test
    @WithMockUser(username = "agentUser", roles = {"LETTINGAGENT"})
    void createBuilding_validationError() throws Exception {

        BuildingRequest request = new BuildingRequest(); // empty

        mockMvc.perform(post("/buildings/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    // Agent Gets All Buildings
    @Test
    @WithMockUser(username = "agentUser", roles = {"LETTINGAGENT"})
    void getAllBuildings_asAgent_success() throws Exception {

        Building building = new Building();
        building.setName("Building A");

        when(buildingService.getAllBuildings())
                .thenReturn(List.of(building));

        mockMvc.perform(get("/buildings/getAll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Building A"));
    }

    @Test
    @WithMockUser(roles = {"LETTINGAGENT"})
    @DisplayName("PUT /buildings/{id}/name - Success as Agent")
    void updateBuildingName_asAgent_success() throws Exception {
        NameUpdateRequest updateRequest = new NameUpdateRequest();
        updateRequest.setName("New Plaza Name");

        // The controller returns a String "Building name updated successfully"
        mockMvc.perform(put("/buildings/BLDG123/name")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("Building name updated successfully"));

        verify(buildingService, times(1)).updateBuildingName(eq("BLDG123"), eq("New Plaza Name"));
    }

    @Test
    @WithMockUser(roles = {"TENANT"})
    @DisplayName("PUT /buildings/{id}/name - Forbidden as Tenant")
    void updateBuildingName_asTenant_shouldFail() throws Exception {
        NameUpdateRequest updateRequest = new NameUpdateRequest();
        updateRequest.setName("Malicious Name");

        mockMvc.perform(put("/buildings/BLDG123/name")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"LETTINGAGENT"})
    @DisplayName("PUT /buildings/{id}/name - Bad Request with empty name")
    void updateBuildingName_validationError() throws Exception {
        NameUpdateRequest invalidRequest = new NameUpdateRequest();
        invalidRequest.setName(""); // Empty name should trigger @NotBlank

        mockMvc.perform(put("/buildings/BLDG123/name")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"LETTINGAGENT"})
    @DisplayName("DELETE /buildings/{id} - Success as Agent")
    void deleteBuilding_asAgent_success() throws Exception {
        mockMvc.perform(delete("/buildings/BLDG123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Building and its apartments deleted successfully"));

        verify(buildingService, times(1)).deleteBuilding("BLDG123");
    }

    @Test
    @WithMockUser(roles = {"TENANT"})
    @DisplayName("DELETE /buildings/{id} - Forbidden as Tenant")
    void deleteBuilding_asTenant_shouldFail() throws Exception {
        mockMvc.perform(delete("/buildings/BLDG123")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(buildingService, never()).deleteBuilding(anyString());
    }
}
