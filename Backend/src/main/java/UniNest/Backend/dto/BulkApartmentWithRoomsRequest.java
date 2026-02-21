package UniNest.Backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.Map;

@Data
public class BulkApartmentWithRoomsRequest {

    @NotBlank(message = "Building ID is required")
    private String buildingId;

    @NotBlank(message = "Landlord ID is required")
    private String landlordId;

    @Min(value = 1, message = "Apartment count must be at least 1") // <--- ADD THIS
    private int apartmentCount;

    @NotEmpty(message = "Room template cannot be empty")
    private Map<String, Integer> roomTemplate;

    private  int maxTenants;
}