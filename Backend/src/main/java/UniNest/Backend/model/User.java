package UniNest.Backend.model;
import lombok.Data;

@Data
public class User {
    private String apartmentId;
    private String email;
    private String houseCode;
    private String role;
    private String id;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
}
