package UniNest.Backend.model;
import com.google.cloud.Timestamp;

import lombok.Data;

@Data
public class Chore {

    private String id;
    private String taskName;
    private String room;
    private int difficultyScore;
    private int estDurationMin;
    private int frequencyPerWeek;
    private String assignedTo; // roommate ID
    private Timestamp createdAt;
    private String assignedUserEmail;
    private String scheduledDate;
    private String status;
    private int actualDuration;
}