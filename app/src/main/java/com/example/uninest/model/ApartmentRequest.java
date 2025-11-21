package com.example.uninest.model;
import java.io.Serializable;
import java.sql.Timestamp;

public class ApartmentRequest implements Serializable {

        private String buildingId;
        private String name;
        private String description;
        private Double rentPrice;
        private Boolean active;

    public String getBuildingId() { return buildingId; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }


    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getRentPrice() { return rentPrice; }
    public void setRentPrice(Double rentPrice) { this.rentPrice = rentPrice; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    }

}
