package UniNest.Backend.controller;

import UniNest.Backend.dto.BillRequest;
import UniNest.Backend.dto.CalendarEventDTO;
import UniNest.Backend.service.BillService;
import UniNest.Backend.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;
    private final BillService billService;
    // ==================================================
    // CREATE EVENT
    // ==================================================
    @PostMapping("/create")
    public CalendarEventDTO.Response createEvent(
            @RequestBody CalendarEventDTO.Create request,
            @RequestParam String userId
    ) {
        //  Create calendar event
        CalendarEventDTO.Response created =
                calendarService.create(request, userId);

        //  If bill, create bill AFTER event success
        if (request.getType().toString().equalsIgnoreCase("BILL_DUE")) {
            BillRequest billRequest =
                    BillMapper.fromCalendar(request, userId);
            billService.createBill(billRequest);
        }

        return created;
    }


    public class BillMapper {

        public static BillRequest fromCalendar(
                CalendarEventDTO.Create req,
                String userId
        ) {
            BillRequest bill = new BillRequest();

            bill.setTitle(req.getTitle());
            bill.setTotalAmount(req.getAmount());
            bill.setHouseCode(req.getHouseCode());

            bill.setCreatorId(userId);
            bill.setDueDate(parseIsoDate(req.getStartDate()));
            bill.setActive(true);

            // TEMP: assign creator (replace later with roommates)
            bill.setRoommateIds(List.of(userId));

            return bill;
        }

        private static Date parseIsoDate(String iso) {
            try {
                return new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss'Z'",
                        Locale.US
                ).parse(iso);
            } catch (Exception e) {
                throw new RuntimeException("Invalid ISO date: " + iso);
            }
        }
    }

    private static Date parseIsoDate(String iso) {
        try {
            return new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    Locale.US
            ).parse(iso);
        } catch (Exception e) {
            throw new RuntimeException("Invalid date: " + iso);
        }
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
