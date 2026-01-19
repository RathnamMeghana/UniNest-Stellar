package UniNest.Backend.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class BulkApartmentWithRoomsRequest {
    private List<ApartmentRequests> apartments;
    private Map<String, Integer> roomTemplate;
}
