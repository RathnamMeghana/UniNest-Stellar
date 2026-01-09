package UniNest.Backend.controller;

import UniNest.Backend.dto.BuildingRequest;
import UniNest.Backend.model.Building;
import UniNest.Backend.service.BuildingService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import UniNest.Backend.util.SanitizationUtil;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/buildings")


public class BuildingController {

    @Autowired
    private BuildingService buildingService;

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

    @GetMapping("/getAll")
    public List<Building> getAllBuildings() {
        return buildingService.getAllBuildings();
    }

    @GetMapping("/byLandlord")
    public List<Building> getBuildingsByLandlord(@RequestParam String landlordId) {
        landlordId = SanitizationUtil.sanitize(landlordId);
        return buildingService.getBuildingsByLandlord(landlordId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Building> getBuildingById(@PathVariable String id) {
        try {
            // Sanitize input
            String cleanId = SanitizationUtil.sanitize(id);

            Building building = buildingService.getBuildingById(cleanId);

            if (building != null) {
                return new ResponseEntity<>(building, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
