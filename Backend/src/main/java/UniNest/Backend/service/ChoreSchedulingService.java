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

        // Wrap your features into input_data array
        Map<String, Object> payload = new HashMap<>();
        payload.put("input_data", Collections.singletonList(Arrays.asList(
                (double) request.getDifficultyScore(),
                (double) request.getEstDurationMin(),
                request.getFrequencyPerWeek(),
                (double) request.getRoommatePreference(),
                (double) request.getAvailabilityMins()
        )));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(MODEL_URL, entity, Map.class);

        // Extract prediction from response
        Map<String, Object> body = response.getBody();
        List predictions = (List) body.get("predictions");
        double pred = (double) predictions.get(0);

        // Return to your Spring DTO
        ChorePredictionResponse predictionResponse = new ChorePredictionResponse();
        predictionResponse.setAssignedTo((int) pred);  // dummy assignment
        predictionResponse.setConfidence(0.9);         // dummy confidence
        return predictionResponse;
    }
}
