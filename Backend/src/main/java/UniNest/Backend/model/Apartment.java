package UniNest.Backend.model;

import com.google.cloud.firestore.annotation.PropertyName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.AccessLevel;
import java.util.List;

@Data
@NoArgsConstructor
public class Apartment {

    private String buildingId;
    private String name;
    private String code;
    private String landlordId;
    private com.google.cloud.Timestamp createdAt;
    private String description;
    private Double rentPrice;
    private Boolean active;
    private List<Room> rooms;
    private int occupiedCount;

    // 1. Tell Lombok NOT to generate default getters for these two
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String totalRooms;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private int maxTenants;

    // 2. Manually define Getters/Setters with the DB field names
    @PropertyName("total_rooms_count")
    public String getTotalRooms() { return totalRooms; }

    @PropertyName("total_rooms_count")
    public void setTotalRooms(String totalRooms) { this.totalRooms = totalRooms; }

    @PropertyName("max_tenants_limit")
    public int getMaxTenants() { return maxTenants; }

    @PropertyName("max_tenants_limit")
    public void setMaxTenants(int maxTenants) { this.maxTenants = maxTenants; }
}