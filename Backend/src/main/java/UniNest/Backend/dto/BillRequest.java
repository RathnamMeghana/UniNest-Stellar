package UniNest.Backend.dto;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class BillRequest {

    public enum BillType { ONE_TIME, RECURRING }
    public enum BillFrequency { WEEKLY, BIWEEKLY, MONTHLY }

    private String id;
    private String houseCode;

    private String title;
    private double totalAmount;

    private BillType billType;
    private BillFrequency frequency; // null for ONE_TIME
    private Date startDate;
    private Date dueDate;

    private boolean active;
    private String creatorId;

    private List<String> roommateIds;
    // replaces the separate splits table
    private List<Split> splits;


    @Data
    public static class Split {
        private String userId;
        private String email;
        private double amountOwed;
        private boolean paid;
        private String billTitle;
        private String billId;


        private Date paidAt;
    }
}
