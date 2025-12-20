package UniNest.Backend.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;

import org.springframework.stereotype.Service;

import java.util.UUID;

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
        ticket.setCategory(request.getCategory());
        ticket.setPriority(request.getPriority());
        ticket.setStatus(request.getStatus());
        ticket.setUserId(request.getUserId());
        ticket.setCreatedAt(time);


        db.collection("tickets").add(ticket);

        return "ticket created successfully with id: " + request.getId();

    }
}
