package com.example.uninest.model;

import java.util.Date;

public class OwedToUser {

    private String billId;
    private String billTitle;
    private String debtorUserId;
    private double amountOwed;
    private Date dueDate;

    public String getBillId() {
        return billId;
    }

    public String getBillTitle() {
        return billTitle;
    }

    public String getDebtorUserId() {
        return debtorUserId;
    }

    public double getAmountOwed() {
        return amountOwed;
    }

    public Date getDueDate() {
        return dueDate;
    }
}
