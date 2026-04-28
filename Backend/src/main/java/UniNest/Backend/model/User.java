package UniNest.Backend.model;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.IgnoreExtraProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@IgnoreExtraProperties // This prevents crashes if new fields are added to DB
public class User {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String houseCode;
    private String apartmentId;
    private String profileImageUrl;
    private int availabilityMins;
    private List<String> tokens;
    private String fcmToken;

    // Use Object so Firestore doesn't crash on String vs Timestamp
    private Object tokensUpdatedAt;

    public String getFullName() {
        String first = (firstName != null) ? firstName : "";
        String last = (lastName != null) ? lastName : "";
        return (first + " " + last).trim();
    }

    /**
     * Resolving Deserialization Conflicts
     * This helper handles the case where Firestore returns either a String or a Timestamp
     */
    public Long getTokensUpdatedAtMillis() {
        if (tokensUpdatedAt instanceof Timestamp) {
            return ((Timestamp) tokensUpdatedAt).toDate().getTime();
        } else if (tokensUpdatedAt instanceof String) {
            try {
                return java.time.Instant.parse((String) tokensUpdatedAt).toEpochMilli();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}