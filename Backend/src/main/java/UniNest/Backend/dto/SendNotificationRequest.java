package UniNest.Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {
    private String houseCode;
    private List<String> tenantUserIds;
    private String title;
    private String body;
}