package UniNest.Backend.model;

import com.google.cloud.Timestamp;
import lombok.Data;

import java.util.List;

@Data
public class Recurrence {

    public enum RecurrenceFrequency {
        DAILY,
        WEEKLY,
        MONTHLY
    }

    private RecurrenceFrequency frequency;
    private int interval;
    private List<String> daysOfWeek;
    private Timestamp endDate;
}
