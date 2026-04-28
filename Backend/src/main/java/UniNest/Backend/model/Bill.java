package UniNest.Backend.model;


import com.google.cloud.Timestamp;

public class Bill {
    public enum BillType {
        ONE_TIME,
        RECURRING
    }

    public enum BillFrequency {
        WEEKLY,
        BYWEEKLY,
        MONTHLY
    }

    private String id;
    private String houseCode;

    private String title;           // "Electricity", "Rent" etc
    private double totalAmount;

    private BillType billType;       // ONE_TIME or RECURRING
    private BillFrequency frequency; // null for one-time

    private Timestamp startDate;
    private Timestamp dueDate;   // next due date

    private boolean active;           // useful for recurring bills
}



