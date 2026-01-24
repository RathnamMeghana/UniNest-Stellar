package UniNest.Backend.model;

import com.google.cloud.Timestamp;
import lombok.Data;

@Data
public class CalendarEvent {

    private String id;
    private String houseCode;

    private String type; // CHORE, MOVE_OUT, etc
    private String title;
    private String description;
    private String location;

    private Timestamp startDate;
    private Timestamp endDate;
    private boolean allDay;

    private String createdBy;
    private String assignedTo;
    private String relatedChoreId;

    private Recurrence recurrence;
    private Timestamp createdAt;

    private Double amount;
    private String status;
    private int actualDuration;
    private int estDuration;
}
