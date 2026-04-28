package UniNest.Backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;
import UniNest.Backend.dto.RoomRequests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import UniNest.Backend.exception.RoomServiceException;

@Service
public class RoomService {

    // -------------------------
    // ADD ROOM TO APARTMENT
    // -------------------------
    public String addRoom(String houseCode, RoomRequests room) {

        if (houseCode == null || houseCode.isBlank())
            throw new IllegalArgumentException("houseCode is required");
        if (room == null)
            throw new IllegalArgumentException("room is required");

        try {
            Firestore db = FirestoreClient.getFirestore();
            room.setHouseCode(houseCode);
            // Generate ID for the room

            DocumentReference newRoomRef = db.collection("apartments")
                    .document(houseCode)
                    .collection("rooms")
                    .document();  // Auto-generated ID

            room.setId(newRoomRef.getId());

            ApiFuture<WriteResult> writeResult = newRoomRef.set(room);
            writeResult.get();

            return "Room added successfully!";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RoomServiceException("Room creation interrupted", e);
        } catch (ExecutionException e) {
            throw new RoomServiceException("Failed to save room", e);
        } catch (FirestoreException e) {
            throw new RoomServiceException("Firestore unavailable", e);
        }
    }

    // -------------------------
    // GET ALL ROOMS in an APARTMENT
    // -------------------------
    public List<RoomRequests> getRooms(String houseCode) {
        List<RoomRequests> list = new ArrayList<>();

        if (houseCode == null || houseCode.isBlank()) {
            throw new IllegalArgumentException("houseCode is required");
        }
        try {
            Firestore db = FirestoreClient.getFirestore();

            CollectionReference roomsRef = db.collection("apartments")
                    .document(houseCode)
                    .collection("rooms");

            ApiFuture<QuerySnapshot> future = roomsRef.get();
            List<QueryDocumentSnapshot> docs = future.get().getDocuments();

            for (DocumentSnapshot doc : docs) {
                try {
                    RoomRequests room = doc.toObject(RoomRequests.class);
                    room.setId(doc.getId());
                    list.add(room);
                } catch (Exception ignored) {
                    // skip corrupted record
                }
            }
            return list;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RoomServiceException("Room query interrupted", e);
        } catch (ExecutionException e) {
            throw new RoomServiceException("Failed to fetch rooms", e);
        } catch (FirestoreException e) {
            throw new RoomServiceException("Firestore unavailable", e);
        }
    }
}


