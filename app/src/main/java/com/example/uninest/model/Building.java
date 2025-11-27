package com.example.uninest.model;

public class Building {
    private String id;
    private String name;
    private String addressLine1;
    private String city;
    private String postcode;
    private String country;
    private String landlordId;
    private Boolean active;
    


    // Getters & setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPostcode() { return postcode; }
    public void setPostcode(String postcode) { this.postcode = postcode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getLandlordId() { return landlordId; }
    public void setLandlordId(String landlordId) { this.landlordId = landlordId; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
