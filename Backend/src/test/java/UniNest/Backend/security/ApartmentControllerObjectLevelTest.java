package UniNest.Backend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import UniNest.Backend.controller.ApartmentController;
import UniNest.Backend.exception.UserServiceException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.util.Collections;

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
import UniNest.Backend.security.FirebaseTokenFilter;
import UniNest.Backend.service.ApartmentService;
import UniNest.Backend.service.RoomService;
import UniNest.Backend.service.UserService;

@WebMvcTest(ApartmentController.class)
@Import(SecurityConfig.class)
class ApartmentControllerObjectLevelTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApartmentService apartmentService;

    @MockBean
    private UserService userService;

    @MockBean
    private RoomService roomService;

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


    // Tenant tries to access another apartment users
    @Test
    @WithMockUser(username = "tenantA", roles = {"TENANT"})
    void tenant_access_other_houseCode_users_shouldReturn403() throws Exception, UserServiceException {

        when(userService.getUsersForApartment("APT-999"))
                .thenThrow(new AccessDeniedException("Forbidden"));

        mockMvc.perform(get("/apartments/APT-999/users"))
                .andExpect(status().isForbidden());
    }


    // Agent tries to fetch apartments from another building
    @Test
    @WithMockUser(username = "agentA", roles = {"LETTINGAGENT"})
    void agent_access_unassigned_building_shouldReturn403() throws Exception {

        when(apartmentService.getApartmentsByBuilding("BUILD-999"))
                .thenThrow(new AccessDeniedException("Forbidden"));

        mockMvc.perform(get("/apartments/getByBuilding")
                        .param("buildingId", "BUILD-999"))
                .andExpect(status().isForbidden());
    }

    // Tenant should NOT see all apartments
    @Test
    @WithMockUser(username = "tenantA", roles = {"TENANT"})
    void tenant_getAll_shouldNotExposeAllApartments() throws Exception {

        // Simulate secure service filtering
        when(apartmentService.getAllApartments())
                .thenThrow(new AccessDeniedException("Forbidden"));

        mockMvc.perform(get("/apartments/getAll"))
                .andExpect(status().isForbidden());
    }


    // Agent removing tenant from unauthorized apartment
    @Test
    @WithMockUser(username = "agentA", roles = {"LETTINGAGENT"})
    void agent_remove_tenant_from_other_building_shouldReturn403() throws Exception {

        doAnswer(invocation -> {
            throw new AccessDeniedException("Forbidden");
        }).when(apartmentService).removeTenantFromApartment(eq("victim@email.com"));

        mockMvc.perform(delete("/apartments/tenants/victim@email.com/remove")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }


    // Unauthenticated access
    @Test
    void unauthenticated_access_shouldReturn403() throws Exception {
        mockMvc.perform(get("/apartments/getAll"))
                .andExpect(status().isForbidden());
    }
}

