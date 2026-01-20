package UniNest.Backend.dto;

import lombok.Data;
import java.util.Map;

@Data
public class BulkApartmentWithRoomsRequest {
    private String buildingId;
    private String landlordId;
    private int apartmentCount;
    private Map<String, Integer> roomTemplate;
}

