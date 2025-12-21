package UniNest.Backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateTicketStatusRequest {
    @NotBlank(message = "ticketId is required")
    private String ticketId;
    @NotBlank(message = "status is required")
    @Pattern(
            regexp = "Open|In_Process|Resolved|Closed",
            message = "status must be Open, In_Process, Resolved or Closed"
    )
    private String status;

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
