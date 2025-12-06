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
import UniNest.Backend.model.User;
import UniNest.Backend.service.ApartmentService;
import UniNest.Backend.service.UserService;


@RestController
@ComponentScan
@RequestMapping("/apartments")

public class ApartmentController {
    @Autowired
    private ApartmentService apartmentService;

    @Autowired // <--- ADD THIS ANNOTATION
    private UserService userService;

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

    @DeleteMapping("/tenants/{email}/remove")
    public ResponseEntity<?> removeTenant(@PathVariable String email) {
        apartmentService.removeTenantFromApartment(email);
        return ResponseEntity.ok("Tenant removed from apartment.");
    }



}
