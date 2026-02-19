package UniNest.Backend.dto;



import com.google.cloud.Timestamp;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class ApartmentRequests {
    @NotBlank(message = "Building ID is required")
    private String buildingId;

    @NotBlank(message = "Name is required")
    private String name;
    @NotBlank(message = "total rooms is required")
    private String totalRooms;

    private String code;
    @NotBlank(message = "landlord id is required")
    private String landlordId;

    private Timestamp createdAt;
    @NotBlank(message = "description is required")
    private String description;
    @Min(value = 0, message = "Rent price cannot be negative")
    private Double rentPrice;

    @NotNull(message = "Room type is required")
    private Boolean active;

    private int maxTenants;


    public void sanitize() {
        this.name = UniNest.Backend.util.SanitizationUtil.sanitize(this.name);
        this.buildingId = UniNest.Backend.util.SanitizationUtil.sanitize(this.buildingId);
        this.landlordId = UniNest.Backend.util.SanitizationUtil.sanitize(this.landlordId);
        this.description = UniNest.Backend.util.SanitizationUtil.sanitize(this.description);
        this.totalRooms = UniNest.Backend.util.SanitizationUtil.sanitize(this.totalRooms);
        if (this.createdAt == null) {
            this.createdAt = com.google.cloud.Timestamp.now();
        }
    }

}
