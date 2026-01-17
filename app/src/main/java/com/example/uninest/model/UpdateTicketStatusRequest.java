package com.example.uninest.model;

public class UpdateTicketStatusRequest {
    private String ticketId;
    private String status;

    public void setTicketId(String ticketId) { this.ticketId = ticketId; }
    public void setStatus(String status) { this.status = status; }
}