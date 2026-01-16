package UniNest.Backend.dto;

import com.google.cloud.Timestamp;

import lombok.Data;
import java.util.List;


public class CalendarEventDTO {

    // ENUMS for event type
    public enum EventType {
        CHORE,
        MOVE_OUT,
        MOVE_IN,
        BILL_DUE,
        MAINTENANCE,
        CUSTOM
    }

    // RECURRENCE DTO
    @Data
    public static class Recurrence {
        private String frequency;       // DAILY, WEEKLY, MONTHLY
        private int interval;
        private List<String> daysOfWeek;
        private Timestamp endDate;         // ISO-8601
    }

    @Data
    public static class Create {
        private String houseCode;
        private EventType type;

        private String title;
        private String description;

        private Timestamp startDate;
        private Timestamp endDate;

        private boolean allDay;

        private String assignedTo;
        private String relatedChoreId;

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
        private String title;
        private String description;

        private Timestamp startDate;
        private Timestamp endDate;
        private Boolean allDay;

        private String assignedTo;
        private Recurrence recurrence;

        public void sanitize() {
            if (this.title != null)
                this.title = UniNest.Backend.util.SanitizationUtil.sanitize(this.title);
            if (this.description != null)
                this.description = UniNest.Backend.util.SanitizationUtil.sanitize(this.description);
            if (this.assignedTo != null)
                this.assignedTo = UniNest.Backend.util.SanitizationUtil.sanitize(this.assignedTo);
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
    }
}
