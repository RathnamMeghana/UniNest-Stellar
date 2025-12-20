package UniNest.Backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import UniNest.Backend.model.Ticket;
@Service
public class TicketService {

    public String createTicket(Ticket request){
        Firestore db = FirestoreClient.getFirestore();
        Timestamp time = Timestamp.now();

        Ticket ticket = new Ticket();


        if(request.getId() == null || request.getId().isEmpty()){
            request.setId(UUID.randomUUID().toString());
        }
        ticket.setId(request.getId());
        ticket.setDescription(request.getDescription());
        ticket.setRoom(request.getRoom());
        ticket.setBuilding(request.getBuilding());
        ticket.setApartmentId(request.getApartmentId());
        ticket.setCategory(request.getCategory());
        ticket.setPriority(request.getPriority());
        ticket.setStatus(request.getStatus());
        ticket.setUserId(request.getUserId());
        ticket.setCreatedAt(time);


        db.collection("tickets").add(ticket);

        return "ticket created successfully with id: " + request.getId();

    }

    public List<Ticket> getTicketsByBuilding(String building) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        // Query documents where the "building" field matches the input
        ApiFuture<QuerySnapshot> future = db.collection("tickets")
                .whereEqualTo("building", building)
                .get();

        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        return documents.stream()
                .map(doc -> doc.toObject(Ticket.class))
                .collect(Collectors.toList());
    }
}
