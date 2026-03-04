package UniNest.Backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreException;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import UniNest.Backend.dto.ApartmentRequests;
import UniNest.Backend.dto.BulkApartmentWithRoomsRequest;
import UniNest.Backend.dto.RoomRequests;
import UniNest.Backend.exception.TenantNotFoundException;
import UniNest.Backend.model.Apartment;
import UniNest.Backend.exception.ApartmentServiceException;
import org.springframework.http.HttpStatus;

@Service
public class ApartmentService {

    // Helper method to generate a short alphanumeric code
    private String generateAlphanumericCode() {
        String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return "APT-" + uuid.substring(0, 7);
    }

    private String getUniqueCode() throws InterruptedException, ExecutionException {
        try {
            Firestore db = FirestoreClient.getFirestore();
            String uniqueCode;
            boolean exists;

            do {
                uniqueCode = generateAlphanumericCode();
                Query query = db.collection("apartments")
                        .whereEqualTo("code", uniqueCode)
                        .limit(1);

                QuerySnapshot snapshot = query.get().get();
                exists = !snapshot.isEmpty();
            } while (exists);

            return uniqueCode;
        } catch (Exception e) {
            throw new ApartmentServiceException("Failed to generate unique code", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String createApartment(ApartmentRequests request) {
        if (request == null) throw new IllegalArgumentException("Request cannot be null");
        try {
            Firestore db = FirestoreClient.getFirestore();
            var landlordDoc = db.collection("users").document(request.getLandlordId()).get().get();

            if (!landlordDoc.exists()) {
                throw new ApartmentServiceException("Landlord with ID " + request.getLandlordId() + " does not exist.", HttpStatus.NOT_FOUND);
            }
            String role = landlordDoc.getString("role");
            if (!"1".equalsIgnoreCase(role)) {
                throw new ApartmentServiceException("User exists but is not authorized as a Landlord.", HttpStatus.FORBIDDEN);
            }

            String uniqueCode = getUniqueCode();
            Apartment apartment = new Apartment();
            apartment.setName(request.getName());
            apartment.setTotalRooms(request.getTotalRooms());
            apartment.setCode(uniqueCode);
            apartment.setLandlordId(request.getLandlordId());
            apartment.setCreatedAt(Timestamp.now());
            apartment.setBuildingId(request.getBuildingId());
            apartment.setDescription(request.getDescription());
            apartment.setRentPrice(request.getRentPrice());
            apartment.setActive(request.getActive());
            apartment.setMaxTenants(request.getMaxTenants());

            db.collection("apartments").document(uniqueCode).set(apartment).get();
            return "Apartment created successfully with code: " + uniqueCode;

        } catch (Exception e) {
            throw new ApartmentServiceException("Apartment creation failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void createApartmentsWithRooms(BulkApartmentWithRoomsRequest request) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            // 1. Extract values once to prevent re-calculation/overlap
            String bId = request.getBuildingId();
            String lId = request.getLandlordId();
            int capacityValue = request.getMaxTenants();

            // Calculate room sum
            int roomSumValue = request.getRoomTemplate().values().stream().mapToInt(Integer::intValue).sum();
            String totalRoomsStr = String.valueOf(roomSumValue);

            // 2. Validate Landlord
            var landlordDoc = db.collection("users").document(lId).get().get();
            if (!landlordDoc.exists()) {
                throw new ApartmentServiceException("Landlord not found", HttpStatus.NOT_FOUND);
            }

            // 3. Loop and Create
            for (int i = 1; i <= request.getApartmentCount(); i++) {
                String uniqueCode = getUniqueCode();
                Apartment apartment = new Apartment();

                apartment.setName("Apartment " + i);
                apartment.setCode(uniqueCode);
                apartment.setBuildingId(bId);
                apartment.setLandlordId(lId);

                // Set the capacity (e.g., 2)
                apartment.setMaxTenants(capacityValue);

                // Set the room count (e.g., 5)
                apartment.setTotalRooms(totalRoomsStr);

                apartment.setRentPrice(0.0);
                apartment.setActive(true);
                apartment.setCreatedAt(Timestamp.now());
                apartment.setDescription("Auto-generated apartment");
                apartment.setOccupiedCount(0);

                // Save Apartment
                db.collection("apartments").document(uniqueCode).set(apartment).get();

                // 4. Create Rooms
                for (Map.Entry<String, Integer> entry : request.getRoomTemplate().entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    for (int r = 1; r <= count; r++) {
                        RoomRequests room = new RoomRequests();
                        room.setType(type);
                        room.setLabel(type + " " + r);
                        room.setHouseCode(uniqueCode);

                        // ID for room doc
                        String roomDocId = type.replaceAll("\\s+", "") + r;
                        db.collection("apartments")
                                .document(uniqueCode)
                                .collection("rooms")
                                .document(roomDocId)
                                .set(room)
                                .get();
                    }
                }
            }
        } catch (ApartmentServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ApartmentServiceException("Bulk creation failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<Apartment> getAllApartments() {
        try {
            Firestore db = FirestoreClient.getFirestore();
            ApiFuture<QuerySnapshot> future = db.collection("apartments").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<Apartment> apartments = new ArrayList<>();

            for (QueryDocumentSnapshot doc : documents) {
                Apartment apartment = doc.toObject(Apartment.class);
                apartments.add(apartment);
            }
            return apartments;
        } catch (Exception e) {
            throw new ApartmentServiceException("Fetching apartments failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void removeTenantFromApartment(String email) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            ApiFuture<QuerySnapshot> q = db.collection("users").whereEqualTo("email", email).get();
            List<QueryDocumentSnapshot> docs = q.get().getDocuments();

            if (docs.isEmpty()) {
                throw new TenantNotFoundException("User not found with email: " + email);
            }

            String userId = docs.get(0).getId();
            db.collection("users").document(userId).update("apartmentId", null, "houseCode", null).get();
        } catch (Exception e) {
            throw new ApartmentServiceException("Removing tenant failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<Apartment> getApartmentsByBuilding(String buildingId) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            ApiFuture<QuerySnapshot> future = db.collection("apartments")
                    .whereEqualTo("buildingId", buildingId).get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<Apartment> apartments = new ArrayList<>();

            for (QueryDocumentSnapshot doc : documents) {
                Apartment apt = doc.toObject(Apartment.class);

                // Count occupied status
                ApiFuture<QuerySnapshot> userQuery = db.collection("users")
                        .whereEqualTo("houseCode", apt.getCode()).get();
                apt.setOccupiedCount(userQuery.get().getDocuments().size());

                apartments.add(apt);
            }
            return apartments;
        } catch (Exception e) {
            throw new ApartmentServiceException("Failed to fetch apartments for building", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void deleteApartment(String houseCode) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            // 1. Unassign all tenants from this apartment
            ApiFuture<QuerySnapshot> userQuery = db.collection("users")
                    .whereEqualTo("houseCode", houseCode).get();
            List<QueryDocumentSnapshot> tenants = userQuery.get().getDocuments();

            for (QueryDocumentSnapshot tenant : tenants) {
                db.collection("users").document(tenant.getId())
                        .update("apartmentId", null, "houseCode", null).get();
            }

            // 2. Delete all rooms associated with the apartment
            ApiFuture<QuerySnapshot> roomQuery = db.collection("apartments")
                    .document(houseCode).collection("rooms").get();
            List<QueryDocumentSnapshot> rooms = roomQuery.get().getDocuments();
            for (QueryDocumentSnapshot room : rooms) {
                db.collection("apartments").document(houseCode)
                        .collection("rooms").document(room.getId()).delete().get();
            }

            // 3. Delete the apartment document itself
            db.collection("apartments").document(houseCode).delete().get();

        } catch (Exception e) {
            throw new ApartmentServiceException("Failed to delete apartment", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}