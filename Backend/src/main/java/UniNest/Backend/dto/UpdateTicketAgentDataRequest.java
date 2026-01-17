package UniNest.Backend.dto;

public class UpdateTicketAgentDataRequest {
    private String ticketId;
    private String response;
    private String arrivalDate;

    // Getters and Setters
    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public String getArrivalDate() { return arrivalDate; }
    public void setArrivalDate(String arrivalDate) { this.arrivalDate = arrivalDate; }
}