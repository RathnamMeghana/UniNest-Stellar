package UniNest.Backend.model;

import com.google.cloud.Timestamp;

public class BillSplit {
    private String billId;
    private String userId;

    private double amountOwed;
    private boolean paid;
    private Timestamp paidAt;
}
