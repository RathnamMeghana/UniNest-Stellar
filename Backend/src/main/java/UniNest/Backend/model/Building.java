package UniNest.Backend.model;

import java.time.LocalDateTime;

import com.google.cloud.Timestamp;

import lombok.Data;

@Data
public class Building {

    private String name;
    private String addressLine1;
    private String city;
    private String postcode;
    private String country;
    private String landlordId;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Boolean active;

}
