package UniNest.Backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class ApartmentRequests {
    @NotBlank
    private String buildingId;

    @NotBlank
    private String name;

    @NotBlank
    private String code;

    @NotBlank
    private String landlordId;

    @NotBlank
    private Boolean active;

}
