package UniNest.Backend.model;

import com.google.cloud.Timestamp;

import lombok.Data;

@Data
public class Ticket {

    private String id;
    private String description;
    private String room;
    private String category;
    private String priority;
    private String status;
    private String userId;
    private Timestamp createdAt;

}
