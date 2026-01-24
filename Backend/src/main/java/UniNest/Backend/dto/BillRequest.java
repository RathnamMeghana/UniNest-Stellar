package UniNest.Backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class BillRequest {

    public enum BillType { ONE_TIME, RECURRING }
    public enum BillFrequency { WEEKLY, BIWEEKLY, MONTHLY }

    private String id;

    @NotBlank(message = "houseCode is required")
    private String houseCode;

    @NotBlank(message = "title is required")
    private String title;

    @Positive(message = "totalAmount must be greater than 0")
    private double totalAmount;

    @NotNull(message = "billType is required")
    private BillType billType;

    // Only required for RECURRING bills
    private BillFrequency frequency;

    private Date startDate;
    private Date dueDate;

    private boolean active = true;

    @NotBlank(message = "creatorId is required")
    private String creatorId;

    @NotEmpty(message = "roommateIds cannot be empty")
    private List<String> roommateIds;

    @NotEmpty(message = "splits cannot be empty")
    private List<Split> splits;

    /**
     * Sanitizes all user-provided string fields
     */
    public void sanitize() {
        this.houseCode = UniNest.Backend.util.SanitizationUtil.sanitize(this.houseCode);
        this.title = UniNest.Backend.util.SanitizationUtil.sanitize(this.title);
        this.creatorId = UniNest.Backend.util.SanitizationUtil.sanitize(this.creatorId);

        if (roommateIds != null) {
            roommateIds.replaceAll(UniNest.Backend.util.SanitizationUtil::sanitize);
        }

        if (splits != null) {
            splits.forEach(Split::sanitize);
        }
    }

    // ------------------- SPLIT -------------------
    @Data
    public static class Split {

        @NotBlank(message = "userId is required")
        private String userId;

        private String email;

        @Positive(message = "amountOwed must be greater than 0")
        private double amountOwed;

        private boolean paid;

        private String billTitle;
        private String billId;

        private Date paidAt;

        /**
         * Sanitizes string fields inside Split
         */
        public void sanitize() {
            this.userId = UniNest.Backend.util.SanitizationUtil.sanitize(this.userId);

            if (this.email != null)
                this.email = UniNest.Backend.util.SanitizationUtil.sanitize(this.email);

            if (this.billTitle != null)
                this.billTitle = UniNest.Backend.util.SanitizationUtil.sanitize(this.billTitle);

            if (this.billId != null)
                this.billId = UniNest.Backend.util.SanitizationUtil.sanitize(this.billId);
        }
    }
}
