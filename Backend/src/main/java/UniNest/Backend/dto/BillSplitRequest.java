package UniNest.Backend.dto;

import com.google.cloud.Timestamp;

import java.util.Date;

import lombok.Data;

@Data
public class BillSplitRequest {
    private String billId;
    private String userId;

    private double amountOwed;
    private boolean paid;
    private Date paidAt;
}
