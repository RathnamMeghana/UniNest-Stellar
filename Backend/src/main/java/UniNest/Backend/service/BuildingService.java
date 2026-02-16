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

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class BuildingService {

    /**
     * Creates a building in Firestore.
     */
    public String createBuilding(BuildingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        request.sanitize();

        try {
            Firestore db = FirestoreClient.getFirestore();
            Timestamp now = Timestamp.now();

            Building building = new Building();
            building.setName(request.getName());
            building.setAddressLine1(request.getAddressLine1());
            building.setCity(request.getCity());
            building.setPostcode(request.getPostcode());
            building.setCountry(request.getCountry());
            building.setLandlordId(request.getLandlordId());
            building.setCreatedAt(now);
            building.setUpdatedAt(now);
            building.setActive(request.getActive() != null ? request.getActive() : true);
            building.setImageUrl(request.getImageUrl());

            db.collection("buildings").add(building).get();

            return "Building created successfully via Admin API";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BuildingServiceException("Process was interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new BuildingServiceException("Firestore rejected the write. Check if image is too large.", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new BuildingServiceException("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<Building> getAllBuildings() {
        try {
            Firestore db = FirestoreClient.getFirestore();
            ApiFuture<QuerySnapshot> future = db.collection("buildings").get();
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
            throw new BuildingServiceException("Process was interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new BuildingServiceException("Failed to fetch buildings from Firestore", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new BuildingServiceException("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<Building> getBuildingsByLandlord(String landlordId) {
        if (landlordId == null || landlordId.isBlank()) {
            throw new IllegalArgumentException("Landlord ID cannot be null or empty");
        }

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

                // Count apartments for this building
                ApiFuture<QuerySnapshot> aptQuery = db.collection("apartments")
                        .whereEqualTo("buildingId", b.getId())
                        .get();
                int count = aptQuery.get().getDocuments().size();
                b.setApartmentCount(count);

                buildings.add(b);
            }
            return buildings;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BuildingServiceException("Process was interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new BuildingServiceException("Failed to fetch buildings for landlord: " + landlordId, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new BuildingServiceException("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
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
                Building b = document.toObject(Building.class);
                b.setId(document.getId());
                return b;
            } else {
                throw new BuildingServiceException("Building does not exist", HttpStatus.NOT_FOUND);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BuildingServiceException("Process was interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new BuildingServiceException("Error fetching building by ID", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
