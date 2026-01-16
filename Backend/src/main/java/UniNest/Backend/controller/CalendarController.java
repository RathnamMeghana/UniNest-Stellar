package UniNest.Backend.controller;

import UniNest.Backend.dto.CalendarEventDTO;
import UniNest.Backend.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    // ==================================================
    // CREATE EVENT
    // ==================================================
    @PostMapping("/create")
    public CalendarEventDTO.Response createEvent(
            @RequestBody CalendarEventDTO.Create request,
            @RequestParam String userId
    ) {
        return calendarService.create(request, userId);
    }


    // get events by apartment

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


    // GET EVENTS FOR USER

    @GetMapping("/getByUser/{userId}")
    public List<CalendarEventDTO.Response> getEventsForUser(
            @PathVariable String userId
    ) {
        return calendarService.getForUser(userId);
    }


    // update event

    @PutMapping("/update/{eventId}")
    public CalendarEventDTO.Response updateEvent(
            @PathVariable String eventId,
            @RequestBody CalendarEventDTO.Update request
    ) {
        return calendarService.update(eventId, request);
    }


    // DELETE EVENT

    @DeleteMapping("/delete/{eventId}")
    public String delete(@PathVariable String eventId) {
        calendarService.delete(eventId);
        return "Event deleted";
    }

}
