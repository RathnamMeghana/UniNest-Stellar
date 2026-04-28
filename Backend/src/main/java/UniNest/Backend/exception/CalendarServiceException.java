package UniNest.Backend.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class CalendarServiceException extends RuntimeException {
    private final HttpStatus status;

    public CalendarServiceException(String message, HttpStatus status) {

        super(message);
        this.status = status;
    }
}
