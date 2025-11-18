package UniNest.Backend.model;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class Apartment {

    private String buildingId;
    private String name;
    private String code;
    private String landlordId;
    private Timestamp createdAt;
    private Boolean active;

}