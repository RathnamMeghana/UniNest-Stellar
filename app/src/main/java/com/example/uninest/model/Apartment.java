package com.example.uninest.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Apartment implements Serializable {

    @SerializedName("buildingId")
    private String buildingId;

    @SerializedName("name")
    private String name;

    @SerializedName("totalRooms")
    private String totalRooms;

    @SerializedName("code")
    private String code;

    @SerializedName("landlordId")
    private String landlordId;

    @SerializedName("description")
    private String description;

    @SerializedName("rentPrice")
    private double rentPrice;

    @SerializedName("active")
    private boolean active;

    @SerializedName("occupiedCount")
    private int occupiedCount;

    @SerializedName("maxTenants")
    private int maxTenants;

    // --- GETTERS (Crucial for fixing your error) ---

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public String getBuildingId() {
        return buildingId;
    }

    public String getTotalRooms() {
        return totalRooms;
    }

    public int getMaxTenants() {
        return maxTenants;
    }

    public int getOccupiedCount() {
        return occupiedCount;
    }

    public String getLandlordId() {
        return landlordId;
    }

    public String getDescription() {
        return description;
    }

    public double getRentPrice() {
        return rentPrice;
    }

    public boolean isActive() {
        return active;
    }

    // --- SETTERS ---

    public void setName(String name) { this.name = name; }
    public void setCode(String code) { this.code = code; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }
    public void setTotalRooms(String totalRooms) { this.totalRooms = totalRooms; }
    public void setMaxTenants(int maxTenants) { this.maxTenants = maxTenants; }
    public void setOccupiedCount(int occupiedCount) { this.occupiedCount = occupiedCount; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }
    public void setDescription(String description) { this.description = description; }
    public void setRentPrice(double rentPrice) { this.rentPrice = rentPrice; }
    public void setActive(boolean active) { this.active = active; }
}