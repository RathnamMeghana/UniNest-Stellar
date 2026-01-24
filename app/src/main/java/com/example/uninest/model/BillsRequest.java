package com.example.uninest.model;

import java.util.Date;
import java.util.List;

public class BillsRequest {

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
    private BillFrequency frequency; // null for ONE_TIME

    private Date startDate;
    private Date dueDate;

    private boolean active;
    private String creatorId;
    private List<String> roommateIds;

    // Embedded splits
    private List<Split> splits;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getHouseCode() { return houseCode; }
    public void setHouseCode(String houseCode) { this.houseCode = houseCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public BillType getBillType() { return billType; }
    public void setBillType(BillType billType) { this.billType = billType; }

    public BillFrequency getFrequency() { return frequency; }
    public void setFrequency(BillFrequency frequency) { this.frequency = frequency; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }

    public List<String> getRoommateIds() { return roommateIds; }
    public void setRoommateIds(List<String> roommateIds) { this.roommateIds = roommateIds; }

    public List<Split> getSplits() { return splits; }
    public void setSplits(List<Split> splits) { this.splits = splits; }

    // Split class
    public static class Split {
        private String userId;
        private double amountOwed;
        private boolean paid;
        private String billTitle;
        private String billId;
        private Date paidAt;

        // Getters & Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public double getAmountOwed() { return amountOwed; }
        public void setAmountOwed(double amountOwed) { this.amountOwed = amountOwed; }

        public boolean isPaid() { return paid; }
        public void setPaid(boolean paid) { this.paid = paid; }

        public Date getPaidAt() { return paidAt; }
        public void setPaidAt(Date paidAt) { this.paidAt = paidAt; }

        public String getBillTitle() { return billTitle; }
        public void setBillTitle(String billTitle) { this.billTitle = billTitle; }

        public String getBillId() { return billId; }
        public void setBillId(String billId) { this.billId = billId; }
    }
}
