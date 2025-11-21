package UniNest.Backend.dto;



import jakarta.validation.constraints.NotNull;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class ApartmentRequests {
    @NotBlank
    private String buildingId;


    private String name;

    private String landlordId;

    @NotNull
    private Boolean active;

}
