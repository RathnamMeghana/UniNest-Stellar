package com.example.uninest.model;

import com.google.firebase.Timestamp;
import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.io.Serializable;

public class Ticket implements Serializable{
    private String id;
    private String description;
    private String room;
    @SerializedName(value = "building", alternate = {"buildingName", "name"})
    private String building;
    private String apartmentId;
    private String category;
    private String priority;
    private String status;
    private String userId;
    private Object createdAt; // set in backend
    private Object updatedAt;
    private String landlordId;
    private String apartmentName;
    private String agentResponse;
    private String arrivalDate;
    private String userName;
    private String prioritySource;
    private String imageUrl;


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

    public Object getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Object updatedAt) { this.updatedAt = updatedAt; }

    public String getLandlordId() { return landlordId; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }

    public String getApartmentName() { return apartmentName; }
    public void setApartmentName(String apartmentName) { this.apartmentName = apartmentName; }

    public String getAgentResponse() { return agentResponse; }
    public void setAgentResponse(String agentResponse) { this.agentResponse = agentResponse; }

    public String getArrivalDate() { return arrivalDate; }
    public void setArrivalDate(String arrivalDate) { this.arrivalDate = arrivalDate; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPrioritySource() { return prioritySource; }
    public void setPrioritySource(String prioritySource) { this.prioritySource = prioritySource; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
