package UniNest.Backend.exception;

public class RoomServiceException extends RuntimeException {
    public RoomServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
