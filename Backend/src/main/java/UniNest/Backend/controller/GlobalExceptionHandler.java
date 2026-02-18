package UniNest.Backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

import UniNest.Backend.exception.ApartmentServiceException;
import UniNest.Backend.exception.BillServiceException;
import UniNest.Backend.exception.BuildingServiceException;
import UniNest.Backend.exception.CalendarServiceException;
import UniNest.Backend.exception.ChoreServiceException;
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

    // Inside GlobalExceptionHandler.java

    @ExceptionHandler(java.nio.file.AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(java.nio.file.AccessDeniedException ex) {
        // This will now return the 403 Forbidden your test expects
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }


    @ExceptionHandler(ApartmentServiceException.class)
    public ResponseEntity<String> handleApartmentException(ApartmentServiceException ex) {
        // Use the status attached to the exception
        if (ex.getStatus() != null) {
            return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
        }

        // Fallback logic if status is  still null
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

    @ExceptionHandler(BillServiceException.class)
    public ResponseEntity<String> handleBillServiceException(BillServiceException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
    }

    @ExceptionHandler(UserServiceException.class)
    public ResponseEntity<String> handleUserServiceException(UserServiceException ex) {
        HttpStatus status = (ex.getStatus() != null) ? ex.getStatus() : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(ex.getMessage());
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


    @ExceptionHandler(CalendarServiceException.class)
    public ResponseEntity<String> handleCalendarException(CalendarServiceException ex) {

        return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
    }

    @ExceptionHandler(ChoreServiceException.class)
    public ResponseEntity<String> handleChoreException(ChoreServiceException ex) {
        // Null-safe check: default to 500 if status is null
        HttpStatus status = (ex.getStatus() != null) ? ex.getStatus() : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        // This will turn the service's "houseCode cannot be empty" error into a 400
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }





}
