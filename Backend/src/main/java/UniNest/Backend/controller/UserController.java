package UniNest.Backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import UniNest.Backend.exception.UserServiceException;
import UniNest.Backend.model.User;
import UniNest.Backend.service.UserService;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    // Endpoint to get roommates by House Code
    @PreAuthorize("hasRole('LETTINGAGENT') or hasRole('TENANT')")
    @GetMapping("/roommates/{houseCode}")
    public List<User> getRoommates(@PathVariable String houseCode) {
        try {
            // Reusing your existing service method
            return userService.getUsersForApartment(houseCode);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching roommates", e);
        }
    }
}