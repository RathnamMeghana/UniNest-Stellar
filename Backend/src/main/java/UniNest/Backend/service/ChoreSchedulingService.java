package UniNest.Backend.service;

import UniNest.Backend.dto.ChorePredictionRequest;
import UniNest.Backend.dto.ChorePredictionResponse;
import UniNest.Backend.dto.ChoreRequests;
import UniNest.Backend.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

@Slf4j
@Service
public class ChoreSchedulingService {

    @Autowired
    private UserService userService;

    @Autowired
    private ChoreService choreService;
    private static final String modelUrl = "http://127.0.0.1:5002/predict";
    private final RestTemplate restTemplate = new RestTemplate();


    public ChorePredictionResponse predictAssignee(ChorePredictionRequest request, String houseCode) {
        log.info(">>> AI SCHEDULING: Calculating best assignee for task '{}' in house '{}'", request.getTaskName(), houseCode);

        // Fetch roommates and chores
        List<User> roommates = userService.getUsersForApartment(houseCode);
        if (roommates == null || roommates.isEmpty()) {
            log.error("!!! AI FAILURE: No roommates found for house code: {}", houseCode);
            throw new RuntimeException("Cannot assign chore: No roommates found in house " + houseCode);
        }

        List<ChoreRequests> allChores = choreService.getAllChoreByApartment(houseCode);

        // Calculate individual workloads (Total minutes currently assigned)
        Map<String, Integer> workloadMap = new HashMap<>();
        for (User u : roommates) workloadMap.put(u.getId(), 0);

        if (allChores != null) {
            for (ChoreRequests c : allChores) {
                // Only count chores that are not yet completed
                if (c.getAssignedTo() != null && !"COMPLETED".equalsIgnoreCase(c.getStatus())) {
                    workloadMap.put(c.getAssignedTo(),
                            workloadMap.getOrDefault(c.getAssignedTo(), 0) + c.getEstDurationMin());
                }
            }
        }

        // Sort roommates by workload (Lowest minutes at index 0)
        roommates.sort(Comparator.comparingInt(u -> workloadMap.get(u.getId())));

        try {
            //  Prepare AI Payload
            int freestPersonLimit = roommates.get(0).getAvailabilityMins() > 0 ?
                    roommates.get(0).getAvailabilityMins() : 180;
            int currentLoad = workloadMap.get(roommates.get(0).getId());

            Map<String, Object> payload = new HashMap<>();
            payload.put("task_name", request.getTaskName());
            payload.put("room", request.getRoom());
            payload.put("difficulty_score", request.getDifficultyScore() != null ? request.getDifficultyScore() : 3);
            payload.put("est_duration_min", request.getEstDurationMin() != null ? request.getEstDurationMin() : 30);
            payload.put("frequency_per_week", request.getFrequencyPerWeek() != null ? request.getFrequencyPerWeek() : 1.0);
            payload.put("availability_mins", (double) Math.max(10, freestPersonLimit - currentLoad));
            payload.put("roommate_preference", 0.5);

            // Call Python AI Server
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            log.debug(">>> AI REQ: Sending data to model at {}", modelUrl);
            ResponseEntity<Map> response = restTemplate.postForEntity(modelUrl, entity, Map.class);
            Map<String, Object> body = response.getBody();

            if (body == null || !body.containsKey("assigned_to")) {
                throw new Exception("AI response body empty or invalid");
            }

            int assignedToIndex = ((Number) body.get("assigned_to")).intValue();
            double confidence = ((Number) body.get("confidence")).doubleValue();

            log.info("<<< AI RES: Successfully predicted user index {} with {}% confidence",
                    assignedToIndex, (confidence * 100));

            ChorePredictionResponse predictionResponse = new ChorePredictionResponse();
            predictionResponse.setAssignedTo(assignedToIndex);
            predictionResponse.setConfidence(confidence);

            return predictionResponse;

        } catch (Exception e) {
            //If AI server is down, return index 0
            // This prevents the app from crashing if the Python script stops
            log.error("!!! AI SERVER ERROR: {}. Falling back to manual workload balancing.", e.getMessage());

            ChorePredictionResponse fallback = new ChorePredictionResponse();
            fallback.setAssignedTo(0); // The person at index 0 is the freest after sorting
            fallback.setConfidence(0.0); // 0 confidence indicates fallback logic was used
            return fallback;
        }
    }
}