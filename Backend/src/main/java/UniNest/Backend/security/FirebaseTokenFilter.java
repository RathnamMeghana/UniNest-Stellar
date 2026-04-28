package UniNest.Backend.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.DocumentSnapshot;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // Skip filter if no Bearer token is present
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String idToken = header.substring(7);

        try {
            // 1. Verify the token with Firebase Admin SDK
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();

            // 2. Try to extract role from Token Claims (Fastest)
            Object roleClaim = decodedToken.getClaims().get("role");
            String firebaseRoleValue = extractRoleFromClaim(roleClaim);

            // 3. Just-in-Time Authorization (Firestore Fallback)
            // If the token is new and doesn't have the role yet, check the database.
            if (firebaseRoleValue == null) {
                try {
                    DocumentSnapshot userDoc = FirestoreClient.getFirestore()
                            .collection("users")
                            .document(uid)
                            .get()
                            .get();

                    if (userDoc.exists()) {
                        firebaseRoleValue = userDoc.getString("role");
                    }
                } catch (Exception dbEx) {
                    System.err.println(">>> Filter Warning: Firestore lookup failed: " + dbEx.getMessage());
                }
            }

            // 4. Strict Role Mapping
            // standardize "1"/"2" or "TENANT"/"LETTINGAGENT" to the exact Spring roles
            String mappedRole = mapToStandardRole(firebaseRoleValue);

            // 5. Create Authentication object
            // This will use your FirebaseAuthentication class to add the "ROLE_" prefix
            FirebaseAuthentication auth = new FirebaseAuthentication(decodedToken, mappedRole);

            // 6. Set Security Context
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Debug Logging (Viewable in IntelliJ or Google Cloud Logs)
            System.out.println(">>> AUTH SUCCESS: User [" + uid + "] assigned role [ROLE_" + mappedRole + "]");

        } catch (Exception e) {
            // If token is invalid or expired, return a JSON error
            System.err.println(">>> AUTH ERROR: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"" + e.getMessage() + "\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Maps Firestore numeric roles or raw claim strings to standard Application Roles.
     */
    private String mapToStandardRole(String rawValue) {
        if (rawValue == null) return "USER";

        String cleanValue = rawValue.trim().toUpperCase();

        return switch (cleanValue) {
            case "1", "LETTINGAGENT" -> "LETTINGAGENT";
            case "2", "TENANT"       -> "TENANT";
            default                  -> "USER";
        };
    }

    /**
     * Handles cases where 'role' claim is a String or a List.
     */
    private String extractRoleFromClaim(Object roleClaim) {
        if (roleClaim instanceof String) {
            return (String) roleClaim;
        } else if (roleClaim instanceof List && !((List<?>) roleClaim).isEmpty()) {
            return ((List<?>) roleClaim).get(0).toString();
        }
        return null;
    }
}