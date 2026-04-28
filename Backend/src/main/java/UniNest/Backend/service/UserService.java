package UniNest.Backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.cloud.FirestoreClient;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.ArrayList;
import java.util.List;

import UniNest.Backend.exception.UserServiceException;
import UniNest.Backend.model.User;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;



@Service
public class UserService {
    /**
     * Retrieves all users associated with a specific apartment ID.
     *
     * @param apartmentId The ID of the apartment to search within.
     * @return A list of User objects belonging to that apartment.
     */
    public List<User> getUsersForApartment(String houseCode) throws UserServiceException {
        if (houseCode == null || houseCode.isBlank()) {
            throw new IllegalArgumentException("houseCode cannot be null or empty");
        }
        try {
            Firestore db = FirestoreClient.getFirestore();

            // select all documents from 'users' where apartmentId == given ID
            Query query = db.collection("users")
                    .whereEqualTo("houseCode", houseCode);


            // Execute the query asynchronously
            ApiFuture<QuerySnapshot> future = query.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            //  Map the documents to User objects
            List<User> users = new ArrayList<>();
            for (QueryDocumentSnapshot doc : documents) {
                User user = doc.toObject(User.class);
                user.setId(doc.getId());
                users.add(user);
            }

            return users;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UserServiceException("User query interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new UserServiceException("Firestore query failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public User getUserByEmailInApartment(String houseCode, String email) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            QuerySnapshot snapshot = db.collection("apartments")
                    .document(houseCode)
                    .collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .get();

            if (snapshot.isEmpty()) {
                throw new RuntimeException("User not found with email: " + email);
            }

            DocumentSnapshot doc = snapshot.getDocuments().get(0);
            User user = doc.toObject(User.class);
            user.setId(doc.getId());

            return user;

        } catch (Exception e) {
            throw new RuntimeException("Failed to find user by email", e);
        }
    }

    public void setFirebaseRole(String uid, String firestoreRole) throws FirebaseAuthException {
        String role;
        switch (firestoreRole) {
            case "1":
                role = "LETTINGAGENT";
                break;
            case "2":
                role = "TENANT";
                break;
            default:
                role = "USER";
        }

        FirebaseAuth.getInstance().setCustomUserClaims(uid, Map.of("role", role));
    }

}
