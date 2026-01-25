package com.example.uninest.model;


import com.google.gson.annotations.SerializedName;

public class Calendar {
    @SerializedName("id")
    private String id;

    @SerializedName("houseCode")
    private String houseCode;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("type")
    private String type;

    private FirestoreTimestamp startDate;
    private FirestoreTimestamp endDate;

    @SerializedName("recurrence")
    private Recurrence recurrence;

    public Recurrence getRecurrence() { return recurrence; }
    public void setRecurrence(Recurrence recurrence) { this.recurrence = recurrence; }

    @SerializedName("amount")
    private Double amount;

    @SerializedName("assignedTo")
    private String assignedTo;

    @SerializedName("createdBy")
    private String createdBy;

    @SerializedName("status")
    private String status;

    @SerializedName("estDuration")
    private int estDuration;

    @SerializedName("relatedChoreId")
    private String relatedChoreId;

    @SerializedName("location")
    private String location;

    @SerializedName("actualDuration")
    private int actualDuration;

    public int getActualDuration() { return actualDuration; }
    public void setActualDuration(int actualDuration) { this.actualDuration = actualDuration; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getEstDuration() { return estDuration; }
    public void setEstDuration(int estDuration) { this.estDuration = estDuration; }


    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getHouseCode() { return houseCode; }
    public void setHouseCode(String houseCode) { this.houseCode = houseCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public FirestoreTimestamp getStartDate() { return startDate; }
    public void setStartDate(FirestoreTimestamp startDate) { this.startDate = startDate; }

    public FirestoreTimestamp getEndDate() { return endDate; }
    public void setEndDate(FirestoreTimestamp endDate) { this.endDate = endDate; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getRelatedChoreId() { return relatedChoreId; }
    public void setRelatedChoreId(String relatedChoreId) { this.relatedChoreId = relatedChoreId; }
}
