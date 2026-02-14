package UniNest.Backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
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
}
