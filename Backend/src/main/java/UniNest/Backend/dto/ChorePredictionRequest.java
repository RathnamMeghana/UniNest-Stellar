package UniNest.Backend.dto;

import lombok.Data;

@Data
public class ChorePredictionRequest {

    private String taskName;
    private String room;
    private Integer difficultyScore;
    private Integer estDurationMin;
    private Double frequencyPerWeek;
    private Integer roommatePreference;
    private Integer availabilityMins;
}
