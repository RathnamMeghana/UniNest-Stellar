package UniNest.Backend.dto;

import com.google.cloud.Timestamp;
import jakarta.validation.constraints.NotBlank; // Required
import jakarta.validation.constraints.NotNull;   // Required
import lombok.Data;
import java.util.List;

public class CalendarEventDTO {

    public enum EventType { CHORE, MOVE_OUT, MOVE_IN, BILL_DUE, MAINTENANCE, OTHER, CUSTOM, EVENT,  REMINDER }

    @Data
    public static class Recurrence {
        private String frequency;
        private int interval;
        private List<String> daysOfWeek;
        private Timestamp endDate;
    }

    @Data
    public static class Create {
        @NotBlank(message = "House code is required")
        private String houseCode;

        @NotNull(message = "Event type is required")
        private EventType type;

        @NotBlank(message = "Title is required")
        private String title;

        private String description;
        private String startDate;
        private String endDate;
        private boolean allDay;
        private String assignedTo;
        private String relatedChoreId;
        private Double amount;
        private int estDuration;
        private int difficultyScore;

        private String location;
        private Recurrence recurrence;


        public void sanitize() {
            this.houseCode = UniNest.Backend.util.SanitizationUtil.sanitize(this.houseCode);
            this.title = UniNest.Backend.util.SanitizationUtil.sanitize(this.title);
            this.description = UniNest.Backend.util.SanitizationUtil.sanitize(this.description);
            this.assignedTo = UniNest.Backend.util.SanitizationUtil.sanitize(this.assignedTo);
            this.relatedChoreId = UniNest.Backend.util.SanitizationUtil.sanitize(this.relatedChoreId);
        }
    }

    @Data
    public static class Update {
        @NotBlank(message = "Title is required")
        private String title;
        private String description;
        private Timestamp startDate;
        private Timestamp endDate;
        private Boolean allDay;
        private String assignedTo;
        private Recurrence recurrence;

        private String status;

        public void sanitize() {
            if (this.title != null) this.title = UniNest.Backend.util.SanitizationUtil.sanitize(this.title);
            if (this.description != null) this.description = UniNest.Backend.util.SanitizationUtil.sanitize(this.description);
            if (this.assignedTo != null) this.assignedTo = UniNest.Backend.util.SanitizationUtil.sanitize(this.assignedTo);
            if (this.status != null) this.status = UniNest.Backend.util.SanitizationUtil.sanitize(this.status);
        }
    }

    @Data
    public static class Response {
        private String id;
        private String houseCode;
        private EventType type;
        private String title;
        private String description;
        private Timestamp startDate;
        private Timestamp endDate;
        private boolean allDay;
        private String createdBy;
        private String assignedTo;
        private String relatedChoreId;
        private Recurrence recurrence;
        private Double amount;
        private String status;
        private int estDuration;
        private int actualDuration;
        private int difficultyScore;
        private String location;
    }
}