package UniNest.Backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppNotification {
    private String id;
    private String userId;
    private String title;
    private String body;
    private String type;
    private String targetScreen;
    private String entityId;
    private Long createdAt;
    private Long eventTime;
    private boolean read;
}