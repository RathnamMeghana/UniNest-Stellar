package com.example.uninest.model;

public class UpdateTicketAgentDataRequest {
    private String ticketId;
    private String response;
    private String arrivalDate;

    public void setTicketId(String ticketId) { this.ticketId = ticketId; }
    public void setResponse(String response) { this.response = response; }
    public void setArrivalDate(String arrivalDate) { this.arrivalDate = arrivalDate; }
}