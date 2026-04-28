package UniNest.Backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import UniNest.Backend.config.SecurityConfig;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // These three mocks are vital to stop contextLoads() from failing
    @MockBean
    private com.google.cloud.firestore.Firestore firestore;

    @MockBean
    private com.google.firebase.auth.FirebaseAuth firebaseAuth;

    @MockBean
    private com.google.firebase.FirebaseApp firebaseApp;

    @Test
    void accessProtectedEndpoint_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/apartments/getAll"))
                .andExpect(status().isForbidden()); // Changed to 403 to match project standard
    }
}