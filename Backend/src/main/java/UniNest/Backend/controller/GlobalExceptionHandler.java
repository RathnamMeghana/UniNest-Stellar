package UniNest.Backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

import UniNest.Backend.exception.ApartmentServiceException;
import UniNest.Backend.exception.BuildingServiceException;
import UniNest.Backend.exception.RoomServiceException;
import UniNest.Backend.exception.TicketNotFoundException;
import UniNest.Backend.exception.TicketServiceException;
import UniNest.Backend.exception.UserServiceException;
import UniNest.Backend.exception.TenantNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage())
                );

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(ApartmentServiceException.class)
    public ResponseEntity<String> handleApartmentException(ApartmentServiceException ex) {
        // Priority 1: Use the status attached to the exception
        if (ex.getStatus() != null) {
            return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
        }

        // Priority 2: Fallback logic if status is somehow still null
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ex.getMessage().contains("does not exist")) status = HttpStatus.NOT_FOUND;
        if (ex.getMessage().contains("not authorized")) status = HttpStatus.FORBIDDEN;

        return ResponseEntity.status(status).body(ex.getMessage());
    }

    @ExceptionHandler(BuildingServiceException.class)
    public ResponseEntity<String> handleBuildingException(BuildingServiceException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
    }

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<String> handleTenantNotFound(TenantNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }


    @ExceptionHandler(RoomServiceException.class)
    public ResponseEntity<String> handleRoomException(RoomServiceException ex) {
        // Logic to differentiate based on message content
        if (ex.getMessage().contains("does not exist")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
        if (ex.getMessage().contains("not authorized")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<String> handleTicketNotFoundException(TicketNotFoundException ex) {
        // Logic to differentiate based on message content
        if (ex.getMessage().contains("does not exist")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
        if (ex.getMessage().contains("not authorized")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }

    @ExceptionHandler(TicketServiceException.class)
    public ResponseEntity<String> handleTicketServiceException(TicketServiceException ex) {
        // Logic to differentiate based on message content
        if (ex.getMessage().contains("does not exist")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
        if (ex.getMessage().contains("not authorized")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }

    @ExceptionHandler(UserServiceException.class)
    public ResponseEntity<String> handleUserServiceException(UserServiceException ex) {
        // Logic to differentiate based on message content
        if (ex.getMessage().contains("does not exist")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
        if (ex.getMessage().contains("not authorized")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }




}
