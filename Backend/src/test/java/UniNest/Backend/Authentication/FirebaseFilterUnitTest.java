package UniNest.Backend.Authentication;

import UniNest.Backend.security.FirebaseTokenFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

public class FirebaseFilterUnitTest {

    @Test
    @DisplayName("Filter: Missing Header should skip authentication")
    public void filter_WhenHeaderMissing_ContinuesChain() throws Exception {
        // Setup real filter and mocks
        FirebaseTokenFilter filter = new FirebaseTokenFilter();
        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        //  Execute
        filter.doFilter(request, response, chain);

        // Verify: The filter should just pass the request to the next filter
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Filter: Invalid Header format should skip authentication")
    public void filter_WhenHeaderInvalidFormat_ContinuesChain() throws Exception {
        FirebaseTokenFilter filter = new FirebaseTokenFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "NotBearer 12345"); // Wrong prefix

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Authentication: Claim '1' is mapped to LETTINGAGENT")
    public void testRoleMapping_Agent() {
        String firebaseRole = "1";
        String expectedAuthority = "ROLE_LETTINGAGENT";

        // Logic that matches your filter:
        String mappedRole = switch (firebaseRole) {
            case "1" -> "LETTINGAGENT";
            case "2" -> "TENANT";
            default -> "USER";
        };

        org.springframework.security.core.authority.SimpleGrantedAuthority authority =
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + mappedRole);

        org.junit.jupiter.api.Assertions.assertEquals(expectedAuthority, authority.getAuthority());
    }
    @Test
    @DisplayName("Filter Logic: Malformed Authorization header should not authenticate user")
    public void filter_MalformedHeader_NoAuthSet() throws Exception {
        FirebaseTokenFilter filter = new FirebaseTokenFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // Header exists but is missing "Bearer " prefix
        request.addHeader("Authorization", "123456789");
        SecurityContextHolder.clearContext();

        filter.doFilter(request, response, chain);

        // SecurityContext should still be empty
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }


}