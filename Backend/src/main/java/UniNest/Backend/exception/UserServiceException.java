package UniNest.Backend.exception;

import java.util.concurrent.ExecutionException;

public class UserServiceException extends Throwable {
    public UserServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
