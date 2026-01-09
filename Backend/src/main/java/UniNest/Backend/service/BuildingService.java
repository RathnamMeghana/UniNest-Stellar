package UniNest.Backend.service;

import UniNest.Backend.dto.BuildingRequest;
import UniNest.Backend.model.Building;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
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
try{
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

        db.collection("buildings").add(building).get();

        return "Building created successfully";

    }
catch (Exception e) {
    throw new RuntimeException("Failed to create building: " + e.getMessage());
}
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

    public List<Building> getBuildingsByLandlord(String landlordId) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            ApiFuture<QuerySnapshot> future = db.collection("buildings")
                    .whereEqualTo("landlordId", landlordId)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            List<Building> buildings = new ArrayList<>();
            for (QueryDocumentSnapshot doc : documents) {
                Building b = doc.toObject(Building.class);
                b.setId(doc.getId());
                buildings.add(b);
            }
            return buildings;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch buildings by landlordId: " + e.getMessage());
        }
    }

    public Building getBuildingById(String buildingId) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            // Fetch specific document by ID
            DocumentSnapshot document = db.collection("buildings").document(buildingId).get().get();

            if (document.exists()) {
                Building building = document.toObject(Building.class);
                building.setId(document.getId()); // Ensure ID is set
                return building;
            } else {
                return null; // Handle not found in controller
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch building: " + e.getMessage());
        }
    }
}
