package UniNest.Backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;

import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import UniNest.Backend.dto.ApartmentRequests;
import UniNest.Backend.model.Apartment;

@Service
public class ApartmentService {
    // Helper method to generate a short alphanumeric code
    private String generateAlphanumericCode() {
        String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        // Use a prefix to make it more readable
        return "APT-" + uuid.substring(0, 7);
    }

    private String getUniqueCode() throws InterruptedException, ExecutionException {
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

    public String createApartment(ApartmentRequests request) {
        try {
            Firestore db = FirestoreClient.getFirestore();

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

            db.collection("apartments").add(apartment);

            return "Apartment created successfully with code: " + uniqueCode;

        } catch (InterruptedException | ExecutionException e) {
            // Handle the exception if the Firestore call fails
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to generate unique apartment code or save apartment.", e);
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
            throw new RuntimeException("Failed to fetch apartments: " + e.getMessage());
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
                throw new RuntimeException("User not found");
            }

            // Step 2 — Get Firestore document ID
            String userId = docs.get(0).getId();

            // Step 3 — update fields
            db.collection("users").document(userId)
                    .update(
                            "apartmentId", null,
                            "houseCode", null
                    );

        } catch (Exception e) {
            throw new RuntimeException("Failed to remove tenant: " + e.getMessage());
        }
    }
}




