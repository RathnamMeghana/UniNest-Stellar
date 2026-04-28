package com.example.uninest.model;

public class HomeAlert {
    private String id;
    private String title;
    private String subtitle;
    private String type;
    private String targetScreen;
    private String entityId;
    private long createdAt;
    private long eventTime;
    private String metaOverride;
    private boolean dismissible = true;

    public HomeAlert() {}

    public HomeAlert(String id, String title, String subtitle, String type,
                     String targetScreen, String entityId, long createdAt, long eventTime) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.type = type;
        this.targetScreen = targetScreen;
        this.entityId = entityId;
        this.createdAt = createdAt;
        this.eventTime = eventTime;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTargetScreen() { return targetScreen; }
    public void setTargetScreen(String targetScreen) { this.targetScreen = targetScreen; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getEventTime() { return eventTime; }
    public void setEventTime(long eventTime) { this.eventTime = eventTime; }

    public String getMetaOverride() { return metaOverride; }
    public void setMetaOverride(String metaOverride) { this.metaOverride = metaOverride; }

    public boolean isDismissible() { return dismissible; }
    public void setDismissible(boolean dismissible) { this.dismissible = dismissible; }
}
