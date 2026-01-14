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

import org.springframework.stereotype.Service;
import UniNest.Backend.exception.BuildingServiceException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class BuildingService {

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
            building.setActive(request.getActive());

            db.collection("buildings").add(building).get();

            return "Building created successfully";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BuildingServiceException("Building creation interrupted", e);
        } catch (ExecutionException e) {
            throw new BuildingServiceException("Failed to create building", e);
        } catch (com.google.cloud.firestore.FirestoreException e) {
            throw new BuildingServiceException("Firestore unavailable", e);
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

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BuildingServiceException("Fetching buildings interrupted", e);
        } catch (ExecutionException e) {
            throw new BuildingServiceException("Failed to fetch buildings by landlord", e);
        } catch (com.google.cloud.firestore.FirestoreException e) {
            throw new BuildingServiceException("Firestore unavailable", e);
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

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BuildingServiceException("Fetching buildings interrupted", e);
        } catch (ExecutionException e) {
            throw new BuildingServiceException("Failed to fetch buildings by landlord", e);
        } catch (com.google.cloud.firestore.FirestoreException e) {
            throw new BuildingServiceException("Firestore unavailable", e);
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
            } else {
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BuildingServiceException("Fetching buildings interrupted", e);
        } catch (ExecutionException e) {
            throw new BuildingServiceException("Failed to fetch buildings by landlord", e);
        } catch (com.google.cloud.firestore.FirestoreException e) {
            throw new BuildingServiceException("Firestore unavailable", e);
        }
    }
}
