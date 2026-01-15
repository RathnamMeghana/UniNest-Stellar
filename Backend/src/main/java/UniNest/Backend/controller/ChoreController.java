package UniNest.Backend.controller;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;

import UniNest.Backend.dto.ChorePredictionRequest;
import UniNest.Backend.dto.ChorePredictionResponse;
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


    @GetMapping("/getAll")
    public List<Chore> getAllChoreByApartment(@RequestParam String houseCode) {
        houseCode = SanitizationUtil.sanitize(houseCode);

        return choreService.getAllChoreByApartment(houseCode);


    }

    @PostMapping("/addWithSmartAssign")
    public Chore addChore(
            @RequestParam String houseCode,
            @RequestBody Chore chore
    ) throws UserServiceException {

        // Sanitize inputs
        houseCode = SanitizationUtil.sanitize(houseCode);
        chore.setTaskName(SanitizationUtil.sanitize(chore.getTaskName()));
        chore.setRoom(SanitizationUtil.sanitize(chore.getRoom()));

        // Ensure chore has an ID
        if (chore.getId() == null || chore.getId().isEmpty()) {
            chore.setId(UUID.randomUUID().toString());
        }

        // Set created timestamp
        chore.setCreatedAt(Timestamp.now());

        // Get roommates
        List<User> roommates = userService.getUsersForApartment(houseCode);
        if (roommates.isEmpty()) {
            throw new ChoreServiceException("No roommates found for houseCode " + houseCode, null);
        }

        // Prepare AI request
        ChorePredictionRequest predictionRequest = new ChorePredictionRequest();
        predictionRequest.setTaskName(chore.getTaskName());
        predictionRequest.setRoom(chore.getRoom());
        predictionRequest.setDifficultyScore(chore.getDifficultyScore());
        predictionRequest.setEstDurationMin(chore.getEstDurationMin());
        predictionRequest.setFrequencyPerWeek(chore.getFrequencyPerWeek());

        // AI predicts
        ChorePredictionResponse assignment = choreSchedulingService.predictAssignee(predictionRequest);
        int predictedIndex = assignment.getAssignedTo();
        User assignedUser = roommates.get(predictedIndex % roommates.size());

        // Assign to chore
        chore.setAssignedTo(assignedUser.getId());

        System.out.println("AssignedTo BEFORE Firestore save: " + chore.getAssignedTo());

        // Save chore once to Firestore under the correct nested path
        try {
            Firestore db = FirestoreClient.getFirestore();
            db.collection("apartments")           // use "apartments" instead of chores
                    .document(houseCode)
                    .collection("chores")
                    .document(chore.getId())
                    .set(chore)
                    .get(); // ensure write completes
        } catch (Exception e) {
            throw new ChoreServiceException("Failed to save chore with assignment", e);
        }

        return chore;
    }

    @PatchMapping("/updateAssignmentByEmail")
    public Chore updateAssignmentByEmail(
            @RequestParam String houseCode,
            @RequestParam String taskName,
            @RequestParam String userEmail
    ) {
        houseCode = SanitizationUtil.sanitize(houseCode);
        taskName = SanitizationUtil.sanitize(taskName);
        userEmail = SanitizationUtil.sanitize(userEmail);

        return choreService.updateAssignmentByTaskNameAndUserEmail(
                houseCode,
                taskName,
                userEmail
        );
    }

    @PostMapping("/addWithAssignment")
    public Chore addChoreWithAssignment(
            @RequestParam String houseCode,
            @RequestParam String userEmail,
            @RequestBody Chore chore
    ) {
        houseCode = SanitizationUtil.sanitize(houseCode);
        userEmail = SanitizationUtil.sanitize(userEmail);
        chore.setTaskName(SanitizationUtil.sanitize(chore.getTaskName()));
        chore.setRoom(SanitizationUtil.sanitize(chore.getRoom()));

        return choreService.addChoreWithAssignment(houseCode, userEmail, chore);
    }


}


