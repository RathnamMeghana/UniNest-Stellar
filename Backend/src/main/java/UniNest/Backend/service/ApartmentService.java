package UniNest.Backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;

import UniNest.Backend.dto.ApartmentRequests;
import UniNest.Backend.model.Apartment;
import UniNest.Backend.model.Building;

@Service
public class ApartmentService {
    public String ApartmentService(ApartmentRequests request) {
        Firestore db = FirestoreClient.getFirestore();

        Timestamp time = Timestamp.now();

        Apartment apartment = new Apartment();
        apartment.setBuildingId(request.getBuildingId());
        apartment.setName(request.getName());
        apartment.setCode(request.getCode());
        apartment.setLandlordId(request.getLandlordId());
        apartment.setCreatedAt(time);
        apartment.setActive(request.getActive());

        db.collection("apartment").add(apartment);

        return "Apartment created successfully";
    };

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

}
