package UniNest.Backend.dto;

import lombok.Data;
import java.util.Date;

@Data
public class OwedToUserResponse {
    private String billId;
    private String billTitle;
    private String debtorUserId;
    private double amountOwed;
    private Date dueDate;
}