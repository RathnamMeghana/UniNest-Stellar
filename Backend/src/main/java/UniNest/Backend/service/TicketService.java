package UniNest.Backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreException;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import UniNest.Backend.exception.TicketServiceException;
import UniNest.Backend.model.Ticket;
import UniNest.Backend.exception.TicketNotFoundException;

@Service
public class TicketService {

    @Autowired
    private UserService userService;

    @Autowired
    private BuildingService buildingService;

    /**
     * Creates a maintenance ticket and enriches it with the Building Name.
     */
    public String createTicket(Ticket request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null or empty");
        }

        try {
            Firestore db = FirestoreClient.getFirestore();
            Timestamp time = Timestamp.now();

            String realBuildingName = "Unknown Building";
            try {
                var aptDoc = db.collection("apartments").document(request.getApartmentId()).get().get();
                if (aptDoc.exists()) {
                    String bId = aptDoc.getString("buildingId");
                    if (bId != null) {
                        var bDoc = db.collection("buildings").document(bId).get().get();
                        if (bDoc.exists()) {
                            realBuildingName = bDoc.getString("name");
                        }
                    }
                }
            } catch (Exception e) {
                realBuildingName = request.getBuilding() != null ? request.getBuilding() : "Unknown Building";
            }

            Ticket ticket = new Ticket();
            if (request.getId() == null || request.getId().isEmpty()) {
                request.setId(UUID.randomUUID().toString());
            }

            ticket.setId(request.getId());
            ticket.setDescription(request.getDescription());
            ticket.setRoom(request.getRoom());
            ticket.setBuilding(realBuildingName);
            ticket.setApartmentId(request.getApartmentId());
            ticket.setLandlordId(request.getLandlordId());
            ticket.setApartmentName(request.getApartmentName());
            ticket.setCategory(request.getCategory());
            ticket.setPriority(request.getPriority());
            ticket.setStatus(request.getStatus());
            ticket.setUserId(request.getUserId());
            ticket.setUserName(request.getUserName());
            ticket.setPrioritySource(request.getPrioritySource());
            ticket.setCreatedAt(time);
            ticket.setUpdatedAt(time);

            db.collection("tickets").add(ticket).get();
            return "Ticket created successfully with id: " + request.getId();

        } catch (Exception e) {
            throw new TicketServiceException("Firestore operation failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Missing method required by TicketController.
     */
    public List<Ticket> getTicketsByBuilding(String building) {
        if (building == null || building.isBlank()) {
            throw new IllegalArgumentException("building name cannot be null or empty");
        }
        try {
            Firestore db = FirestoreClient.getFirestore();
            ApiFuture<QuerySnapshot> future = db.collection("tickets")
                    .whereEqualTo("building", building)
                    .get();

            return future.get().getDocuments().stream()
                    .map(doc -> doc.toObject(Ticket.class))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new TicketServiceException("Failed to fetch tickets by building", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Updates agent response and arrival date.
     * Automatically creates a reminder in the 'calendar_events' collection.
     */
    public String updateAgentData(String ticketId, String response, String arrivalDate) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            ApiFuture<QuerySnapshot> future = db.collection("tickets").whereEqualTo("id", ticketId).get();
            List<QueryDocumentSnapshot> docs = future.get().getDocuments();

            if (docs.isEmpty()) throw new TicketNotFoundException("Ticket not found: " + ticketId);

            DocumentReference ticketRef = docs.get(0).getReference();
            Ticket ticket = docs.get(0).toObject(Ticket.class);

            ticketRef.update(
                    "agentResponse", response,
                    "arrivalDate", arrivalDate,
                    "updatedAt", Timestamp.now()
            ).get();

            if (ticket != null && arrivalDate != null && !arrivalDate.isEmpty()) {
                String houseCode = ticket.getApartmentId().trim();
                Timestamp visitTime = parseDateToTimestamp(arrivalDate);

                DocumentReference calendarRef = db.collection("calendar_events").document();
                String generatedId = calendarRef.getId();

                Map<String, Object> calendarEntry = new HashMap<>();
                calendarEntry.put("id", generatedId);
                calendarEntry.put("houseCode", houseCode);
                calendarEntry.put("type", "MAINTENANCE");
                calendarEntry.put("title", "Reminder: Maintenance Visit");
                calendarEntry.put("description", "Agent message: " + response + " (Issue: " + ticket.getCategory() + ")");
                calendarEntry.put("startDate", visitTime);
                calendarEntry.put("endDate", visitTime);
                calendarEntry.put("status", "NOT_STARTED");
                calendarEntry.put("allDay", false);
                calendarEntry.put("createdAt", Timestamp.now());
                calendarEntry.put("createdBy", ticket.getLandlordId());

                //calendarRef.set(calendarEntry).get();
                calendarRef.set(calendarEntry).get();

            }

            return "Agent data updated and reminder added to calendar";
        } catch (Exception e) {
            throw new TicketServiceException("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Timestamp parseDateToTimestamp(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy");
            Date date = sdf.parse(dateStr);
            return Timestamp.of(date);
        } catch (Exception e) {
            return Timestamp.now();
        }
    }

    public List<Ticket> getTicketsByLandlord(String landlordId) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> future = db.collection("tickets").whereEqualTo("landlordId", landlordId).get();
        return future.get().getDocuments().stream().map(doc -> doc.toObject(Ticket.class)).collect(Collectors.toList());
    }

    public List<Ticket> getTicketsByApartment(String houseCode) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            ApiFuture<QuerySnapshot> future = db.collection("tickets").whereEqualTo("apartmentId", houseCode).get();
            return future.get().getDocuments().stream().map(doc -> doc.toObject(Ticket.class)).collect(Collectors.toList());
        } catch (Exception e) {
            throw new TicketServiceException("Fetch failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String updateTicketStatus(String ticketId, String status) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            ApiFuture<QuerySnapshot> future = db.collection("tickets").whereEqualTo("id", ticketId).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            if (documents.isEmpty()) throw new TicketNotFoundException("Ticket not found");
            documents.get(0).getReference().update("status", status, "updatedAt", Timestamp.now()).get();
            return "Ticket status updated successfully";
        } catch (Exception e) {
            throw new TicketServiceException("Update failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String updateTicketPriority(String ticketId, String priority) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            ApiFuture<QuerySnapshot> future = db.collection("tickets").whereEqualTo("id", ticketId).get();
            List<QueryDocumentSnapshot> docs = future.get().getDocuments();
            if (docs.isEmpty()) throw new TicketNotFoundException("Ticket not found");
            docs.get(0).getReference().update("priority", priority, "prioritySource", "Manual", "updatedAt", Timestamp.now());
            return "Ticket priority updated successfully";
        } catch (Exception e) {
            throw new TicketServiceException("Update failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}