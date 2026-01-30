package UniNest.Backend.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class FirebaseTokenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String idToken = header.substring(7);

            try {
                FirebaseToken decodedToken =
                        FirebaseAuth.getInstance().verifyIdToken(idToken);

                String role = (String) decodedToken.getClaims().get("role");

                FirebaseAuthentication auth =
                        new FirebaseAuthentication(decodedToken, role);

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Firebase token");
                return;
            }



        }

        chain.doFilter(request, response);
    }
}
