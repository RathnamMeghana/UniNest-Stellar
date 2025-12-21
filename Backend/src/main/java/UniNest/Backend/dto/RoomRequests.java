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
}
