package UniNest.Backend.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth") // This matches your Android URL: /auth/firebase-login
public class AuthController {

    @PostMapping("/firebase-login")
    public ResponseEntity<?> syncUserRole(@RequestBody Map<String, String> body) {
        try {
            String idToken = body.get("token");
            // 1. Verify the token sent from Android
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();

            // 2. Get role from Firestore (Server-side)
            DocumentSnapshot userDoc = FirestoreClient.getFirestore()
                    .collection("users").document(uid).get().get();

            if (!userDoc.exists()) {
                return ResponseEntity.status(404).body("User record not found in Firestore");
            }

            String firestoreRole = userDoc.getString("role");

            // 3. Map role string to Custom Claim
            // "1" = LETTINGAGENT, "2" = TENANT
            String claimValue = "1".equals(firestoreRole) ? "LETTINGAGENT" : "TENANT";

            // 4. SET THE CUSTOM CLAIM
            // This injects the role into the Firebase Authentication system
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", claimValue);
            FirebaseAuth.getInstance().setCustomUserClaims(uid, claims);

            System.out.println("Successfully set role " + claimValue + " for user: " + uid);

            return ResponseEntity.ok("Claims updated successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(401).body("Error syncing claims: " + e.getMessage());
        }
    }
}