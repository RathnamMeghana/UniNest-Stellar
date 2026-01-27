package com.example.uninest.model;

import com.google.gson.annotations.SerializedName;

public class User {
    //private String id;
    private String apartmentId;
    private String email;
    private String houseCode;
    private String role;
    private String firstName;
    private String lastName;

    @SerializedName("id")
    private String id;

    //public String getId() { return id; }
    //public void setId(String id) { this.id = id; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getApartmentId() { return apartmentId; }
    public void setApartmentId(String apartmentId) { this.apartmentId = apartmentId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getHouseCode() { return houseCode; }
    public void setHouseCode(String houseCode) { this.houseCode = houseCode; }


    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }


    // Helper to get full name easily
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return email;
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
