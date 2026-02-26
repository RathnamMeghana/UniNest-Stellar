package UniNest.Backend.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore; // Added import
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient; // Added import
import org.springframework.context.annotation.Bean; // Added import
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {
            FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                    .setProjectId("uninest-c8d4b")
                    .setStorageBucket("uninest-c8d4b.firebasestorage.app")
                    .setDatabaseUrl("https://uninest-c8d4b.firebaseio.com");

            // Check if we are running locally with the file
            ClassPathResource res = new ClassPathResource("serviceAccountKey.json");
            if (res.exists()) {
                optionsBuilder.setCredentials(GoogleCredentials.fromStream(res.getInputStream()));
                System.out.println("Firebase initialized using local JSON key.");
            } else {
                // This is what Google Cloud Run will use automatically
                optionsBuilder.setCredentials(GoogleCredentials.getApplicationDefault());
                System.out.println("Firebase initialized using Application Default Credentials.");
            }

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(optionsBuilder.build());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }

    @Bean
    public Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }
}
