package UniNest.Backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateTicketPriorityRequest {
    @NotBlank(message = "ticketId is required")
    private String ticketId;
    @NotBlank(message = "priority is required")
    @Pattern(
            regexp = "Low|Medium|High",
            message = "priority must be Low, Medium, High "
    )
    private String priority;

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}

