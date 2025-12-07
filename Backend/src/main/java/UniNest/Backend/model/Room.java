package UniNest.Backend.model;

import lombok.Data;

@Data
public class Room {
    private String id;
    private String apartmentId;
    private String type;
    private String label;
}
