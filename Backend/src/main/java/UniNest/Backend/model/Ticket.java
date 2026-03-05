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
    private String landlordId;
    private String apartmentName;
    private String category;
    private String priority;
    private String status;
    private String userId;
    private Timestamp createdAt;
    private String agentResponse;
    private String arrivalDate;
    private Timestamp updatedAt;
    private String userName;
    private String prioritySource;
    private String imageUrl;


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
        this.landlordId = SanitizationUtil.sanitize(this.landlordId);
        this.apartmentName = SanitizationUtil.sanitize(this.apartmentName);
        this.agentResponse = SanitizationUtil.sanitize(this.agentResponse);
        this.arrivalDate = SanitizationUtil.sanitize(this.arrivalDate);
        this.userName = SanitizationUtil.sanitize(this.userName);
        this.prioritySource = SanitizationUtil.sanitize(this.prioritySource);
        if (this.imageUrl != null && !this.imageUrl.isEmpty()) {
            this.imageUrl = SanitizationUtil.sanitizeBase64(this.imageUrl);
        }
    }

}
