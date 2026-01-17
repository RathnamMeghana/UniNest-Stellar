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

    private int difficultyScore; // remove @NotBlank
    private int estDurationMin;
    private int frequencyPerWeek;

    private String assignedTo;
    private String assignedUserEmail;

    private Timestamp createdAt; // Firestore timestamp
    private String houseCode;    // String for JSON
    private String scheduledDate; // ISO string for JSON
    private String createdBy;     // String for JSON

    public void sanitize() {
        if (this.taskName != null)
            this.taskName = UniNest.Backend.util.SanitizationUtil.sanitize(this.taskName);
        if (this.room != null)
            this.room = UniNest.Backend.util.SanitizationUtil.sanitize(this.room);
        if (this.assignedTo != null)
            this.assignedTo = UniNest.Backend.util.SanitizationUtil.sanitize(this.assignedTo);
    }
}

