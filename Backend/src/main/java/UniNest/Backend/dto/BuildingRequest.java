package UniNest.Backend.dto;



import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class BuildingRequest {

    @NotBlank
    private String name;


    private String addressLine1;


    private String city;


    private String postcode;


    private String country;


    private String landlordId;

    @NotNull
    private Boolean active;

}
