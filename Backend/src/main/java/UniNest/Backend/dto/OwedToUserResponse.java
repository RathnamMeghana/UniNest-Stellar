package UniNest.Backend.dto;

import UniNest.Backend.util.SanitizationUtil;
import lombok.Data;
import java.util.Date;

@Data
public class OwedToUserResponse {
    private String billId;
    private String billTitle;
    private String debtorUserId;
    private double amountOwed;
    private Date dueDate;

    private BillRequest.BillType billType;
    private BillRequest.BillFrequency frequency;


    public void sanitize() {
        if (this.billId != null)
            this.billId = SanitizationUtil.sanitize(this.billId);
        if (this.billTitle != null)
            this.billTitle = SanitizationUtil.sanitize(this.billTitle);
        if (this.debtorUserId != null)
            this.debtorUserId = SanitizationUtil.sanitize(this.debtorUserId);

    }
}