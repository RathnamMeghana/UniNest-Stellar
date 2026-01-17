package com.example.uninest.model;


import com.google.gson.annotations.SerializedName;

public class Calendar {
    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    private FirestoreTimestamp startDate;
    private FirestoreTimestamp endDate;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public FirestoreTimestamp getStartDate() { return startDate; }
    public void setStartDate(FirestoreTimestamp startDate) { this.startDate = startDate; }

    public FirestoreTimestamp getEndDate() { return endDate; }
    public void setEndDate(FirestoreTimestamp endDate) { this.endDate = endDate; }
}
