package UniNest.Backend.controller;

import UniNest.Backend.dto.CalendarEventDTO;
import UniNest.Backend.exception.ChoreServiceException;
import UniNest.Backend.service.CalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.cloud.Timestamp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CalendarControllerTest {

    @InjectMocks
    private CalendarController calendarController;

    @Mock
    private CalendarService calendarService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createEvent_success() throws Exception {
        // Arrange
        CalendarEventDTO.Create request = new CalendarEventDTO.Create();
        request.setTitle("Test Event");
        request.setHouseCode("HOUSE123");
        request.setAssignedTo("user123");
        request.setAllDay(false);

        Timestamp now = Timestamp.now();
        request.setStartDate(now);
        request.setEndDate(now);

        // Mock service to return a Response object
        CalendarEventDTO.Response mockResponse = new CalendarEventDTO.Response();
        mockResponse.setTitle("Test Event");
        mockResponse.setHouseCode("HOUSE123");
        mockResponse.setAssignedTo("user123");

        when(calendarService.create(any(CalendarEventDTO.Create.class), anyString()))
                .thenReturn(mockResponse);

        // Act
        CalendarEventDTO.Response result = calendarController.createEvent(request, "user123");

        // Assert
        assertNotNull(result);
        assertEquals("Test Event", result.getTitle());
        assertEquals("HOUSE123", result.getHouseCode());
        assertEquals("user123", result.getAssignedTo());

        verify(calendarService, times(1))
                .create(any(CalendarEventDTO.Create.class), eq("user123"));
    }

    @Test
    void createEvent_throwsException_whenServiceFails() throws Exception {
        CalendarEventDTO.Create request = new CalendarEventDTO.Create();
        request.setTitle("Fail Event");
        request.setHouseCode("HOUSE123");
        request.setAssignedTo("user123");
        request.setAllDay(false);

        Timestamp now = Timestamp.now();
        request.setStartDate(now);
        request.setEndDate(now);

        // Mock service to throw
        when(calendarService.create(any(CalendarEventDTO.Create.class), anyString()))
                .thenThrow(new ChoreServiceException("Service failed", null));

        // Act & Assert
        ChoreServiceException ex = assertThrows(ChoreServiceException.class,
                () -> calendarController.createEvent(request, "user123"));
        assertEquals("Service failed", ex.getMessage());

        verify(calendarService, times(1))
                .create(any(CalendarEventDTO.Create.class), eq("user123"));
    }
}
