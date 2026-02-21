package UniNest.Backend.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class BillServiceException extends RuntimeException {
    private final HttpStatus status;

    public BillServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}