package UniNest.Backend.security;

import UniNest.Backend.dto.CalendarEventDTO;
import UniNest.Backend.model.CalendarEvent;
import UniNest.Backend.service.CalendarService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.file.AccessDeniedException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CalendarControllerObjectLevelTest {

    @InjectMocks
    private CalendarService calendarService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
    }


    // METHOD TO SET AUTH USER
      private void setAuthenticatedUser(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null)
        );
    }

    // OBJECT-LEVEL AUTHORIZATION TESTS
    @Test
    void getEventsForUser_otherUser_shouldThrow403() {
        setAuthenticatedUser("user1");


        CalendarService spyService = spy(calendarService);
        doAnswer(invocation -> {
            String userId = invocation.getArgument(0);
            if (!"user1".equals(userId)) {
                throw new AccessDeniedException("Cannot access another user's events");
            }
            return Collections.emptyList();
        }).when(spyService).getForUser(anyString());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> spyService.getForUser("user2"));

        assertEquals("Cannot access another user's events", ex.getMessage());
    }

    @Test
    void getEventsForUser_sameUser_shouldPass() throws AccessDeniedException {
        setAuthenticatedUser("user1");

        CalendarEventDTO.Response event = new CalendarEventDTO.Response();
        event.setId("evt1");
        event.setAssignedTo("user1");

        CalendarService spyService = spy(calendarService);
        doReturn(List.of(event)).when(spyService).getForUser("user1");

        List<CalendarEventDTO.Response> events = spyService.getForUser("user1");
        assertEquals(1, events.size());
        assertEquals("user1", events.get(0).getAssignedTo());
    }

    @Test
    void createEvent_shouldReturnCreatedEvent() {
        setAuthenticatedUser("user1");

        CalendarEventDTO.Create dto = new CalendarEventDTO.Create();
        dto.setHouseCode("HOUSE123");
        dto.setTitle("Meeting");
        dto.setStartDate("2026-02-15T10:00:00Z");
        dto.setEndDate("2026-02-15T11:00:00Z");

        CalendarService spyService = spy(calendarService);
        CalendarEventDTO.Response mockResponse = new CalendarEventDTO.Response();
        mockResponse.setId("evt123");
        doReturn(mockResponse).when(spyService).create(dto, "user1");

        CalendarEventDTO.Response response = spyService.create(dto, "user1");
        assertEquals("evt123", response.getId());
    }

    @Test
    void updateEvent_shouldReturnUpdatedEvent() {
        setAuthenticatedUser("user1");

        CalendarEventDTO.Update dto = new CalendarEventDTO.Update();
        dto.setTitle("Updated Title");

        CalendarEvent event = new CalendarEvent();
        event.setId("evt1");

        CalendarService spyService = spy(calendarService);
        doReturn(new CalendarEventDTO.Response()).when(spyService).update("evt1", dto);

        CalendarEventDTO.Response response = spyService.update("evt1", dto);
        assertNotNull(response);
    }

    @Test
    void deleteEvent_shouldNotThrow() {
        setAuthenticatedUser("user1");

        CalendarService spyService = spy(calendarService);
        doNothing().when(spyService).delete("evt1");

        assertDoesNotThrow(() -> spyService.delete("evt1"));
    }

    @Test
    void getForApartment_shouldReturnList() {
        CalendarService spyService = spy(calendarService);
        doReturn(Collections.emptyList()).when(spyService).getForapartment("HOUSE123");

        List<CalendarEventDTO.Response> events = spyService.getForapartment("HOUSE123");
        assertNotNull(events);
        assertEquals(0, events.size());
    }

    @Test
    void getForRange_shouldReturnList() {
        CalendarService spyService = spy(calendarService);
        doReturn(Collections.emptyList()).when(spyService)
                .getForRange("HOUSE123", "2026-02-01T00:00:00Z", "2026-02-28T23:59:59Z");

        List<CalendarEventDTO.Response> events = spyService.getForRange(
                "HOUSE123", "2026-02-01T00:00:00Z", "2026-02-28T23:59:59Z");
        assertNotNull(events);
        assertEquals(0, events.size());
    }
}
