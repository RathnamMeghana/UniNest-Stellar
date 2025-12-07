package UniNest.Backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;
import UniNest.Backend.model.Room;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class RoomService {

    // -------------------------
    // ADD ROOM TO APARTMENT
    // -------------------------
    public String addRoom(String houseCode, Room room) {
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
        } catch (Exception e) {
            e.printStackTrace();
            return "Error adding room: " + e.getMessage();
        }
    }

    // -------------------------
    // GET ALL ROOMS IN APARTMENT
    // -------------------------
    public List<Room> getRooms(String houseCode) {
        List<Room> list = new ArrayList<>();

        try {
            Firestore db = FirestoreClient.getFirestore();

            CollectionReference roomsRef = db.collection("apartments")
                    .document(houseCode)
                    .collection("rooms");

            ApiFuture<QuerySnapshot> future = roomsRef.get();
            List<QueryDocumentSnapshot> docs = future.get().getDocuments();

            for (DocumentSnapshot doc : docs) {
                Room room = doc.toObject(Room.class);
                room.setId(doc.getId());
                list.add(room);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}
