package UniNest.Backend.model;

import lombok.Data;
import com.google.cloud.Timestamp;

import java.util.List;

@Data
public class Apartment {

    private String buildingId;
    private String name;
    private String totalRooms;
    private String code;
    private String landlordId;
    private Timestamp createdAt;
    private String description;
    private Double rentPrice;
    private Boolean active;
    private List<Room> rooms;


}