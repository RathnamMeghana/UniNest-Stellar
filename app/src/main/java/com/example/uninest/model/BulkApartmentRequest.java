package com.example.uninest.model;

import java.util.Map;

public class BulkApartmentRequest {

    private String buildingId;
    private int apartmentCount;
    private Map<String, Integer> roomTemplate; // e.g., Bedroom -> 4, Bathroom -> 2
    private String landlordId;

    private int maxTenants;

    public int getMaxTenants() { return maxTenants; }
    public void setMaxTenants(int maxTenants) { this.maxTenants = maxTenants; }


    // Getters & Setters
    public String getBuildingId() { return buildingId; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }

    public int getApartmentCount() { return apartmentCount; }
    public void setApartmentCount(int apartmentCount) { this.apartmentCount = apartmentCount; }

    public Map<String, Integer> getRoomTemplate() { return roomTemplate; }
    public void setRoomTemplate(Map<String, Integer> roomTemplate) { this.roomTemplate = roomTemplate; }

    public String getLandlordId() { return landlordId; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }
}
