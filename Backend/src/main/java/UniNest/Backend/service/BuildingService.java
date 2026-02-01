package UniNest.Backend.service;

import UniNest.Backend.dto.BuildingRequest;
import UniNest.Backend.model.Building;
import UniNest.Backend.exception.BuildingServiceException;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class BuildingService {

    /**
     * Creates a building in Firestore.
     * Note: Admin SDK bypasses security rules.
     */
    public String createBuilding(BuildingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        try {
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
            building.setActive(request.getActive() != null ? request.getActive() : true);

            // This is the Base64 String sent from Android
            building.setImageUrl(request.getImageUrl());

            // Save to Firestore. .get() makes the call synchronous so we can catch errors
            db.collection("buildings").add(building).get();

            return "Building created successfully via Admin API";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BuildingServiceException("Process was interrupted", e);
        } catch (ExecutionException e) {
            // This usually catches the 1MB limit error (INVALID_ARGUMENT)
            throw new BuildingServiceException("Firestore rejected the write. Check if image is too large.", e);
        } catch (Exception e) {
            throw new BuildingServiceException("An unexpected error occurred: " + e.getMessage(), e);
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
            throw new BuildingServiceException("Failed to fetch buildings", e);
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
                ApiFuture<QuerySnapshot> aptQuery = db.collection("apartments")
                        .whereEqualTo("buildingId", b.getId())
                        .get();

                int count = aptQuery.get().getDocuments().size();
                b.setApartmentCount(count);

                buildings.add(b);
            }
            return buildings;
        } catch (Exception e) {
            throw new BuildingServiceException("Failed to fetch buildings for landlord: " + landlordId, e);
        }
    }

    public Building getBuildingById(String buildingId) {
        if (buildingId == null || buildingId.isBlank()) {
            throw new IllegalArgumentException("Building id cannot be null or empty");
        }
        try {
            Firestore db = FirestoreClient.getFirestore();
            DocumentSnapshot document = db.collection("buildings").document(buildingId).get().get();

            if (document.exists()) {
                Building building = document.toObject(Building.class);
                building.setId(document.getId());
                return building;
            }
            return null;
        } catch (Exception e) {
            throw new BuildingServiceException("Error fetching building by ID", e);
        }
    }
}