package UniNest.Backend.dto;

import com.google.cloud.Timestamp;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChoreRequests {
    private String id;
    @NotBlank(message = "taskName is required")
    private String taskName;
    @NotBlank(message = "room is required")
    private String room;
    @NotBlank(message = "difficultyScore type is required")
    private int difficultyScore;
    @NotBlank(message = "estDurationMin type is required")
    private int estDurationMin;
    @NotBlank(message = "frequencyPerWeek type is required")
    private int frequencyPerWeek;
    private String assignedTo;
    private Timestamp createdAt;


    public void sanitize() {
        this.id = UniNest.Backend.util.SanitizationUtil.sanitize(this.id);
        this.taskName = UniNest.Backend.util.SanitizationUtil.sanitize(this.taskName);
        this.room = UniNest.Backend.util.SanitizationUtil.sanitize(this.room);
        this.assignedTo = UniNest.Backend.util.SanitizationUtil.sanitize(this.assignedTo);
    }
}

