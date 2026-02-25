package UniNest.Backend.service;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

import UniNest.Backend.dto.CalendarEventDTO;
import UniNest.Backend.dto.ChoreRequests;
import UniNest.Backend.exception.ChoreServiceException;

@Service
public class ChoreService {

    private final CalendarService calendarService;
    private final Firestore firestore;

    public ChoreService(CalendarService calendarService, Firestore firestore) {
        this.calendarService = calendarService;
        this.firestore = firestore;
    }

    /* =========================
       GET ALL CHORES
       ========================= */
    public List<ChoreRequests> getAllChoreByApartment(String houseCode) {
        if (houseCode == null || houseCode.isBlank()) {
            throw new IllegalArgumentException("houseCode is required");
        }

        List<ChoreRequests> list = new ArrayList<>();

        try {
            CollectionReference choresRef = firestore.collection("apartments")
                    .document(houseCode)
                    .collection("chores");

            List<QueryDocumentSnapshot> docs = choresRef.get().get().getDocuments();

            for (QueryDocumentSnapshot doc : docs) {
                try {
                    ChoreRequests chore = doc.toObject(ChoreRequests.class);
                    chore.setId(doc.getId());

                    if (chore.getAssignedTo() != null && !chore.getAssignedTo().isBlank()) {
                        DocumentSnapshot userDoc = firestore.collection("users")
                                .document(chore.getAssignedTo())
                                .get()
                                .get();

                        if (userDoc.exists()) {
                            chore.setAssignedUserEmail(userDoc.getString("email"));
                        }
                    }

                    list.add(chore);

                } catch (Exception e) {
                    System.err.println("Skipping corrupted chore: " + e.getMessage());
                }
            }

            return list;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ChoreServiceException("Chore query interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new ChoreServiceException("Failed to fetch chores", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /* =========================
       ADD CHORE
       ========================= */
    public ChoreRequests addChore(String houseCode, ChoreRequests chore, String userId) {
        if (houseCode == null || houseCode.isBlank()) {
            throw new IllegalArgumentException("houseCode is required");
        }

        try {
            chore.sanitize();
            chore.setHouseCode(houseCode);
            chore.setCreatedBy(userId);
            chore.setCreatedAt(Timestamp.now());

            if (chore.getScheduledDate() == null || chore.getScheduledDate().isBlank()) {
                chore.setScheduledDate(Instant.now().toString());
            }

            CollectionReference choresRef = firestore.collection("apartments")
                    .document(houseCode)
                    .collection("chores");

            DocumentReference docRef = choresRef.document();
            chore.setId(docRef.getId());
            docRef.set(chore).get();

            // Create Calendar Event (single clean call)
            CalendarEventDTO.Create event = buildCalendarEvent(chore, houseCode);
            calendarService.create(event, userId);

            return chore;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ChoreServiceException("Chore creation interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            throw new ChoreServiceException("Failed to add chore", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /* =========================
       UPDATE ASSIGNMENT
       ========================= */
    public ChoreRequests updateAssignmentByTaskNameAndUserEmail(
            String houseCode, String taskName, String userEmail
    ) {
        try {
            Query userQuery = firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .whereEqualTo("houseCode", houseCode);

            QuerySnapshot userSnapshot = userQuery.get().get();
            if (userSnapshot.isEmpty()) {
                throw new ChoreServiceException("User not found", HttpStatus.NOT_FOUND);
            }

            String userId = userSnapshot.getDocuments().get(0).getId();

            Query choreQuery = firestore.collection("apartments")
                    .document(houseCode)
                    .collection("chores")
                    .whereEqualTo("taskName", taskName);

            QuerySnapshot choreSnapshot = choreQuery.get().get();
            if (choreSnapshot.isEmpty()) {
                throw new ChoreServiceException("Chore not found", HttpStatus.NOT_FOUND);
            }

            DocumentSnapshot choreDoc = choreSnapshot.getDocuments().get(0);
            choreDoc.getReference().update("assignedTo", userId).get();

            return choreDoc.toObject(ChoreRequests.class);

        } catch (Exception e) {
            throw new ChoreServiceException("Failed to update assignment", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /* =========================
       ADD CHORE WITH ASSIGNMENT
       ========================= */
    public ChoreRequests addChoreWithAssignment(
            String houseCode, String userEmail, ChoreRequests chore
    ) {
        try {
            chore.sanitize();

            Query userQuery = firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .whereEqualTo("houseCode", houseCode);

            QuerySnapshot userSnapshot = userQuery.get().get();
            if (userSnapshot.isEmpty()) {
                throw new ChoreServiceException("User not found", HttpStatus.NOT_FOUND);
            }

            String assigneeId = userSnapshot.getDocuments().get(0).getId();

            chore.setAssignedTo(assigneeId);
            chore.setHouseCode(houseCode);
            chore.setCreatedAt(Timestamp.now());

            if (chore.getScheduledDate() == null || chore.getScheduledDate().isBlank()) {
                chore.setScheduledDate(Instant.now().toString());
            }

            CollectionReference choresRef = firestore.collection("apartments")
                    .document(houseCode)
                    .collection("chores");

            DocumentReference docRef = choresRef.document();
            chore.setId(docRef.getId());
            docRef.set(chore).get();

            // Single calendar event
            CalendarEventDTO.Create event = buildCalendarEvent(chore, houseCode);
            calendarService.create(event, assigneeId);

            return chore;

        } catch (Exception e) {
            throw new ChoreServiceException(
                    "Failed to add chore with assignment: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /* =========================
       HELPER: Build Calendar Event
       ========================= */
    private CalendarEventDTO.Create buildCalendarEvent(ChoreRequests chore, String houseCode) {
        CalendarEventDTO.Create event = new CalendarEventDTO.Create();

        event.setType(CalendarEventDTO.EventType.CHORE);
        event.setTitle(chore.getTaskName());
        event.setDescription(chore.getDescription());
        event.setLocation(chore.getRoom());
        event.setEstDuration(chore.getEstDurationMin());
        event.setDifficultyScore(chore.getDifficultyScore());
        event.setHouseCode(houseCode);
        event.setAssignedTo(chore.getAssignedTo());
        event.setRelatedChoreId(chore.getId());
        event.setStartDate(chore.getScheduledDate());
        event.setEndDate(chore.getScheduledDate());
        event.setAllDay(false);

        if (chore.getFrequencyPerWeek() > 0) {
            CalendarEventDTO.Recurrence rec = new CalendarEventDTO.Recurrence();
            rec.setFrequency(chore.getFrequencyPerWeek() == 1 ? "WEEKLY" : "MONTHLY");
            rec.setInterval(1);
            event.setRecurrence(rec);
        }

        return event;
    }

    /* =========================
       UPDATE CHORE STATUS
       ========================= */
    public ChoreRequests updateChoreStatus(
            String houseCode,
            String choreId,
            String status,
            int actualDuration,
            String newAssigneeId
    ) {
        try {
            DocumentReference choreRef = firestore.collection("apartments")
                    .document(houseCode)
                    .collection("chores")
                    .document(choreId);

            DocumentSnapshot choreSnap = choreRef.get().get();
            ChoreRequests oldChore = choreSnap.toObject(ChoreRequests.class);

            Map<String, Object> updates = new HashMap<>();
            updates.put("status", status);
            updates.put("actualDuration", actualDuration);
            if (newAssigneeId != null) {
                updates.put("assignedTo", newAssigneeId);
            }

            choreRef.update(updates).get();

            List<QueryDocumentSnapshot> events = firestore.collection("calendar_events")
                    .whereEqualTo("relatedChoreId", choreId)
                    .get().get().getDocuments();

            if (!events.isEmpty()) {
                String eventId = events.get(0).getId();

                Map<String, Object> calUpdates = new HashMap<>();
                calUpdates.put("status", status);
                calUpdates.put("actualDuration", actualDuration);

                if ("COMPLETED".equals(status)) {
                    calUpdates.put("endDate", Timestamp.now());
                    calUpdates.put("recurrence", FieldValue.delete());

                    if (oldChore != null && oldChore.getFrequencyPerWeek() > 0) {
                        spawnNextOccurrence(houseCode, oldChore);
                    }
                }

                firestore.collection("calendar_events")
                        .document(eventId)
                        .update(calUpdates);
            }

            return new ChoreRequests();

        } catch (Exception e) {
            throw new ChoreServiceException("Update failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // SPAWN NEXT OCCURRENCE

    private void spawnNextOccurrence(String houseCode, ChoreRequests oldChore) {
        Instant currentScheduled = Instant.parse(oldChore.getScheduledDate());

        Instant nextScheduled = oldChore.getFrequencyPerWeek() == 1
                ? currentScheduled.plusSeconds(7L * 24 * 3600)
                : currentScheduled.plusSeconds(30L * 24 * 3600);

        ChoreRequests nextChore = new ChoreRequests();
        nextChore.setTaskName(oldChore.getTaskName());
        nextChore.setRoom(oldChore.getRoom());
        nextChore.setDescription(oldChore.getDescription());
        nextChore.setDifficultyScore(oldChore.getDifficultyScore());
        nextChore.setEstDurationMin(oldChore.getEstDurationMin());
        nextChore.setFrequencyPerWeek(oldChore.getFrequencyPerWeek());
        nextChore.setAssignedTo(oldChore.getAssignedTo());
        nextChore.setScheduledDate(nextScheduled.toString());
        nextChore.setStatus("NOT_STARTED");

        this.addChore(houseCode, nextChore, oldChore.getCreatedBy());
    }
}