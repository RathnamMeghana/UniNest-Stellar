package UniNest.Backend.exception;

import org.springframework.http.HttpStatus;

public class BuildingServiceException extends RuntimeException {

    private final HttpStatus status;

    public BuildingServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

