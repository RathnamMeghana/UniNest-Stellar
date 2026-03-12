package UniNest.Backend.controller;

import UniNest.Backend.dto.CalendarEventDTO;
import UniNest.Backend.service.CalendarService;
import jakarta.validation.Valid; // Ensure this is imported
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {

    @Autowired
    private  CalendarService calendarService;


    @PreAuthorize("hasAnyRole('LETTINGAGENT', 'TENANT')")
    @PostMapping("/create")
    public CalendarEventDTO.Response createEvent(
            @Valid @RequestBody CalendarEventDTO.Create request,
            @RequestParam String userId
    ) {
        request.sanitize();
        return calendarService.create(request, userId);
    }

    //@PreAuthorize("hasRole('LETTINGAGENT') or hasRole('TENANT')")
    @PreAuthorize("hasAnyRole('LETTINGAGENT', 'TENANT')")
    @GetMapping("/getByApartment/{houseCode}")
    public List<CalendarEventDTO.Response> getEventsForHouse(
            @PathVariable String houseCode,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end
    ) {
        if (start != null && end != null) {
            return calendarService.getForRange(houseCode, start, end);
        } else {
            return calendarService.getForapartment(houseCode);
        }
    }

    @PreAuthorize("hasRole('TENANT')")
    @GetMapping("/getByUser/{userId}")
    public List<CalendarEventDTO.Response> getEventsForUser(@PathVariable String userId) {
        return calendarService.getForUser(userId);
    }

    // Fixed @PreAuthorize typo
    //@PreAuthorize("hasRole('LETTINGAGENT') or hasRole('TENANT')")
    @PreAuthorize("hasAnyRole('LETTINGAGENT', 'TENANT')")
    @PutMapping("/update/{eventId}")
    public CalendarEventDTO.Response updateEvent(
            @PathVariable String eventId,
            @Valid @RequestBody CalendarEventDTO.Update request // Added @Valid here too
    ) {
        request.sanitize();
        return calendarService.update(eventId, request);
    }

    // Fixed @PreAuthorize typo
   // @PreAuthorize("hasRole('LETTINGAGENT') or hasRole('TENANT')")
    @PreAuthorize("hasAnyRole('LETTINGAGENT', 'TENANT')")
    @DeleteMapping("/delete/{eventId}")
    public String delete(@PathVariable String eventId) {
        calendarService.delete(eventId);
        return "Event deleted";
    }
}