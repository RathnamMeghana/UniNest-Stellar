package UniNest.Backend.dto;

import UniNest.Backend.util.SanitizationUtil;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

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

    @NotNull(message = "Active status is required")
    private Boolean active;

    private String imageUrl;

    public void sanitize() {
        // Standard sanitization for text fields
        this.name = SanitizationUtil.sanitize(this.name);
        this.addressLine1 = SanitizationUtil.sanitize(this.addressLine1);
        this.city = SanitizationUtil.sanitize(this.city);
        this.postcode = SanitizationUtil.sanitize(this.postcode);
        this.country = SanitizationUtil.sanitize(this.country);
        this.landlordId = SanitizationUtil.sanitize(this.landlordId);

        // SPECIAL sanitization for the Base64 image field
        if (this.imageUrl != null) {
            this.imageUrl = SanitizationUtil.sanitizeBase64(this.imageUrl);
        }
    }
}