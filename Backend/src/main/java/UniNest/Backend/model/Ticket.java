package UniNest.Backend.model;

import com.google.cloud.Timestamp;

import UniNest.Backend.util.SanitizationUtil;
import lombok.Data;

@Data
public class Ticket {

    private String id;
    private String description;
    private String room;
    private String building;
    private String apartmentId;
    private String category;
    private String priority;
    private String status;
    private String userId;
    private Timestamp createdAt;

    public void sanitize() {
        this.id = SanitizationUtil.sanitize(this.id);
        this.description = SanitizationUtil.sanitize(this.description);
        this.room = SanitizationUtil.sanitize(this.room);
        this.building = SanitizationUtil.sanitize(this.building);
        this.apartmentId = SanitizationUtil.sanitize(this.apartmentId);
        this.category = SanitizationUtil.sanitize(this.category);
        this.priority = SanitizationUtil.sanitize(this.priority);
        this.status = SanitizationUtil.sanitize(this.status);
        this.userId = SanitizationUtil.sanitize(this.userId);
    }

}
