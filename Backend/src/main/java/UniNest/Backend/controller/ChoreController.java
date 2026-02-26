package UniNest.Backend.controller;

import UniNest.Backend.dto.ChorePredictionRequest;
import UniNest.Backend.dto.ChorePredictionResponse;
import UniNest.Backend.dto.ChoreRequests;
import UniNest.Backend.exception.ChoreServiceException;
import UniNest.Backend.exception.UserServiceException;
import UniNest.Backend.model.User;
import UniNest.Backend.service.ChoreSchedulingService;
import UniNest.Backend.service.ChoreService;
import UniNest.Backend.service.UserService;
import UniNest.Backend.util.SanitizationUtil;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chores")
public class ChoreController {

    @Autowired
    private ChoreService choreService;

    @Autowired
    private ChoreSchedulingService choreSchedulingService;


    @Autowired
    private UserService userService;

    /**
     * Endpoint to just get a prediction without saving the chore.
     */
    @PreAuthorize("hasRole('TENANT')")
    @PostMapping("/assign")
    public ChorePredictionResponse assignChore(
            @RequestParam String houseCode,
            @RequestBody ChorePredictionRequest request
    ) {
        houseCode = SanitizationUtil.sanitize(houseCode);
        return choreSchedulingService.predictAssignee(request, houseCode);
    }

    //@PreAuthorize("hasRole('TENANT')")
    @GetMapping("/getAll/{houseCode}")
    public List<ChoreRequests> getAllChoreByApartment(@PathVariable String houseCode) {
        houseCode = SanitizationUtil.sanitize(houseCode);
        return choreService.getAllChoreByApartment(houseCode);
    }

    /**
     * Looks up current house members.
     *  Calls AI to find the best fit.
     *  Saves the chore to Firestore with the AI's choice.
     */
    @PreAuthorize("hasRole('TENANT')")
    @PostMapping("/addWithSmartAssign")
    public ChoreRequests addChoreWithSmartAssign(@RequestParam String houseCode, @RequestBody ChoreRequests chore) throws UserServiceException {

        //  Get Roommates
        List<User> roommates = userService.getUsersForApartment(houseCode);

        //  Apply the same Workload Sort as the Service
        List<ChoreRequests> allChores = choreService.getAllChoreByApartment(houseCode);
        Map<String, Integer> workloadMap = new HashMap<>();
        for (User u : roommates) workloadMap.put(u.getId(), 0);
        if (allChores != null) {
            for (ChoreRequests c : allChores) {
                if (c.getAssignedTo() != null && !"COMPLETED".equals(c.getStatus())) {
                    workloadMap.put(c.getAssignedTo(), workloadMap.getOrDefault(c.getAssignedTo(), 0) + c.getEstDurationMin());
                }
            }
        }
        // Sort: Freest person first
        roommates.sort(Comparator.comparingInt(u -> workloadMap.get(u.getId())));

        // Get AI Prediction
        ChorePredictionRequest predReq = new ChorePredictionRequest();
        predReq.setTaskName(chore.getTaskName());
        predReq.setDifficultyScore(chore.getDifficultyScore());
        predReq.setEstDurationMin(chore.getEstDurationMin());
        predReq.setFrequencyPerWeek((double) chore.getFrequencyPerWeek());

        ChorePredictionResponse assignment = choreSchedulingService.predictAssignee(predReq, houseCode);

        // Pick from the SORTED list
        int index = Math.abs(assignment.getAssignedTo()) % roommates.size();
        User assignedUser = roommates.get(index);

        // Save
        chore.setAssignedTo(assignedUser.getId());
        chore.setStatus("NOT_STARTED");
        return choreService.addChore(houseCode, chore, SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @PreAuthorize("hasRole('TENANT')")
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

    @PreAuthorize("hasRole('TENANT')")
    @PostMapping("/addWithAssignment")
    public ChoreRequests addChoreWithAssignment(
            @RequestParam String houseCode,
            @RequestParam String userEmail,
            @RequestBody ChoreRequests chore
    ) {
        houseCode = SanitizationUtil.sanitize(houseCode);
        userEmail = SanitizationUtil.sanitize(userEmail);
        return choreService.addChoreWithAssignment(houseCode, userEmail, chore);
    }

    @PreAuthorize("hasRole('TENANT')")
    @PatchMapping("/updateStatus")
    public ChoreRequests updateChoreStatus(
            @RequestParam String houseCode,
            @RequestParam String choreId,
            @RequestParam String status,
            @RequestParam(defaultValue = "0") int actualDuration,
            @RequestParam(required = false) String assignedTo
    ) {
        return choreService.updateChoreStatus(houseCode, choreId, status, actualDuration, assignedTo);
    }
}