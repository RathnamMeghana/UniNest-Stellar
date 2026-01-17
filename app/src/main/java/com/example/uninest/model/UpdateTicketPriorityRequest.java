package com.example.uninest.model;

public class UpdateTicketPriorityRequest {
    private String ticketId;
    private String priority;

    public void setTicketId(String ticketId) { this.ticketId = ticketId; }
    public void setPriority(String priority) { this.priority = priority; }
}