package UniNest.Backend.exception;

public class BuildingServiceException extends RuntimeException {
    public BuildingServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}