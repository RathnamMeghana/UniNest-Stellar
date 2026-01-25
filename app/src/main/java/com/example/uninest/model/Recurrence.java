package com.example.uninest.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Recurrence {

    @SerializedName("frequency")
    private String frequency; // "WEEKLY", "DAILY", "MONTHLY"

    @SerializedName("interval")
    private int interval;

    @SerializedName("daysOfWeek")
    private List<String> daysOfWeek;


    @SerializedName("endDate")
    private FirestoreTimestamp endDate;


    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = interval; }

    public List<String> getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(List<String> daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public FirestoreTimestamp getEndDate() { return endDate; }
    public void setEndDate(FirestoreTimestamp endDate) { this.endDate = endDate; }
}