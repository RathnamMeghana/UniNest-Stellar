package UniNest.Backend.controller;

import UniNest.Backend.dto.ChorePredictionRequest;
import UniNest.Backend.dto.ChorePredictionResponse;
import UniNest.Backend.service.ChoreSchedulingService;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/chores")
public class ChoreController {

    @Autowired
    private ChoreSchedulingService choreService;

    @PostMapping("/assign")
    public ChorePredictionResponse assignChore(
            @RequestBody ChorePredictionRequest request
    ) {
        return choreService.predictAssignee(request);
    }
}
