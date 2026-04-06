package UniNest.Backend.dto;

import UniNest.Backend.util.SanitizationUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NameUpdateRequest {
    @NotBlank(message = "Name is required")
    private String name;

    public void sanitize() {
        this.name = SanitizationUtil.sanitize(this.name);
    }
}
