package com.example.uninest.model;


import com.google.firebase.Timestamp;

public class Apartment {

    private String buildingId;
    private String name;
    private String totalRooms;
    private String code; // set in backend
    private String landlordId;
    private Timestamp createdAt; // set in backend
    private String description;
    private Double rentPrice;
    private Boolean active;


    // Getters & setters

    public String getBuildingId() { return buildingId; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTotalRooms() { return totalRooms; }
    public void setTotalRooms(String totalRooms) { this.totalRooms = totalRooms; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLandlordId() { return landlordId; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getRentPrice() { return rentPrice; }
    public void setRentPrice(Double rentPrice) { this.rentPrice = rentPrice; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}