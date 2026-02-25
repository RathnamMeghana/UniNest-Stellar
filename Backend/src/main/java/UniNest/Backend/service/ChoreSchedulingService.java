package UniNest.Backend.service;

import UniNest.Backend.dto.ChorePredictionRequest;
import UniNest.Backend.dto.ChorePredictionResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

@Service
public class ChoreSchedulingService {
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String MODEL_URL = "http://127.0.0.1:5002/predict";
    public ChorePredictionResponse predictAssignee(ChorePredictionRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Build payload exactly as Flask expects
        Map<String, Object> payload = new HashMap<>();
        // Optional task_name
        if (request.getTaskName() != null && !request.getTaskName().isEmpty()) {
            payload.put("task_name", request.getTaskName());}
        // Required numeric features with defaults if missing
        payload.put("difficulty_score", request.getDifficultyScore() != null ? request.getDifficultyScore() : 3);
        payload.put("est_duration_min", request.getEstDurationMin() != null ? request.getEstDurationMin() : 30);
        payload.put("frequency_per_week", request.getFrequencyPerWeek() != null ? request.getFrequencyPerWeek() : 1);
        payload.put("roommate_preference", request.getRoommatePreference() != null ? request.getRoommatePreference() : 0.5);
        payload.put("availability_mins", request.getAvailabilityMins() != null ? request.getAvailabilityMins() : 120);
        // Always include room
        if (request.getRoom() != null && !request.getRoom().isEmpty()) {
            payload.put("room", request.getRoom());
        } else {
            throw new RuntimeException("Room field is required for prediction");
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        // Make POST request to Flask model
        ResponseEntity<Map> response = restTemplate.postForEntity(MODEL_URL, entity, Map.class);

        Map<String, Object> body = response.getBody();
        if (body == null || !body.containsKey("assigned_to")) {
            throw new RuntimeException("Invalid response from ML model: " + body);
        }

        // Safely parse assigned_to (String or Number)
        Object assignedObj = body.get("assigned_to");
        int assignedTo;
        if (assignedObj instanceof Number) {
            assignedTo = ((Number) assignedObj).intValue();
        } else if (assignedObj instanceof String) {
            assignedTo = Integer.parseInt((String) assignedObj);
        } else {
            throw new RuntimeException("Unexpected type for assigned_to: " + assignedObj);
        }

        // Safely parse confidence
        double confidence = 0.9; // default
        Object confObj = body.get("confidence");
        if (confObj != null) {
            if (confObj instanceof Number) {
                confidence = ((Number) confObj).doubleValue();
            } else if (confObj instanceof String) {
                confidence = Double.parseDouble((String) confObj);
            }
        }

        // Return Spring DTO
        ChorePredictionResponse predictionResponse = new ChorePredictionResponse();
        predictionResponse.setAssignedTo(assignedTo);
        predictionResponse.setConfidence(confidence);
        return predictionResponse;
    }
}
