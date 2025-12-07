package UniNest.Backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;

import java.util.List;
import java.util.concurrent.ExecutionException;

import UniNest.Backend.model.Room;

@service
public class RoomService {



    public String addRoom(String apartmentId, Room room ){
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("apartments")
                .document(apartmentId)
                .collection("rooms")
                .document();

        room.setId(docRef.getId());
        docRef.set(room);
        return room.getId();

    }
    public List<Room> getRooms(String apartmentId) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> future = db
                .collection("apartments")
                .document(apartmentId)
                .collection("rooms")
                .get();
        return future.get().toObjects(Room.class);
    }



}
