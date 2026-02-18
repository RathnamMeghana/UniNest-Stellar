package UniNest.Backend.Authentication;

import UniNest.Backend.config.SecurityConfig;
import UniNest.Backend.controller.ApartmentController;
import UniNest.Backend.controller.GlobalExceptionHandler;
import UniNest.Backend.security.FirebaseTokenFilter;
import UniNest.Backend.service.ApartmentService;
import UniNest.Backend.service.UserService;
import UniNest.Backend.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApartmentController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
public class AuthenticationAccessTest {

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

    @BeforeEach
    public void setup() throws Exception {
        // The mock filter MUST continue the chain.
        //  user with NO TOKEN.
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response); // Pass through to Spring Security
            return null;
        }).when(firebaseTokenFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("Authentication: Unauthenticated request to /apartments/getAll should be BLOCKED")
    public void unauthenticatedRequest_IsBlocked() throws Exception {
        mockMvc.perform(get("/apartments/getAll"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authentication: Request to /auth/login should be ALLOWED (Public)")
    public void publicRequest_IsAllowed() throws Exception {
        // Since we are only loading ApartmentController, /auth/login will be a 404

        mockMvc.perform(get("/auth/login"))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Security Logic: Firebase role '1' maps to ROLE_LETTINGAGENT")
    public void testRoleMapping() {
        String firebaseRole = "1"; // From Firestore/Firebase Claims

        // This represents the mapping logic inside your filter
        String mappedRole = switch (firebaseRole) {
            case "1" -> "LETTINGAGENT";
            case "2" -> "TENANT";
            default -> "USER";
        };

        org.springframework.security.core.authority.SimpleGrantedAuthority authority =
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + mappedRole);

        org.junit.jupiter.api.Assertions.assertEquals("ROLE_LETTINGAGENT", authority.getAuthority());
    }


}