package UniNest.Backend.service;

import UniNest.Backend.dto.BuildingRequest;
import UniNest.Backend.model.Building;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;

import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BuildingService {

    public String createBuilding(BuildingRequest request) {

        Firestore db = FirestoreClient.getFirestore();

        Timestamp time = Timestamp.now();

        Building building = new Building();
        building.setName(request.getName());
        building.setAddressLine1(request.getAddressLine1());
        building.setCity(request.getCity());
        building.setPostcode(request.getPostcode());
        building.setCountry(request.getCountry());
        building.setLandlordId(request.getLandlordId());
        building.setCreatedAt(time);
        building.setUpdatedAt(time);
        building.setActive(request.getActive());

        db.collection("buildings").add(building);

        return "Building created successfully";

    }

    public List<Building> getAllBuildings() {
        try {
            Firestore db = FirestoreClient.getFirestore();

            ApiFuture<QuerySnapshot> future = db.collection("buildings").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            List<Building> buildings = new ArrayList<>();

            for (QueryDocumentSnapshot doc : documents) {
                Building building = doc.toObject(Building.class);
                building.setId(doc.getId());
                buildings.add(building);
            }

            return buildings;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch buildings: " + e.getMessage());
        }
    }

}
