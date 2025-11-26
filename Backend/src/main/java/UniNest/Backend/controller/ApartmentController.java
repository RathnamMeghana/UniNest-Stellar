package UniNest.Backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import UniNest.Backend.dto.ApartmentRequests;
import UniNest.Backend.model.Apartment;
import UniNest.Backend.service.ApartmentService;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@ComponentScan
@RequestMapping("/apartments")

public class ApartmentController {
    @Autowired
    private ApartmentService apartmentService;

    @PostMapping("/create")
    public String createApartment(@RequestBody ApartmentRequests request) {
        return apartmentService.createApartment(request);
    }

    @GetMapping("/getAll")
    public List<Apartment> getAllApartments() {


        return apartmentService.getAllApartments();
    }
}
