package UniNest.Backend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import UniNest.Backend.config.SecurityConfig;
import UniNest.Backend.controller.BuildingController;
import UniNest.Backend.service.BuildingService;

@WebMvcTest(BuildingController.class)
@Import(SecurityConfig.class)
class BuildingControllerObjectLevelTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BuildingService buildingService;

    @MockBean
    private FirebaseTokenFilter firebaseTokenFilter;

    // ---------------------------------------------------
    // BYPASS FIREBASE FILTER
    // ---------------------------------------------------
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


    // OBJECT LEVEL AUTHORIZATION TESTS



    //  Landlord attempts to view other landlord buildings via landlordId parameter
    @Test
    @WithMockUser(username = "landlordA", roles = {"LANDLORD"})
    void landlord_access_other_landlord_buildings_shouldReturn403() throws Exception {

        when(buildingService.getBuildingsByLandlord("LANDLORD-999"))
                .thenThrow(new AccessDeniedException("Forbidden"));

        mockMvc.perform(get("/buildings/byLandlord")
                        .param("landlordId", "LANDLORD-999"))
                .andExpect(status().isForbidden());
    }


    // Landlord attempts access to unowned building
    @Test
    @WithMockUser(username = "landlordA", roles = {"LANDLORD"})
    void landlord_access_unowned_building_shouldReturn403() throws Exception {

        when(buildingService.getBuildingById("BUILD-999"))
                .thenThrow(new AccessDeniedException("Forbidden"));

        mockMvc.perform(get("/buildings/BUILD-999"))
                .andExpect(status().isForbidden());
    }


    //  Tenant attempts to fetch all buildings
    @Test
    @WithMockUser(username = "tenantUser", roles = {"TENANT"})
    void tenant_getAllBuildings_shouldReturn403() throws Exception {

        mockMvc.perform(get("/buildings/getAll"))
                .andExpect(status().isForbidden());
    }


    // Unauthenticated access attempt
    @Test
    void unauthenticated_access_shouldReturn403() throws Exception {

        mockMvc.perform(get("/buildings/getAll"))
                .andExpect(status().isForbidden());
    }
}
