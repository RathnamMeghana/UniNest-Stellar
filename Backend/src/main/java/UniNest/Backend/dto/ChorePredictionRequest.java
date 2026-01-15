package UniNest.Backend.dto;

import lombok.Data;

@Data
public class ChorePredictionRequest {

    private String taskName;
    private String room;
    private int difficultyScore;
    private int estDurationMin;
    private double frequencyPerWeek;
    private int roommatePreference;
    private int availabilityMins;
}
