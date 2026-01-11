package UniNest.Backend.exception;

public class ApartmentServiceException extends RuntimeException {
    public ApartmentServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
