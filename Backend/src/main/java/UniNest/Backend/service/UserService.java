package UniNest.Backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;

import java.util.concurrent.ExecutionException;
import java.util.ArrayList;
import java.util.List;

import UniNest.Backend.model.User;
import org.springframework.stereotype.Service;


@Service
public class UserService {
    /**
     * Retrieves all users associated with a specific apartment ID.
     * @param apartmentId The ID of the apartment to search within.
     * @return A list of User objects belonging to that apartment.
     */
    public List<User> getUsersForApartment(String houseCode) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            // select all documents from 'users' where apartmentId == given ID
            Query query = db.collection("users")
                    .whereEqualTo("houseCode", houseCode);



            // Execute the query asynchronously
            ApiFuture<QuerySnapshot> future = query.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            // 3. Map the documents to User objects
            List<User> users = new ArrayList<>();
            for (QueryDocumentSnapshot doc : documents) {
                User user = doc.toObject(User.class);
                users.add(user);
            }

            return users;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Query for users interrupted.", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Failed to fetch users from Firestore.", e);
        }
    }
}
