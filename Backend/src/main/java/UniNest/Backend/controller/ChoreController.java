package UniNest.Backend.controller;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;

import UniNest.Backend.dto.ChorePredictionRequest;
import UniNest.Backend.dto.ChorePredictionResponse;
import UniNest.Backend.dto.ChoreRequests;
import UniNest.Backend.exception.ChoreServiceException;
import UniNest.Backend.exception.UserServiceException;
import UniNest.Backend.model.Chore;
import UniNest.Backend.model.User;
import UniNest.Backend.service.ChoreSchedulingService;
import UniNest.Backend.service.ChoreService;
import UniNest.Backend.service.UserService;
import UniNest.Backend.util.SanitizationUtil;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/chores")



public class ChoreController {

    @Autowired
    private ChoreService choreService;

    @Autowired
    private ChoreSchedulingService choreSchedulingService;

    @Autowired
    private UserService userService;

    @PostMapping("/assign")
    public ChorePredictionResponse assignChore(
            @RequestBody ChorePredictionRequest request
    ) {
        return choreSchedulingService.predictAssignee(request);
    }


    @GetMapping("/getAll/{houseCode}")
    public List<ChoreRequests> getAllChoreByApartment(@PathVariable String houseCode) {
        houseCode = SanitizationUtil.sanitize(houseCode);

        return choreService.getAllChoreByApartment(houseCode);


    }

    @PostMapping("/addWithSmartAssign")
    public ChoreRequests addChoreWithSmartAssign(
            @RequestParam String houseCode,
            @RequestBody ChoreRequests chore
    ) throws UserServiceException {

        // sanitize only request primitives
        houseCode = SanitizationUtil.sanitize(houseCode);

        // 1️⃣ get roommates
        List<User> roommates = userService.getUsersForApartment(houseCode);
        if (roommates.isEmpty()) {
            throw new ChoreServiceException(
                    "No roommates found for houseCode " + houseCode, null
            );
        }

        // 2️⃣ build AI request
        ChorePredictionRequest predictionRequest = new ChorePredictionRequest();
        predictionRequest.setTaskName(chore.getTaskName());
        predictionRequest.setRoom(chore.getRoom());
        predictionRequest.setDifficultyScore(chore.getDifficultyScore());
        predictionRequest.setEstDurationMin(chore.getEstDurationMin());
        predictionRequest.setFrequencyPerWeek(chore.getFrequencyPerWeek());

        //  predict assignment
        ChorePredictionResponse assignment =
                choreSchedulingService.predictAssignee(predictionRequest);

        int predictedIndex = assignment.getAssignedTo();
        User assignedUser = roommates.get(predictedIndex % roommates.size());

        //  set assignment ONLY
        chore.setAssignedTo(assignedUser.getId());

        //  delegate EVERYTHING ELSE to service
        return choreService.addChore(
                houseCode,
                chore,
                assignedUser.getId() // createdBy
        );
    }


    @PatchMapping("/updateAssignmentByEmail")
    public ChoreRequests updateAssignmentByEmail(
            @RequestParam String houseCode,
            @RequestParam String taskName,
            @RequestParam String userEmail
    ) {
        houseCode = SanitizationUtil.sanitize(houseCode);
        taskName = SanitizationUtil.sanitize(taskName);
        userEmail = SanitizationUtil.sanitize(userEmail);

        return choreService.updateAssignmentByTaskNameAndUserEmail(houseCode, taskName, userEmail);
    }

    @PostMapping("/addWithAssignment")
    public ChoreRequests addChoreWithAssignment(
            @RequestParam String houseCode,
            @RequestParam String assignedUserId,
            @RequestBody ChoreRequests chore,
            @RequestParam String createdBy
    ) {

        houseCode = SanitizationUtil.sanitize(houseCode);
        assignedUserId = SanitizationUtil.sanitize(assignedUserId);
        createdBy = SanitizationUtil.sanitize(createdBy);

        chore.setAssignedTo(assignedUserId);

        return choreService.addChore(houseCode, chore, createdBy);
    }


}


