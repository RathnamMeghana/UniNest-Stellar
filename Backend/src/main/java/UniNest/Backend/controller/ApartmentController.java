package UniNest.Backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import UniNest.Backend.dto.ApartmentRequests;
import UniNest.Backend.model.Apartment;
import UniNest.Backend.model.Room;
import UniNest.Backend.model.User;
import UniNest.Backend.service.ApartmentService;
import UniNest.Backend.service.UserService;
import UniNest.Backend.service.RoomService;


@RestController
@ComponentScan
@RequestMapping("/apartments")

public class ApartmentController {
    @Autowired
    private ApartmentService apartmentService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoomService roomService;

    @PostMapping("/create")
    public String createApartment(@RequestBody ApartmentRequests request) {
        return apartmentService.createApartment(request);
    }

    @GetMapping("/getAll")
    public List<Apartment> getAllApartments() {


        return apartmentService.getAllApartments();
    }


    @GetMapping("/{houseCode}/users")
    public ResponseEntity<List<User>> getUsersByApartment(
            @PathVariable String houseCode
    ) {
        List<User> users = userService.getUsersForApartment(houseCode);

        if (users.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(users);
    }


    @PostMapping("/{houseCode}/addRoom")
    public ResponseEntity<String> addRoom(
            @PathVariable String houseCode,
            @RequestBody Room room
    ) {
        try {
            String roomId = roomService.addRoom(houseCode, room);
            return ResponseEntity.ok("Room added with ID: " + roomId);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error adding room: " + e.getMessage());
        }
    }

    @GetMapping("/{houseCode}/rooms")
    public ResponseEntity<List<Room>> getRooms(@PathVariable String houseCode) {
        try {
            List<Room> rooms = roomService.getRooms(houseCode);
            if (rooms.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(rooms);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/tenants/{email}/remove")
    public ResponseEntity<?> removeTenant(@PathVariable String email) {
        apartmentService.removeTenantFromApartment(email);
        return ResponseEntity.ok("Tenant removed from apartment.");
    }
}
