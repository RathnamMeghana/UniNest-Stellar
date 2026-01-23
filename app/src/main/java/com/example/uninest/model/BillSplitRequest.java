package com.example.uninest.model;

import java.util.Date;

public class BillSplitRequest {
    private String billId;
    private String userId;
    private String billTitle;

    private double amountOwed;
    private boolean paid;
    private Date paidAt;

    // Getters and Setters
    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getAmountOwed(){
        return amountOwed;
    }
    public void setAmountOwed(Double amountOwed) {
        this.amountOwed = amountOwed;
    }

    public boolean getPaid(){
        return paid;
    }
    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public Date getPaidAt(){
        return paidAt;
    }

    public void setPaidAt(Date paidAt){
        this.paidAt = paidAt;
    }


    public String getBillTitle() {
        return billTitle;
    }
}
