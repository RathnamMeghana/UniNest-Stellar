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


        private List<BillSplitRequest> splits;

        private List<String> roommateIds;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHouseCode() {
        return houseCode;
    }

    public void setHouseCode(String houseCode) {
        this.houseCode = houseCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public BillFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(BillFrequency frequency) {
        this.frequency = frequency;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public List<BillSplitRequest> getSplits() {
        return splits;
    }

    public void setSplits(List<BillSplitRequest> splits) {
        this.splits = splits;
    }

    public List<String> getRoommateIds() {
        return roommateIds;
    }

    public void setRoommateIds(List<String> roommateIds) {
        this.roommateIds = roommateIds;
    }
}

