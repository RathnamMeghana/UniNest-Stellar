package UniNest.Backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreException;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import UniNest.Backend.exception.ChoreServiceException;
import UniNest.Backend.dto.ChoreRequests;
import UniNest.Backend.model.Chore;

@Service
public class ChoreService {


    public List<ChoreRequests> getAllChoreByApartment(String houseCode) {
        List<ChoreRequests> list = new ArrayList<>();

        if (houseCode == null || houseCode.isBlank()) {
            throw new IllegalArgumentException("houseCode is required");
        }
        try {
            Firestore db = FirestoreClient.getFirestore();



            CollectionReference choresRef = db.collection("apartments")
                    .document(houseCode)
                    .collection("chores");

            ApiFuture<QuerySnapshot> future = choresRef.get();
            List<QueryDocumentSnapshot> docs = future.get().getDocuments();

            for (DocumentSnapshot doc : docs) {
                try {
                    ChoreRequests chore = doc.toObject(ChoreRequests.class);
                    chore.setId(doc.getId());
                    list.add(chore);
                } catch (Exception ignored) {
                    // skip corrupted record
                }
            }

            return list;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ChoreServiceException("Chore query interrupted", e);
        } catch (ExecutionException e) {
            throw new ChoreServiceException("Failed to fetch Chore", e);
        } catch (FirestoreException e) {
            throw new ChoreServiceException("Firestore unavailable", e);
        }
    }

    public ChoreRequests addChore(String houseCode, ChoreRequests chore) {
        if (houseCode == null || houseCode.isBlank()) {
            throw new IllegalArgumentException("houseCode is required");
        }
        try {
            chore.setCreatedAt(Timestamp.now());
            Firestore db = FirestoreClient.getFirestore();
            chore.sanitize();


            // Save chore first (without assignedTo)
            CollectionReference choresRef = db.collection("apartments").document(houseCode).collection("chores");

            ApiFuture<com.google.cloud.firestore.DocumentReference> future = choresRef.add(chore);
            String choreId = future.get().getId();
            chore.setId(choreId);

            return chore;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ChoreServiceException("Chore add interrupted", e);
        } catch (ExecutionException e) {
            throw new ChoreServiceException("Failed to add Chore", e);
        } catch (FirestoreException e) {
            throw new ChoreServiceException("Firestore unavailable", e);
        }
    }

    public ChoreRequests updateAssignmentByTaskNameAndUserEmail( String houseCode,String taskName, String userEmail) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            // 1️ Find user by email + houseCode
            Query userQuery = db.collection("users")
                    .whereEqualTo("email", userEmail)
                    .whereEqualTo("houseCode", houseCode);

            QuerySnapshot userSnapshot = userQuery.get().get();

            if (userSnapshot.isEmpty()) {
                throw new ChoreServiceException("No user found with email " + userEmail, null);
            }

            String userId = userSnapshot.getDocuments().get(0).getId();

            // 2️ Find chore by taskName (CORRECT PATH)
            Query choreQuery = db.collection("apartments")
                    .document(houseCode)
                    .collection("chores")
                    .whereEqualTo("taskName", taskName);

            QuerySnapshot choreSnapshot = choreQuery.get().get();

            if (choreSnapshot.isEmpty()) {
                throw new ChoreServiceException("No chore found with task name " + taskName, null);
            }

            // 3️ Update assignedTo
            DocumentSnapshot choreDoc = choreSnapshot.getDocuments().get(0);

            choreDoc.getReference()
                    .update("assignedTo", userId)
                    .get();

        } catch (Exception e) {
            throw new ChoreServiceException("Failed to update assignment", e);
        }
        return null;
    }

    public ChoreRequests addChoreWithAssignment(String houseCode, String userEmail, ChoreRequests chore) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            //chore.sanitize();

            // 1️ Find user by email + houseCode
            Query userQuery = db.collection("users")
                    .whereEqualTo("email", userEmail)
                    .whereEqualTo("houseCode", houseCode);

            QuerySnapshot userSnapshot = userQuery.get().get();

            if (userSnapshot.isEmpty()) {
                throw new ChoreServiceException(
                        "No user found with email " + userEmail, null
                );
            }

            String userId = userSnapshot.getDocuments().get(0).getId();

            // 2️ Set assignment + metadata
            chore.setAssignedTo(userId);
            chore.setCreatedAt(Timestamp.now());

            // 3️ Save chore under CORRECT path
            CollectionReference choresRef = db.collection("apartments")
                    .document(houseCode)
                    .collection("chores");

            DocumentReference docRef = choresRef.document();
            chore.setId(docRef.getId());

            docRef.set(chore).get();

            return chore;

        } catch (Exception e) {
            throw new ChoreServiceException("Failed to add chore with assignment", e);
        }
    }


}
