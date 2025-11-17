package UniNest.Backend.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class BuildingRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String addressLine1;

    @NotBlank
    private String city;

    @NotBlank
    private String postcode;

    @NotBlank
    private String country;

    @NotBlank
    private String landlordId;

    @NotBlank
    private Boolean active;

}
