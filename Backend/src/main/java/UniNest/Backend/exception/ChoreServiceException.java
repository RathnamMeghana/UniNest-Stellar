package UniNest.Backend.exception;

public class ChoreServiceException extends RuntimeException {
    public ChoreServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
