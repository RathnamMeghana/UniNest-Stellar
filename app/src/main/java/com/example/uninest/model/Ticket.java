package com.example.uninest.model;

import com.google.firebase.Timestamp;

import java.util.Date;

public class Ticket {
    private String id;
    private String description;
    private String room;
    private String building;
    private String apartmentId;
    private String category;
    private String priority;
    private String status;
    private String userId;
    private Object createdAt; // set in backend
    private String landlordId;
    private String apartmentName;


    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }

    public String getApartmentId() { return apartmentId; }
    public void setApartmentId(String apartmentId) { this.apartmentId = apartmentId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Object getCreatedAt() { return createdAt; }
    public void setCreatedAt(Object createdAt) { this.createdAt = createdAt; }

    public String getLandlordId() { return landlordId; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }

    public String getApartmentName() { return apartmentName; }
    public void setApartmentName(String apartmentName) { this.apartmentName = apartmentName; }

}
