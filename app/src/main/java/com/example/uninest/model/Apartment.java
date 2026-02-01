package com.example.uninest.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Apartment implements Serializable {

    @SerializedName("buildingId")
    private String buildingId;

    @SerializedName("name")
    private String name;

    // Based on logs: [{"totalRooms":"9", ...}] - this must be a String
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

    // Handles the nested createdAt object from the logs
    @SerializedName("createdAt")
    private CreatedAt createdAt;

    private int occupiedCount;

    // Static inner class to handle the Timestamp structure
    public static class CreatedAt {
        private long seconds;
        private int nanos;

        public long getSeconds() { return seconds; }
        public void setSeconds(long seconds) { this.seconds = seconds; }
        public int getNanos() { return nanos; }
        public void setNanos(int nanos) { this.nanos = nanos; }
    }

    // Getters and Setters
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

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getRentPrice() { return rentPrice; }
    public void setRentPrice(double rentPrice) { this.rentPrice = rentPrice; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public CreatedAt getCreatedAt() { return createdAt; }
    public void setCreatedAt(CreatedAt createdAt) { this.createdAt = createdAt; }

    public int getOccupiedCount() { return occupiedCount; }
    public void setOccupiedCount(int occupiedCount) { this.occupiedCount = occupiedCount; }
}