package UniNest.Backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for API calls

                // Allow public access to all API endpoints used by the Android app
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/**",           // Authentication endpoints
                                "/apartments/**",     // ApartmentController
                                "/buildings/**",      // BuildingController
                                "/tickets/**",        // TicketController
                                "/chores/**",         // ChoreController
                                "/calendar/**",       // CalendarController
                                "/bills/**",          // BillsController
                                "/users/**"           // UserController
                        ).permitAll()

                        // Any other request (admin pages, static resources, etc.) require authentication
                        .anyRequest().authenticated()
                )

                // Keep HTTP basic enabled for protected endpoints
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
