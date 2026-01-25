package UniNest.Backend.dto;



import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class BuildingRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Address is required")
    private String addressLine1;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "postcode is required")
    private String postcode;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "landlord Id is required")
    private String landlordId;

    @NotNull(message = "Room type is required")
    private Boolean active;

    private String imageUrl;

}
