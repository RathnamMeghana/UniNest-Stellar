package UniNest.Backend.service;

import UniNest.Backend.dto.ChorePredictionRequest;
import UniNest.Backend.dto.ChorePredictionResponse;
import UniNest.Backend.dto.ChoreRequests;
import UniNest.Backend.model.User;
import UniNest.Backend.exception.UserServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

@Service
public class ChoreSchedulingService {

    @Autowired
    private UserService userService;

    @Autowired
    private ChoreService choreService;

    private final RestTemplate restTemplate = new RestTemplate();

    // URL for your Flask AI server
    private static final String MODEL_URL = "http://127.0.0.1:5002/predict";

    public ChorePredictionResponse predictAssignee(ChorePredictionRequest request, String houseCode) {
        try {
            // 1. Fetch roommates
            List<User> roommates = userService.getUsersForApartment(houseCode);

            // 2. Fetch chores to calculate individual workloads
            List<ChoreRequests> allChores = choreService.getAllChoreByApartment(houseCode);

            // Map: UserID -> Total Minutes Assigned
            Map<String, Integer> workloadMap = new HashMap<>();
            for (User u : roommates) workloadMap.put(u.getId(), 0); // Initialize all at 0

            if (allChores != null) {
                for (ChoreRequests c : allChores) {
                    if (c.getAssignedTo() != null && !"COMPLETED".equals(c.getStatus())) {
                        workloadMap.put(c.getAssignedTo(),
                                workloadMap.getOrDefault(c.getAssignedTo(), 0) + c.getEstDurationMin());
                    }
                }
            }

            // --- THE GENIUS HACK: SORT ROOMMATES BY WORKLOAD ---
            // Roommate with 0 minutes comes first (Index 0)
            // Roommate with 100 minutes comes last
            roommates.sort(Comparator.comparingInt(u -> workloadMap.get(u.getId())));

            // 3. Prepare AI Payload (using the freest person's availability)
            int freestPersonMins = roommates.get(0).getAvailabilityMins() > 0 ?
                    roommates.get(0).getAvailabilityMins() : 180;
            int currentLoad = workloadMap.get(roommates.get(0).getId());

            Map<String, Object> payload = new HashMap<>();
            payload.put("task_name", request.getTaskName());
            payload.put("room", request.getRoom());
            payload.put("difficulty_score", request.getDifficultyScore() != null ? request.getDifficultyScore() : 3);
            payload.put("est_duration_min", request.getEstDurationMin() != null ? request.getEstDurationMin() : 30);
            payload.put("frequency_per_week", request.getFrequencyPerWeek() != null ? request.getFrequencyPerWeek() : 1.0);

            // Tell the AI how much time the "Freest" person has left
            payload.put("availability_mins", (double) Math.max(10, freestPersonMins - currentLoad));
            payload.put("roommate_preference", 0.5);

            // 4. Call Flask AI
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(MODEL_URL, entity, Map.class);
            Map<String, Object> body = response.getBody();

            // 5. Return the index
            int assignedToIndex = 0;
            Object assignedObj = body.get("assigned_to");
            if (assignedObj instanceof Number) {
                assignedToIndex = ((Number) assignedObj).intValue();
            }

            ChorePredictionResponse predictionResponse = new ChorePredictionResponse();
            predictionResponse.setAssignedTo(assignedToIndex);
            predictionResponse.setConfidence(((Number) body.get("confidence")).doubleValue());

            return predictionResponse;

        } catch (Exception e) {
            throw new RuntimeException("Scheduling failed: " + e.getMessage());
        }
    }
}