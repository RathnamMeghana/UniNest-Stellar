package com.example.uninest.model;

public class CalendarRequest {

    private String houseCode;
    private String type;            // "CHORE" or "OTHER"
    private String title;
    private String description;

    private long startSeconds;      // epoch seconds
    private long endSeconds;

    private boolean allDay;

    private String createdBy;
    private String assignedTo;

    private String relatedChoreId;  // ONLY for CHORE (nullable)

    // --- CHORE-specific fields ---
    private String choreRoom;
    private int choreDuration;       // minutes
    private int choreDifficulty;     // 1-5
    private int choreFrequency;      // per week

    // -------- getters --------
    public String getHouseCode() { return houseCode; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getStartSeconds() { return startSeconds; }
    public long getEndSeconds() { return endSeconds; }
    public boolean isAllDay() { return allDay; }
    public String getCreatedBy() { return createdBy; }
    public String getAssignedTo() { return assignedTo; }
    public String getRelatedChoreId() { return relatedChoreId; }

    public String getChoreRoom() { return choreRoom; }
    public int getChoreDuration() { return choreDuration; }
    public int getChoreDifficulty() { return choreDifficulty; }
    public int getChoreFrequency() { return choreFrequency; }

    // -------- setters --------
    public void setHouseCode(String houseCode) { this.houseCode = houseCode; }
    public void setType(String type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStartSeconds(long startSeconds) { this.startSeconds = startSeconds; }
    public void setEndSeconds(long endSeconds) { this.endSeconds = endSeconds; }
    public void setAllDay(boolean allDay) { this.allDay = allDay; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public void setRelatedChoreId(String relatedChoreId) { this.relatedChoreId = relatedChoreId; }

    public void setChoreRoom(String choreRoom) { this.choreRoom = choreRoom; }
    public void setChoreDuration(int choreDuration) { this.choreDuration = choreDuration; }
    public void setChoreDifficulty(int choreDifficulty) { this.choreDifficulty = choreDifficulty; }
    public void setChoreFrequency(int choreFrequency) { this.choreFrequency = choreFrequency; }
}
