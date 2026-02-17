package UniNest.Backend.controller;

import com.google.cloud.Timestamp;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import UniNest.Backend.dto.ApartmentRequests;
import UniNest.Backend.dto.BulkApartmentWithRoomsRequest;
import UniNest.Backend.dto.RoomRequests;
import UniNest.Backend.exception.UserServiceException;
import UniNest.Backend.model.Apartment;
import UniNest.Backend.model.Room;
import UniNest.Backend.model.User;
import UniNest.Backend.service.ApartmentService;
import UniNest.Backend.service.UserService;
import UniNest.Backend.service.RoomService;
import jakarta.validation.Valid;
import UniNest.Backend.util.SanitizationUtil;

@RestController

@RequestMapping("/apartments")

public class ApartmentController {


    @Autowired
    private ApartmentService apartmentService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoomService roomService;

    @PreAuthorize("hasRole('LETTINGAGENT')")
    @PostMapping("/create")
    public String createApartment(@Valid @RequestBody ApartmentRequests request) {
        request.sanitize(); // This runs the cleaning logic
        return apartmentService.createApartment(request); // This passes the cleaned object
    }

    @PreAuthorize("hasRole('LETTINGAGENT') or hasRole('TENANT')")
    @GetMapping("/getAll")
    public List<Apartment> getAllApartments() {


        return apartmentService.getAllApartments();
    }


    @PreAuthorize("hasRole('LETTINGAGENT') or hasRole('TENANT')")
    @GetMapping("/{houseCode}/users")
    public ResponseEntity<List<User>> getUsersByApartment(
            @PathVariable String houseCode
    ) throws UserServiceException {
        List<User> users = userService.getUsersForApartment(houseCode);

        if (users.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasRole('LETTINGAGENT')")
    @PostMapping("/{houseCode}/addRoom")
    public ResponseEntity<String> addRoom(
            @PathVariable String houseCode,
            @Valid @RequestBody RoomRequests roomRequest
    ) {
        try {
            // Sanitize RoomRequest fields
            roomRequest.sanitize();

            RoomRequests room = new RoomRequests();
            room.setType(roomRequest.getType());
            room.setLabel(roomRequest.getLabel());

            String roomId = roomService.addRoom(houseCode, room);
            return ResponseEntity.ok("Room added with ID: " + roomId);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error adding room: " + e.getMessage());
        }
    }

    @PreAuthorize("hasRole('LETTINGAGENT')")
    @GetMapping("/{houseCode}/rooms")
    public ResponseEntity<List<RoomRequests>> getRooms(@PathVariable String houseCode) {
        try {
            List<RoomRequests> rooms = roomService.getRooms(houseCode);
            if (rooms.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(rooms);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PreAuthorize("hasRole('LETTINGAGENT')")
    @DeleteMapping("/tenants/{email}/remove")
    public ResponseEntity<?> removeTenant(@PathVariable String email) {
        apartmentService.removeTenantFromApartment(email);
        return ResponseEntity.ok("Tenant removed from apartment.");
    }

    @PreAuthorize("hasRole('LETTINGAGENT')")
    @PostMapping("/bulkWithRooms")
    public ResponseEntity<String> bulkWithRooms(@RequestBody BulkApartmentWithRoomsRequest request) {
        try {
            apartmentService.createApartmentsWithRooms(request);
            return ResponseEntity.ok("Apartments and rooms created successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Bulk apartment + room creation failed: " + e.getMessage());
        }
    }
    @PreAuthorize("hasRole('LETTINGAGENT')")
    @GetMapping("/getByBuilding")
    public List<Apartment> getApartmentsByBuilding(@RequestParam String buildingId) {
        return apartmentService.getApartmentsByBuilding(buildingId);
    }


}
