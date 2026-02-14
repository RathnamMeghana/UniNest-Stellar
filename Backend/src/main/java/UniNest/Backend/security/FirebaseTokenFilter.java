package UniNest.Backend.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String idToken = header.substring(7);

            try {
                FirebaseToken decodedToken =
                        FirebaseAuth.getInstance().verifyIdToken(idToken);


                Object roleClaim = decodedToken.getClaims().get("role");
                String role = null;
                String firebaseRoleValue = null;

                //  Determine the raw role value from the claim
                if (roleClaim instanceof String) {
                    firebaseRoleValue = (String) roleClaim;
                } else if (roleClaim instanceof List) {
                    // Handle case where 'role' might be stored as a list/array
                    List<?> rolesList = (List<?>) roleClaim;
                    if (!rolesList.isEmpty()) {
                        firebaseRoleValue = rolesList.get(0).toString(); // Take the first element
                    }
                }

                // Map the raw value to the expected application role for Spring Security
                if (firebaseRoleValue != null) {
                    // Apply mapping based on your observation that '1' might map to 'LETTINGAGENT'
                    switch (firebaseRoleValue) {
                        case "1":
                            role = "LETTINGAGENT";
                            break;

                        case "2":
                            role = "TENANT";
                            break;
                        default:
                            role = firebaseRoleValue; // Use as is if no specific mapping
                            break;
                    }
                }


                FirebaseAuthentication auth =
                        new FirebaseAuthentication(decodedToken, role);

                // Enhanced Logging for Debugging
                System.out.println(">>> SECURITY FILTER DEBUG <<<");
                System.out.println("User ID: " + decodedToken.getUid());
                System.out.println("Firebase Raw Role Value: " + firebaseRoleValue);
                System.out.println("Application Role Set for Security: " + role);
                // Log ALL claims to see if 'TENANT' role exists anywhere else
                System.out.println("ALL CLAIMS: " + decodedToken.getClaims());


                SecurityContextHolder.getContext().setAuthentication(auth);
                // --- END OF MODIFICATION/ENHANCEMENT ---

            } catch (Exception e) {
                e.printStackTrace(); // Print exception for better debugging in case of verification failure
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Firebase token");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}