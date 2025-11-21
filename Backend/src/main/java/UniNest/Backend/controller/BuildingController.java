package UniNest.Backend.controller;

import UniNest.Backend.dto.BuildingRequest;
import UniNest.Backend.model.Building;
import UniNest.Backend.service.BuildingService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/buildings")


public class BuildingController {

    @Autowired
    private BuildingService buildingService;

    @PostMapping("/create")
    public String createBuilding(@RequestBody BuildingRequest request) {
        return buildingService.createBuilding(request);
    }

    @GetMapping("/getAll")
    public List<Building> getAllBuildings() {
        return buildingService.getAllBuildings();
    }

}
