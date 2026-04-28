package UniNest.Backend.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class TicketServiceException extends RuntimeException {
    private final HttpStatus status;

    public TicketServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
