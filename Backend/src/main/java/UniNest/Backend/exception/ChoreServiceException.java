package UniNest.Backend.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class ChoreServiceException extends RuntimeException {
    private final HttpStatus status;

    public ChoreServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}