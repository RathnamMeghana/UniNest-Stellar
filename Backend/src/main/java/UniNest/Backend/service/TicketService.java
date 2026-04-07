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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private Timestamp parseDateToTimestamp(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy");
            Date date = sdf.parse(dateStr);
            return Timestamp.of(date);
        } catch (Exception e) {
            return Timestamp.now();
        }
    }

    public String createTicket(Ticket request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null or empty");
        }

        try {
            Firestore db = FirestoreClient.getFirestore();
            Timestamp time = Timestamp.now();

            String realBuildingName = "Unknown Building";
            try {
                if (request.getApartmentId() != null && !request.getApartmentId().isBlank()) {
                    DocumentSnapshot aptDoc = db.collection("apartments")
                            .document(request.getApartmentId())
                            .get()
                            .get();

                    if (aptDoc.exists()) {
                        String buildingId = aptDoc.getString("buildingId");
                        if (buildingId != null && !buildingId.isBlank()) {
                            DocumentSnapshot buildingDoc = db.collection("buildings")
                                    .document(buildingId)
                                    .get()
                                    .get();

                            if (buildingDoc.exists() && buildingDoc.getString("name") != null) {
                                realBuildingName = buildingDoc.getString("name");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                realBuildingName = request.getBuilding() != null && !request.getBuilding().isBlank()
                        ? request.getBuilding()
                        : "Unknown Building";
            }

            if (request.getId() == null || request.getId().isEmpty()) {
                request.setId(UUID.randomUUID().toString());
            }

            Ticket ticket = new Ticket();
            ticket.setId(request.getId());
            ticket.setDescription(request.getDescription());
            ticket.setRoom(request.getRoom());
            ticket.setBuilding(realBuildingName);
            ticket.setApartmentId(request.getApartmentId());
            ticket.setLandlordId(request.getLandlordId());
            ticket.setApartmentName(request.getApartmentName());
            ticket.setCategory(request.getCategory());
            ticket.setPriority(request.getPriority());
            ticket.setDeletedByTenant(false);
            ticket.setStatus(request.getStatus());
            ticket.setUserId(request.getUserId());
            ticket.setUserName(request.getUserName());
            ticket.setPrioritySource(request.getPrioritySource());
            ticket.setImageUrl(request.getImageUrl());
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

            return "Ticket created successfully with id: " + request.getId();

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
            throw new IllegalArgumentException("Building name cannot be null or empty");
        }

        try {
            Firestore db = FirestoreClient.getFirestore();

            ApiFuture<QuerySnapshot> future = db.collection("tickets")
                    .whereEqualTo("building", building)
                    .get();

            return future.get().getDocuments().stream()
                    .map(doc -> doc.toObject(Ticket.class))
                    .collect(Collectors.toList());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TicketServiceException("Operation interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new TicketServiceException("Failed to fetch tickets by building", HttpStatus.INTERNAL_SERVER_ERROR);
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

            return future.get().getDocuments().stream()
                    .map(doc -> doc.toObject(Ticket.class))
                    .filter(ticket -> !ticket.isDeletedByTenant())
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

    public List<Ticket> getTicketsByLandlord(String landlordId, boolean includeDeleted) {
        if (landlordId == null || landlordId.isBlank()) {
            throw new IllegalArgumentException("LandlordId cannot be null or empty");
        }

        try {
            Firestore db = FirestoreClient.getFirestore();

            // Landlord authorization check (Keep this as is)
            DocumentSnapshot landlordDoc = db.collection("users").document(landlordId).get().get();
            if (!landlordDoc.exists() || !"1".equalsIgnoreCase(landlordDoc.getString("role"))) {
                throw new TicketServiceException("Unauthorized as Landlord", HttpStatus.FORBIDDEN);
            }

            // 1. Fetch all tickets for this landlord (Simple query)
            ApiFuture<QuerySnapshot> future = db.collection("tickets")
                    .whereEqualTo("landlordId", landlordId)
                    .get();

            List<Ticket> allTickets = future.get().getDocuments().stream()
                    .map(doc -> doc.toObject(Ticket.class))
                    .collect(Collectors.toList());

            // 2. Logic: If agent wants to see deleted, return all. Otherwise, filter them.
            if (includeDeleted) {
                return allTickets;
            } else {
                return allTickets.stream()
                        .filter(t -> !t.isDeletedByTenant())
                        .collect(Collectors.toList());
            }

        } catch (Exception e) {
            throw new TicketServiceException("Error fetching tickets", HttpStatus.INTERNAL_SERVER_ERROR);
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
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("ticketId required");
        }
        if (priority == null || priority.isBlank()) {
            throw new IllegalArgumentException("priority required");
        }

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

    public String updateAgentData(String ticketId, String response, String arrivalDate) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("ticketId required");
        }

        try {
            Firestore db = FirestoreClient.getFirestore();

            ApiFuture<QuerySnapshot> future = db.collection("tickets")
                    .whereEqualTo("id", ticketId)
                    .get();

            List<QueryDocumentSnapshot> docs = future.get().getDocuments();

            if (docs.isEmpty()) {
                throw new TicketNotFoundException("Ticket not found: " + ticketId);
            }

            QueryDocumentSnapshot ticketDoc = docs.get(0);
            DocumentReference ref = ticketDoc.getReference();
            Ticket ticket = ticketDoc.toObject(Ticket.class);

            ref.update(
                    "agentResponse", response,
                    "arrivalDate", arrivalDate,
                    "updatedAt", Timestamp.now()
            ).get();

            if (ticket != null && arrivalDate != null && !arrivalDate.isBlank()) {
                String houseCode = ticket.getApartmentId() != null ? ticket.getApartmentId().trim() : null;
                Timestamp visitTime = parseDateToTimestamp(arrivalDate);

                DocumentReference calendarRef = db.collection("calendar_events").document();
                String generatedId = calendarRef.getId();

                Map<String, Object> calendarEntry = new HashMap<>();
                calendarEntry.put("id", generatedId);
                calendarEntry.put("houseCode", houseCode);
                calendarEntry.put("type", "MAINTENANCE");
                calendarEntry.put("title", "Reminder: Maintenance Visit");
                calendarEntry.put(
                        "description",
                        "Agent message: " + (response != null ? response : "") +
                                " (Issue: " + (ticket.getCategory() != null ? ticket.getCategory() : "General") + ")"
                );
                calendarEntry.put("startDate", visitTime);
                calendarEntry.put("endDate", visitTime);
                calendarEntry.put("status", "NOT_STARTED");
                calendarEntry.put("allDay", false);
                calendarEntry.put("createdAt", Timestamp.now());
                calendarEntry.put("createdBy", ticket.getLandlordId());

                calendarRef.set(calendarEntry).get();
            }

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

            return arrivalDate != null && !arrivalDate.isBlank()
                    ? "Agent data updated and reminder added to calendar"
                    : "Agent data updated";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TicketServiceException("Error updating agent data", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new TicketServiceException("Error updating agent data", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (FirestoreException e) {
            throw new TicketServiceException("Firestore unavailable", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String softDeleteTicket(String ticketId, String currentUserId) {
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("ticketId required");
        }
        try {
            Firestore db = FirestoreClient.getFirestore();

            // 1. Find the document where the 'id' field matches ticketId
            ApiFuture<QuerySnapshot> future = db.collection("tickets")
                    .whereEqualTo("id", ticketId)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            if (documents.isEmpty()) {
                throw new TicketNotFoundException("Ticket not found: " + ticketId);
            }

            DocumentSnapshot doc = documents.get(0);
            String ticketOwnerId = doc.getString("userId");

            if (ticketOwnerId == null || !ticketOwnerId.equals(currentUserId)) {
                // Throwing a custom exception or a standard Security exception
                throw new TicketServiceException("You are not authorized to delete this ticket", HttpStatus.FORBIDDEN);
            }

            // 3. Update the deletedByTenant flag
            doc.getReference().update(
                    "deletedByTenant", true,
                    "updatedAt", Timestamp.now()
            ).get();

            return "Ticket removed successfully";

        } catch (InterruptedException | ExecutionException e) {
            throw new TicketServiceException("Failed to delete ticket", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public String confirmVisitResolution(String ticketId, String tenantId) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            // Find the ticket
            ApiFuture<QuerySnapshot> future = db.collection("tickets").whereEqualTo("id", ticketId).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            if (documents.isEmpty()) {
                throw new TicketNotFoundException("Ticket not found: " + ticketId);
            }

            QueryDocumentSnapshot ticketDoc = documents.get(0);
            Ticket ticket = ticketDoc.toObject(Ticket.class);

            // Ensure this tenant actually raised the ticket
            if (!ticket.getUserId().equals(tenantId)) {
                throw new TicketServiceException("Unauthorized: Only the tenant who raised the ticket can verify completion.", HttpStatus.FORBIDDEN);
            }

            // Update the status to RESOLVED
            ticketDoc.getReference().update(
                    "status", "RESOLVED",
                    "updatedAt", Timestamp.now()
            ).get();

            // 4. Notify the Letting Agent
            if (ticket.getLandlordId() != null) {
                String title = "Repair Confirmed Done";
                String body = "Tenant " + ticket.getUserName() + " has verified that the " + ticket.getCategory() + " issue is resolved.";

                notificationService.sendToUsers(title, body, List.of(ticket.getLandlordId()), "AGENT_TICKETS", ticket.getId());
            }

            return "Visit confirmed and ticket marked as resolved.";

        } catch (Exception e) {
            throw new TicketServiceException("Verification failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}