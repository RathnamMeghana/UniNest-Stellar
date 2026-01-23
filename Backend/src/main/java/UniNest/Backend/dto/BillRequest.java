package UniNest.Backend.dto;

import com.google.cloud.Timestamp;
import lombok.Data;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Data
public class BillRequest {



    public enum BillType {
        ONE_TIME,
        RECURRING
    }

    public enum BillFrequency {
        WEEKLY,
        BIWEEKLY,
        MONTHLY
    }


    private String id;
    private String houseCode;

    private String title;
    private double totalAmount;

    private BillType billType;
    private String billTitle;
    private BillFrequency frequency; // null for ONE_TIME


    private Date startDate;

    private Date dueDate;

    private boolean active;
    private String creatorId;
    private List<String> roommateIds;


    private List<BillSplitRequest> splits;


}
