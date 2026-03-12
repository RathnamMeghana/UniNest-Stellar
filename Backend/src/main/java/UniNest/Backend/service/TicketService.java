package UniNest.Backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreException;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import UniNest.Backend.exception.TicketNotFoundException;
import UniNest.Backend.exception.TicketServiceException;
import UniNest.Backend.model.Ticket;

@Service
public class TicketService {

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    private Long parseIsoToMillis(String isoString) {
        try {
            return java.time.Instant.parse(isoString).toEpochMilli();
        } catch (Exception e) {
            return null;
        }
    }

    public String createTicket(Ticket request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null or empty");
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
            ticket.setUserName(request.getUserName());
            ticket.setPrioritySource(request.getPrioritySource());
            ticket.setCreatedAt(time);
            ticket.setUpdatedAt(time);

            db.collection("tickets").add(ticket).get();

            if (ticket.getApartmentId() != null && !ticket.getApartmentId().isBlank()) {
                List<String> agentIds = notificationService.getAgentUserIdsByHouseCode(ticket.getApartmentId());

                if (!agentIds.isEmpty()) {
                    String title = "New maintenance request";
                    String body = (ticket.getRoom() != null && !ticket.getRoom().isBlank()
                            ? ticket.getRoom() + ": "
                            : "")
                            + (ticket.getCategory() != null && !ticket.getCategory().isBlank()
                            ? ticket.getCategory()
                            : "Issue reported");

                    notificationService.createAgentMaintenanceNotification(
                            agentIds,
                            title,
                            body,
                            ticket.getId(),
                            System.currentTimeMillis()
                    );

                    notificationService.sendToUsers(
                            title,
                            body,
                            agentIds,
                            "AGENT_TICKETS",
                            ticket.getId()
                    );
                }
            }

            return "ticket created successfully with id: " + request.getId();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TicketServiceException("Operation interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new TicketServiceException("Firestore operation failed", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (FirestoreException e) {
            throw new TicketServiceException("Firestore unavailable", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<Ticket> getTicketsByBuilding(String building) {
        if (building == null || building.isBlank()) {
            throw new IllegalArgumentException("building cannot be null or empty");
        }

        try {
            Firestore db = FirestoreClient.getFirestore();

            ApiFuture<QuerySnapshot> future = db.collection("tickets")
                    .whereEqualTo("building", building)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            return documents.stream()
                    .map(doc -> doc.toObject(Ticket.class))
                    .collect(Collectors.toList());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TicketServiceException("Operation interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new TicketServiceException("Firestore operation failed", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (FirestoreException e) {
            throw new TicketServiceException("Firestore unavailable", HttpStatus.INTERNAL_SERVER_ERROR);
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
            throw new TicketServiceException("Operation interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new TicketServiceException("Firestore operation failed", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (FirestoreException e) {
            throw new TicketServiceException("Firestore unavailable", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String updateTicketStatus(String ticketId, String status) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("ticketId required");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status required");
        }

        try {
            Firestore db = FirestoreClient.getFirestore();

            ApiFuture<QuerySnapshot> future = db.collection("tickets")
                    .whereEqualTo("id", ticketId)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            if (documents.isEmpty()) {
                throw new TicketNotFoundException("Ticket not found: " + ticketId);
            }

            QueryDocumentSnapshot ticketDoc = documents.get(0);
            Ticket ticket = ticketDoc.toObject(Ticket.class);

            ticketDoc.getReference().update(
                    "status", status,
                    "updatedAt", Timestamp.now()
            ).get();

            String normalizedStatus = status.trim().toUpperCase();

            if (ticket != null && ticket.getApartmentId() != null && !ticket.getApartmentId().isBlank()) {
                List<String> tenantIds = notificationService.getTenantUserIdsByHouseCode(ticket.getApartmentId());

                if (!tenantIds.isEmpty()) {
                    if ("IN_PROGRESS".equals(normalizedStatus) || "IN_PROCESS".equals(normalizedStatus)) {
                        String title = "Ticket update";
                        String body = "Your maintenance ticket is now being reviewed";

                        notificationService.createMaintenanceNotification(
                                tenantIds,
                                title,
                                body,
                                ticket.getId(),
                                System.currentTimeMillis()
                        );

                        notificationService.sendToUsers(
                                title,
                                body,
                                tenantIds,
                                "TICKETS",
                                ticket.getId()
                        );
                    } else if ("RESOLVED".equals(normalizedStatus) || "CLOSED".equals(normalizedStatus)) {
                        String title = "Ticket resolved";
                        String body = (ticket.getCategory() != null && !ticket.getCategory().isBlank())
                                ? ticket.getCategory() + " issue has been resolved"
                                : "Your maintenance ticket has been resolved";

                        notificationService.createMaintenanceNotification(
                                tenantIds,
                                title,
                                body,
                                ticket.getId(),
                                System.currentTimeMillis()
                        );

                        notificationService.sendToUsers(
                                title,
                                body,
                                tenantIds,
                                "TICKETS",
                                ticket.getId()
                        );
                    }
                }
            }

            return "Ticket status updated successfully";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TicketServiceException("Update interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new TicketServiceException("Firestore update failed", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (FirestoreException e) {
            throw new TicketServiceException("Firestore unavailable", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String updateTicketPriority(String ticketId, String priority) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            ApiFuture<QuerySnapshot> future = db.collection("tickets")
                    .whereEqualTo("id", ticketId)
                    .get();

            List<QueryDocumentSnapshot> docs = future.get().getDocuments();

            if (docs.isEmpty()) {
                throw new TicketNotFoundException("Ticket not found: " + ticketId);
            }

            docs.get(0).getReference().update(
                    "priority", priority,
                    "prioritySource", "Manual",
                    "updatedAt", Timestamp.now()
            ).get();

            return "Ticket priority updated successfully";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TicketServiceException("Operation interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new TicketServiceException("Firestore operation failed", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (FirestoreException e) {
            throw new TicketServiceException("Firestore unavailable", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<Ticket> getTicketsByLandlord(String landlordId) throws ExecutionException, InterruptedException {
        if (landlordId == null || landlordId.isBlank()) {
            throw new IllegalArgumentException("LandlordId cannot be null or empty");
        }

        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot landlordDoc = db.collection("users").document(landlordId).get().get();

        if (!landlordDoc.exists()) {
            throw new TicketServiceException("Landlord with ID " + landlordId + " does not exist.", HttpStatus.NOT_FOUND);
        }

        String role = landlordDoc.getString("role");
        if (!"1".equalsIgnoreCase(role)) {
            throw new TicketServiceException("User exists but is not authorized as a Landlord.", HttpStatus.FORBIDDEN);
        }

        try {
            ApiFuture<QuerySnapshot> future = db.collection("tickets")
                    .whereEqualTo("landlordId", landlordId)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            return documents.stream()
                    .map(doc -> doc.toObject(Ticket.class))
                    .collect(Collectors.toList());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TicketServiceException("Operation interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new TicketServiceException("Firestore operation failed", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (FirestoreException e) {
            throw new TicketServiceException("Firestore unavailable", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String updateAgentData(String ticketId, String response, String arrivalDate) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            ApiFuture<QuerySnapshot> future = db.collection("tickets")
                    .whereEqualTo("id", ticketId)
                    .get();

            List<QueryDocumentSnapshot> docs = future.get().getDocuments();

            if (docs.isEmpty()) {
                throw new TicketNotFoundException("Ticket not found");
            }

            QueryDocumentSnapshot ticketDoc = docs.get(0);
            DocumentReference ref = ticketDoc.getReference();
            Ticket ticket = ticketDoc.toObject(Ticket.class);

            ref.update(
                    "agentResponse", response,
                    "arrivalDate", arrivalDate,
                    "updatedAt", Timestamp.now()
            ).get();

            if (ticket != null && ticket.getApartmentId() != null && !ticket.getApartmentId().isBlank()) {
                List<String> tenantIds = notificationService.getTenantUserIdsByHouseCode(ticket.getApartmentId());

                if (!tenantIds.isEmpty()) {
                    String title;
                    String body;
                    Long eventTime = System.currentTimeMillis();

                    if (arrivalDate != null && !arrivalDate.isBlank()) {
                        title = (ticket.getCategory() != null && !ticket.getCategory().isBlank())
                                ? ticket.getCategory() + " visit scheduled"
                                : "Maintenance visit scheduled";
                        body = "Scheduled for " + arrivalDate;

                        Long parsedTime = parseIsoToMillis(arrivalDate);
                        if (parsedTime != null) {
                            eventTime = parsedTime;
                        }
                    } else if (response != null && !response.isBlank()) {
                        title = "Ticket update";
                        body = "The letting agent responded to your maintenance ticket";
                    } else {
                        title = "Ticket update";
                        body = "Your maintenance ticket has been updated";
                    }

                    notificationService.createMaintenanceNotification(
                            tenantIds,
                            title,
                            body,
                            ticket.getId(),
                            eventTime
                    );

                    notificationService.sendToUsers(
                            title,
                            body,
                            tenantIds,
                            "TICKETS",
                            ticket.getId()
                    );
                }
            }

            return "Agent data updated";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TicketServiceException("Error updating agent data", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new TicketServiceException("Error updating agent data", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (FirestoreException e) {
            throw new TicketServiceException("Firestore unavailable", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}