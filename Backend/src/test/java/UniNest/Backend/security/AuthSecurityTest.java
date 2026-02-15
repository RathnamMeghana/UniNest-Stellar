package UniNest.Backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import UniNest.Backend.config.SecurityConfig;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class AuthSecurityTest {
    @MockBean
    private com.google.cloud.firestore.Firestore firestore;


    @Autowired
    private MockMvc mockMvc;

    @Test
    void accessProtectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/apartments/getAll"))
                .andExpect(status().isForbidden());
    }
}
