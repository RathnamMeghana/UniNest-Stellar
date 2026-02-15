package UniNest.Backend.dto;



import UniNest.Backend.util.SanitizationUtil;
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

    /**
     * Sanitize all String fields to prevent XSS or other malicious input.
     */
    public void sanitize() {
        if (this.name != null) this.name = SanitizationUtil.sanitize(this.name);
        if (this.addressLine1 != null) this.addressLine1 = SanitizationUtil.sanitize(this.addressLine1);
        if (this.city != null) this.city = SanitizationUtil.sanitize(this.city);
        if (this.postcode != null) this.postcode = SanitizationUtil.sanitize(this.postcode);
        if (this.country != null) this.country = SanitizationUtil.sanitize(this.country);
        if (this.landlordId != null) this.landlordId = SanitizationUtil.sanitize(this.landlordId);
        if (this.imageUrl != null) this.imageUrl = SanitizationUtil.sanitize(this.imageUrl);
    }

}
