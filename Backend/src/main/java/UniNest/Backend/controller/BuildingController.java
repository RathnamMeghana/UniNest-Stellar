package UniNest.Backend.controller;

import UniNest.Backend.dto.BuildingRequest;
import UniNest.Backend.model.Building;
import UniNest.Backend.service.BuildingService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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


}
