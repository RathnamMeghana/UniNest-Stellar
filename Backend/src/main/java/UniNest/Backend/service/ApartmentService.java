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
        // Use a prefix to make it more readable
        return "APT-" + uuid.substring(0, 7);
    }

    private String getUniqueCode() throws InterruptedException, ExecutionException {
        try{
        Firestore db = FirestoreClient.getFirestore();
        String uniqueCode;
        boolean exists;

        do {
            uniqueCode = generateAlphanumericCode();

            // check for code is it in the database
            Query query = db.collection("apartments")
                    .whereEqualTo("code", uniqueCode)
                    .limit(1);

            QuerySnapshot snapshot = query.get().get();

            // 3. Check if any apartment were returned
            exists = !snapshot.isEmpty();

        } while (exists);

        return uniqueCode;
    }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApartmentServiceException("Apartment code generation interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new ApartmentServiceException("Failed to generate unique apartment code", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (FirestoreException e) {
            throw new ApartmentServiceException("Firestore unavailable", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String createApartment(ApartmentRequests request) {
        if (request == null) throw new IllegalArgumentException("Request cannot be null");
        try {
            Firestore db = FirestoreClient.getFirestore();
            var landlordDoc = db.collection("users").document(request.getLandlordId()).get().get();


            if (!landlordDoc.exists()) {
                // Change null to HttpStatus.NOT_FOUND
                throw new ApartmentServiceException("Landlord with ID " + request.getLandlordId() + " does not exist.", HttpStatus.NOT_FOUND);
            }
            String role = landlordDoc.getString("role");
            if (!"1".equalsIgnoreCase(role)) {
                // Change null to HttpStatus.FORBIDDEN
                throw new ApartmentServiceException("User exists but is not authorized as a Landlord.", HttpStatus.FORBIDDEN);
            }

            // Assign the guaranteed unique code
            String uniqueCode = getUniqueCode();

            Timestamp time = Timestamp.now();

            Apartment apartment = new Apartment();
            apartment.setName(request.getName());
            apartment.setTotalRooms(request.getTotalRooms());

            // Set the unique code
            apartment.setCode(uniqueCode);

            apartment.setLandlordId(request.getLandlordId());
            apartment.setCreatedAt(time);
            apartment.setBuildingId(request.getBuildingId());
            apartment.setDescription(request.getDescription());
            apartment.setRentPrice(request.getRentPrice());


            apartment.setActive(request.getActive());

            //db.collection("apartments").add(apartment).get();
            db.collection("apartments").document(uniqueCode).set(apartment).get();


            return "Apartment created successfully with code: " + uniqueCode;





        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApartmentServiceException("Apartment creation interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new ApartmentServiceException("Failed to create apartment", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (FirestoreException e) {
            throw new ApartmentServiceException("Firestore unavailable", HttpStatus.INTERNAL_SERVER_ERROR);
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

        }  catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApartmentServiceException("Fetching apartments interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new ApartmentServiceException("Failed to fetch apartments", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (FirestoreException e) {
            throw new ApartmentServiceException("Firestore unavailable", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void removeTenantFromApartment(String email) {

        try {
            Firestore db = FirestoreClient.getFirestore();

            // Step 1 — find the user doc by email (or houseCode)
            ApiFuture<QuerySnapshot> q = db.collection("users")
                    .whereEqualTo("email", email)
                    .get();

            List<QueryDocumentSnapshot> docs = q.get().getDocuments();
            if (docs.isEmpty()) {
                throw new TenantNotFoundException("User not found with email: " + email);
            }


            //Get Firestore document ID
            String userId = docs.get(0).getId();

            //  update fields
            db.collection("users").document(userId).update("apartmentId", null,"houseCode", null).get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApartmentServiceException("Removing tenant interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new ApartmentServiceException("Failed to remove tenant", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (FirestoreException e) {
            throw new ApartmentServiceException("Firestore unavailable", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    public void createApartmentsWithRooms(BulkApartmentWithRoomsRequest request) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            String buildingId = request.getBuildingId();
            String landlordId = request.getLandlordId();

            // 1. VALIDATION: Verify Landlord exists and is authorized (Role "1")
            var landlordDoc = db.collection("users").document(landlordId).get().get();
            if (!landlordDoc.exists()) {
                throw new ApartmentServiceException("Landlord with ID " + landlordId + " does not exist.", HttpStatus.NOT_FOUND);
            }
            String role = landlordDoc.getString("role");
            if (!"1".equalsIgnoreCase(role)) {
                throw new ApartmentServiceException("User exists but is not authorized as a Landlord.", HttpStatus.FORBIDDEN);
            }

            // 2. VALIDATION: Check count
            if (request.getApartmentCount() < 1) {
                throw new ApartmentServiceException("Apartment count must be at least 1", HttpStatus.BAD_REQUEST);
            }

            for (int i = 1; i <= request.getApartmentCount(); i++) {
                String uniqueCode = getUniqueCode();

                Apartment apartment = new Apartment();
                apartment.setName("Apartment " + i);

                // Sum the rooms from roomTemplate
                int totalRooms = request.getRoomTemplate().values().stream().mapToInt(Integer::intValue).sum();
                apartment.setTotalRooms(String.valueOf(totalRooms));

                apartment.setCode(uniqueCode);
                apartment.setLandlordId(landlordId); // Explicitly setting Landlord ID
                apartment.setBuildingId(buildingId);
                apartment.setDescription("Auto-generated apartment");
                apartment.setRentPrice(0.00);
                apartment.setActive(true);
                apartment.setCreatedAt(Timestamp.now());

                // Save apartment document
                db.collection("apartments").document(uniqueCode).set(apartment).get();

                // 3. Create rooms in sub-collection
                for (Map.Entry<String, Integer> entry : request.getRoomTemplate().entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();

                    for (int r = 1; r <= count; r++) {
                        RoomRequests room = new RoomRequests();
                        room.setType(type);
                        room.setLabel(type + " " + r);
                        room.setHouseCode(uniqueCode);


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
            // Re-throw our custom exceptions so GlobalExceptionHandler can catch them
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ApartmentServiceException("Bulk apartment + room creation failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    public List<Apartment> getApartmentsByBuilding(String buildingId) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            ApiFuture<QuerySnapshot> future = db.collection("apartments")
                    .whereEqualTo("buildingId", buildingId)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<Apartment> apartments = new ArrayList<>();

            for (QueryDocumentSnapshot doc : documents) {
                Apartment apt = doc.toObject(Apartment.class);

                ApiFuture<QuerySnapshot> userQuery = db.collection("users")
                        .whereEqualTo("houseCode", apt.getCode())
                        .get();

                int count = userQuery.get().getDocuments().size();
                apt.setOccupiedCount(count);

                apartments.add(apt);
            }
            return apartments;
        } catch (Exception e) {
            throw new ApartmentServiceException("Failed to fetch apartments for building", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}




