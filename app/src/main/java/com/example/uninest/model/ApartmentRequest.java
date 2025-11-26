package com.example.uninest.model;
import com.example.uninest.data.api.BuildingApi;
import com.google.firebase.Timestamp;

import java.io.Serializable;


public class ApartmentRequest implements Serializable {

    private String buildingId;
    private String name;
    private String totalRooms;
    private String code; // set in backend
    private String landlordId;
    private Timestamp createdAt; // set in backend
    private String description;
    private Double rentPrice;
    private Boolean active;

    public String getBuildingId() { return buildingId; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTotalRooms() { return totalRooms; }
    public void setTotalRooms(String totalRooms) { this.totalRooms = totalRooms; }
    public String getLandlordId() { return landlordId; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getRentPrice() { return rentPrice; }
    public void setRentPrice(Double rentPrice) { this.rentPrice = rentPrice; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    }


