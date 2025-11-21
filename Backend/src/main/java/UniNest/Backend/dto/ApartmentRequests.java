package UniNest.Backend.dto;



import com.google.cloud.Timestamp;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class ApartmentRequests {
    @NotBlank
    private String buildingId;

    @NotBlank
    private String name;
    private String code;
    private String landlordId;
    private Timestamp createdAt;
    private String description;
    private Double rentPrice;

    @NotNull
    private Boolean active;

}
