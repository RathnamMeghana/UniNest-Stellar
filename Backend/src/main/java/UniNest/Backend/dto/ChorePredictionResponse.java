package UniNest.Backend.dto;

import lombok.Data;

@Data
public class ChorePredictionResponse {
    private int assignedTo;
    private double confidence;

}
