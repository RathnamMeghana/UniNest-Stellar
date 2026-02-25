package UniNest.Backend.exception;
import org.springframework.http.HttpStatus;

public class ApartmentServiceException extends RuntimeException {

    private final HttpStatus status;

    public ApartmentServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

