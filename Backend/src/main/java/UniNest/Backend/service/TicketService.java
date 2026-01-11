package UniNest.Backend.service;

import com.google.api.core.ApiFuture;
import com.google.api.services.storage.model.BucketAccessControl;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreException;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import UniNest.Backend.exception.ApartmentServiceException;
import UniNest.Backend.exception.TicketServiceException;
import UniNest.Backend.model.Ticket;
import UniNest.Backend.exception.TicketNotFoundException;
import UniNest.Backend.model.User;

@Service
public class TicketService {
    @Autowired
    private UserService userService;

    public String createTicket(Ticket request)  {

        if (request == null) {
            throw new IllegalArgumentException(" Request cannot be null or empty");
        }

        try {
            Firestore db = FirestoreClient.getFirestore();
            Timestamp time = Timestamp.now();

            Ticket ticket = new Ticket();


            if (request.getId() == null || request.getId().isEmpty()) {
                request.setId(UUID.randomUUID().toString());
            }
            ticket.setId(request.getId());
            ticket.setDescription(request.getDescription());
            ticket.setRoom(request.getRoom());
            ticket.setBuilding(request.getBuilding());
            ticket.setApartmentId(request.getApartmentId());
            ticket.setLandlordId(request.getLandlordId());
            ticket.setApartmentName(request.getApartmentName());
            ticket.setCategory(request.getCategory());
            ticket.setPriority(request.getPriority());
            ticket.setStatus(request.getStatus());
            ticket.setUserId(request.getUserId());
            ticket.setCreatedAt(time);

            db.collection("tickets").add(ticket).get();


            return "ticket created successfully with id: " + request.getId();


        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TicketServiceException("Operation interrupted", e);
        } catch (ExecutionException e) {
            throw new TicketServiceException("Firestore operation failed", e);
        } catch (FirestoreException e) {
            throw new TicketServiceException("Firestore unavailable", e);
        }
    }


    public List<Ticket> getTicketsByBuilding(String building) {
        if (building == null || building.isBlank()) {
            throw new IllegalArgumentException("building cannot be null or empty");
        }
        try {
            Firestore db = FirestoreClient.getFirestore();


            // Query documents where the "building" field matches the input
            ApiFuture<QuerySnapshot> future = db.collection("tickets")
                    .whereEqualTo("building", building)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            return documents.stream()
                    .map(doc -> doc.toObject(Ticket.class))
                    .collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TicketServiceException("Operation interrupted", e);
        } catch (ExecutionException e) {
            throw new TicketServiceException("Firestore operation failed", e);
        } catch (FirestoreException e) {
            throw new TicketServiceException("Firestore unavailable", e);
        }
    }

    public List<Ticket> getTicketsByApartment(String houseCode) {
        if (houseCode == null || houseCode.isBlank()) {
            throw new IllegalArgumentException("HouseCode cannot be null or empty");
        }
        try {
            Firestore db = FirestoreClient.getFirestore();

            ApiFuture<QuerySnapshot> future = db.collection("tickets")
                    .whereEqualTo("apartmentId", houseCode)
                    .get();


            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            return documents.stream()
                    .map(doc -> doc.toObject(Ticket.class))
                    .collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TicketServiceException("Operation interrupted", e);
        } catch (ExecutionException e) {
            throw new TicketServiceException("Firestore operation failed", e);
        } catch (FirestoreException e) {
            throw new TicketServiceException("Firestore unavailable", e);
        }
    }

    public String updateTicketStatus(String ticketId, String status) {
        if (ticketId == null || ticketId.isBlank())
            throw new IllegalArgumentException("ticketId required");
        if (status == null || status.isBlank())
            throw new IllegalArgumentException("status required");

        try {
            Firestore db = FirestoreClient.getFirestore();

            ApiFuture<QuerySnapshot> future = db.collection("tickets")
                    .whereEqualTo("id", ticketId)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            if (documents.isEmpty()) {
                throw new TicketNotFoundException("Ticket not found: " + ticketId);

            }

            documents.get(0).getReference().update("status", status).get();

            return "Ticket status updated successfully";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TicketServiceException("Update interrupted", e);
        } catch (ExecutionException e) {
            throw new TicketServiceException("Firestore update failed", e);
        } catch (com.google.cloud.firestore.FirestoreException e) {
            throw new TicketServiceException("Firestore unavailable", e);
        }
    }



    public String updateTicketPriority(String ticketId, String priority) {

        try{

        Firestore db = FirestoreClient.getFirestore();

        ApiFuture<QuerySnapshot> future = db.collection("tickets")
                .whereEqualTo("id", ticketId)
                .get();

        List<QueryDocumentSnapshot> docs = future.get().getDocuments();

        if (docs.isEmpty()) {
            throw new TicketNotFoundException("Ticket not found: " + ticketId);
        }


        docs.get(0).getReference().update("priority", priority);

        return "Ticket priority updated successfully";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TicketServiceException("Operation interrupted", e);
        } catch (ExecutionException e) {
            throw new TicketServiceException("Firestore operation failed", e);
        } catch (FirestoreException e) {
            throw new TicketServiceException("Firestore unavailable", e);
        }
    }

    public List<Ticket> getTicketsByLandlord(String landlordId) throws ExecutionException, InterruptedException {
        if (landlordId == null || landlordId.isBlank()) {
            throw new IllegalArgumentException("LandlordId cannot be null or empty");
        }
        Firestore db = FirestoreClient.getFirestore();
        var landlordDoc = db.collection("users").document(landlordId).get().get();

        if (!landlordDoc.exists()) {
            throw new  TicketServiceException("Landlord with ID " + landlordId + " does not exist.", null);
        }
        String role = landlordDoc.getString("role");
        if (!"1".equalsIgnoreCase(role)) {
            throw new TicketServiceException("User exists but is not authorized as a Landlord.", null);
        }



        try {
            db = FirestoreClient.getFirestore();

            // Fetch all tickets where landlordId matches the logged-in agent
            ApiFuture<QuerySnapshot> future = db.collection("tickets")
                    .whereEqualTo("landlordId", landlordId)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            return documents.stream()
                    .map(doc -> doc.toObject(Ticket.class))
                    .collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TicketServiceException("Operation interrupted", e);
        } catch (ExecutionException e) {
            throw new TicketServiceException("Firestore operation failed", e);
        } catch (FirestoreException e) {
            throw new TicketServiceException("Firestore unavailable", e);
        }
    }

}
