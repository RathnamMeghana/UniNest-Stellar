package UniNest.Backend.service;

import java.util.ArrayList;
import java.util.List;
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
import UniNest.Backend.exception.TenantNotFoundException;
import UniNest.Backend.model.Apartment;
import UniNest.Backend.exception.ApartmentServiceException;

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
            throw new ApartmentServiceException("Apartment code generation interrupted", e);
        } catch (ExecutionException e) {
            throw new ApartmentServiceException("Failed to generate unique apartment code", e);
        } catch (FirestoreException e) {
            throw new ApartmentServiceException("Firestore unavailable", e);
        }
    }

    public String createApartment(ApartmentRequests request) {
        if (request == null) throw new IllegalArgumentException("Request cannot be null");
        try {
            Firestore db = FirestoreClient.getFirestore();
            var landlordDoc = db.collection("users").document(request.getLandlordId()).get().get();

            if (!landlordDoc.exists()) {
                throw new ApartmentServiceException("Landlord with ID " + request.getLandlordId() + " does not exist.", null);
            }
            String role = landlordDoc.getString("role");
            if (!"1".equalsIgnoreCase(role)) {
                throw new ApartmentServiceException("User exists but is not authorized as a Landlord.", null);
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
            throw new ApartmentServiceException("Apartment creation interrupted", e);
        } catch (ExecutionException e) {
            throw new ApartmentServiceException("Failed to create apartment", e);
        } catch (FirestoreException e) {
            throw new ApartmentServiceException("Firestore unavailable", e);
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
            throw new ApartmentServiceException("Fetching apartments interrupted", e);
        } catch (ExecutionException e) {
            throw new ApartmentServiceException("Failed to fetch apartments", e);
        } catch (FirestoreException e) {
            throw new ApartmentServiceException("Firestore unavailable", e);
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
            throw new ApartmentServiceException("Removing tenant interrupted", e);
        } catch (ExecutionException e) {
            throw new ApartmentServiceException("Failed to remove tenant", e);
        } catch (FirestoreException e) {
            throw new ApartmentServiceException("Firestore unavailable", e);
        }
    }

    public void createApartments(List<ApartmentRequests> requests) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            for (ApartmentRequests request : requests) {
                String uniqueCode = getUniqueCode();

                Apartment apartment = new Apartment();
                apartment.setName(request.getName());
                apartment.setTotalRooms(request.getTotalRooms());
                apartment.setCode(uniqueCode);
                apartment.setLandlordId(request.getLandlordId());
                apartment.setBuildingId(request.getBuildingId());
                apartment.setDescription(request.getDescription());
                apartment.setRentPrice(request.getRentPrice());
                apartment.setActive(request.getActive());
                apartment.setCreatedAt(Timestamp.now());


                db.collection("apartments").document(uniqueCode).set(apartment).get();

            }

        } catch (Exception e) {
            throw new ApartmentServiceException("Bulk apartment creation failed", e);
        }
    }

}




