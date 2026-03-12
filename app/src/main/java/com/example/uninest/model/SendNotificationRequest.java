package com.example.uninest.model;

import java.util.List;

public class SendNotificationRequest {

    private String houseCode;
    private List<String> tenantUserIds;
    private String title;
    private String body;

    public SendNotificationRequest(String houseCode, List<String> tenantUserIds, String title, String body) {
        this.houseCode = houseCode;
        this.tenantUserIds = tenantUserIds;
        this.title = title;
        this.body = body;
    }

    public String getHouseCode() {
        return houseCode;
    }

    public List<String> getTenantUserIds() {
        return tenantUserIds;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }
}