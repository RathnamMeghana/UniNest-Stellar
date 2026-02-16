package UniNest.Backend.controller;

import UniNest.Backend.dto.BuildingRequest;
import UniNest.Backend.model.Building;
import UniNest.Backend.service.BuildingService;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import UniNest.Backend.util.SanitizationUtil;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/buildings")


public class BuildingController {

    @Autowired
    private BuildingService buildingService;
    @PreAuthorize("hasRole('LETTINGAGENT')")
    @PostMapping("/create")
    public String createBuilding(@Valid @RequestBody BuildingRequest request) {
        request.setName(SanitizationUtil.sanitize(request.getName()));
        request.setAddressLine1(SanitizationUtil.sanitize(request.getAddressLine1()));
        request.setCity(SanitizationUtil.sanitize(request.getCity()));
        request.setPostcode(SanitizationUtil.sanitize(request.getPostcode()));
        request.setCountry(SanitizationUtil.sanitize(request.getCountry()));
        request.setLandlordId(SanitizationUtil.sanitize(request.getLandlordId()));
        return buildingService.createBuilding(request);
    }


    //@PreAuthorize("hasRole('TENANT')")
    @PreAuthorize("hasRole('LETTINGAGENT')")
    @GetMapping("/getAll")
    public List<Building> getAllBuildings() {

        return buildingService.getAllBuildings();
    }

    @PreAuthorize("hasRole('LETTINGAGENT')")
    @GetMapping("/byLandlord")
    public List<Building> getBuildingsByLandlord(@RequestParam String landlordId) {
        landlordId = SanitizationUtil.sanitize(landlordId);
        return buildingService.getBuildingsByLandlord(landlordId);
    }



    @PreAuthorize("hasRole('LETTINGAGENT')")
    @GetMapping("/{id}")
    public ResponseEntity<Building> getBuildingById(@PathVariable String id) {
        // 1. Sanitize
        String cleanId = SanitizationUtil.sanitize(id);

        // 2. Call service (Service already throws BuildingServiceException if not found)
        Building building = buildingService.getBuildingById(cleanId);

        // 3. Return success
        return new ResponseEntity<>(building, HttpStatus.OK);
    }
}
