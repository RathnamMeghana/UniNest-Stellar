package UniNest.Backend.security;

import com.google.firebase.auth.FirebaseToken;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

public class FirebaseAuthentication extends AbstractAuthenticationToken {

    private final FirebaseToken firebaseToken;

    // Modified Constructor: Pass the role explicitly
    public FirebaseAuthentication(FirebaseToken firebaseToken, String role) {
        super(convertRoleToAuthorities(role));
        this.firebaseToken = firebaseToken;
        setAuthenticated(true);
    }

    private static Collection<? extends GrantedAuthority> convertRoleToAuthorities(String role) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (role != null && !role.isEmpty()) {
            // Standardizes the role to uppercase and adds the ROLE_ prefix
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        }
        return authorities;
    }

    @Override
    public Object getCredentials() { return null; }

    @Override
    public Object getPrincipal() { return firebaseToken; }
}