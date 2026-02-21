package UniNest.Backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class BackendApplicationTests {

	// Add these so the Application Context can start during tests
	@MockBean
	private com.google.cloud.firestore.Firestore firestore;

	@MockBean
	private com.google.firebase.auth.FirebaseAuth firebaseAuth;

	@MockBean
	private com.google.firebase.FirebaseApp firebaseApp;

	@Test
	void contextLoads() {
		// checks if the Spring context starts successfully
	}
}