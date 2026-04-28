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
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/firebase-login")
    public ResponseEntity<?> syncUserRole(@RequestBody Map<String, String> body) {
        try {
            String idToken = body.get("token");
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();

            DocumentSnapshot userDoc = FirestoreClient.getFirestore()
                    .collection("users").document(uid).get().get();

            if (!userDoc.exists()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found in Firestore"));
            }

            String firestoreRole = userDoc.getString("role");
            String claimValue = "2".equals(firestoreRole) ? "TENANT" : "LETTINGAGENT";

            Map<String, Object> claims = new HashMap<>();
            claims.put("role", claimValue);
            FirebaseAuth.getInstance().setCustomUserClaims(uid, claims);

            System.out.println(">>> SYNC SUCCESS: Role " + claimValue + " set for " + uid);

            // RETURN JSON instead of a String
            return ResponseEntity.ok(Map.of("status", "success", "role", claimValue));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
}