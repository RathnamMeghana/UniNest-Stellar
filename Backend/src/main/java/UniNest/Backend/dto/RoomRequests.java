package UniNest.Backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoomRequests {

    private String id;
    private String houseCode;
    @NotBlank(message = "Room type is required")
    @Size(max = 50, message = "Room type cannot exceed 50 characters")
    private String type;
    @NotBlank(message = "Label is required")
    private String label;


    public void sanitize() {
        this.id = UniNest.Backend.util.SanitizationUtil.sanitize(this.id);
        this.houseCode = UniNest.Backend.util.SanitizationUtil.sanitize(this.houseCode);
        this.type = UniNest.Backend.util.SanitizationUtil.sanitize(this.type);
        this.label = UniNest.Backend.util.SanitizationUtil.sanitize(this.label);
    }
}
