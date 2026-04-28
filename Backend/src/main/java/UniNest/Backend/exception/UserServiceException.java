package UniNest.Backend.exception;

import org.springframework.http.HttpStatus;

import java.util.concurrent.ExecutionException;

import lombok.Getter;

@Getter
public class UserServiceException extends RuntimeException {
    private final HttpStatus status;

    public UserServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}

