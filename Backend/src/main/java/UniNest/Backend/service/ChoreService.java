package UniNest.Backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import UniNest.Backend.dto.CalendarEventDTO;
import UniNest.Backend.dto.ChoreRequests;
import UniNest.Backend.exception.ChoreServiceException;

@Service
public class ChoreService {

    private final CalendarService calendarService;

    public ChoreService(CalendarService calendarService) {
        this.calendarService = calendarService;
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
            Firestore db = FirestoreClient.getFirestore();

            CollectionReference choresRef = db.collection("apartments")
                    .document(houseCode)
                    .collection("chores");

            List<QueryDocumentSnapshot> docs = choresRef.get().get().getDocuments();

            for (QueryDocumentSnapshot doc : docs) {
                try {
                    ChoreRequests chore = doc.toObject(ChoreRequests.class);
                    chore.setId(doc.getId());

                    if (chore.getAssignedTo() != null && !chore.getAssignedTo().isBlank()) {
                        DocumentSnapshot userDoc = db.collection("users")
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
            throw new ChoreServiceException("Chore query interrupted", e);
        } catch (ExecutionException e) {
            throw new ChoreServiceException("Failed to fetch chores", e);
        }
    }

    /* =========================
       ADD CHORE (SMART ASSIGN)
       ========================= */
    public ChoreRequests addChore(String houseCode, ChoreRequests chore, String userId) {

        if (houseCode == null || houseCode.isBlank()) {
            throw new IllegalArgumentException("houseCode is required");
        }

        try {
            Firestore db = FirestoreClient.getFirestore();

            /* 1 Sanitize incoming JSON */
            chore.sanitize();

            /* 2️ Set required metadata */
            chore.setHouseCode(houseCode);
            chore.setCreatedBy(userId);
            chore.setCreatedAt(Timestamp.now());


            if (chore.getScheduledDate() == null || chore.getScheduledDate().isBlank()) {
                chore.setScheduledDate(Instant.now().toString());
            }

            /* 3️ Persist chore */
            CollectionReference choresRef = db.collection("apartments")
                    .document(houseCode)
                    .collection("chores");


            DocumentReference docRef = choresRef.document();
            chore.setId(docRef.getId());

            docRef.set(chore).get();

            /* 4️ Create calendar event */
            Instant scheduledInstant = Instant.parse(chore.getScheduledDate());
            Timestamp eventTimestamp = Timestamp.ofTimeSecondsAndNanos(
                    scheduledInstant.getEpochSecond(),
                    scheduledInstant.getNano()
            );

            CalendarEventDTO.Create event = new CalendarEventDTO.Create();


            event.setType(CalendarEventDTO.EventType.CHORE);
            event.setTitle(chore.getTaskName());
            event.setEstDuration(chore.getEstDurationMin());
            event.setDescription("Room: " + chore.getRoom());
            event.setHouseCode(houseCode);
            event.setAssignedTo(chore.getAssignedTo());
            event.setRelatedChoreId(chore.getId());
            event.setStartDate(chore.getScheduledDate());
            event.setEndDate(chore.getScheduledDate());
            event.setAllDay(false);

            if (chore.getFrequencyPerWeek() > 0) {
                CalendarEventDTO.Recurrence rec = new CalendarEventDTO.Recurrence();

                if (chore.getFrequencyPerWeek() == 1) {
                    rec.setFrequency("WEEKLY");
                } else {
                    rec.setFrequency("MONTHLY");
                }

                rec.setInterval(1);

                event.setRecurrence(rec);
            }

            calendarService.create(event, userId);

            /* 5️ RETURN THE SAVED OBJECT */
            return chore;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ChoreServiceException("Chore creation interrupted", e);
        } catch (ExecutionException e) {
            throw new ChoreServiceException("Failed to add chore", e);
        }
    }

    /* =========================
       UPDATE ASSIGNMENT
       ========================= */
    public ChoreRequests updateAssignmentByTaskNameAndUserEmail(
            String houseCode,
            String taskName,
            String userEmail
    ) {

        try {
            Firestore db = FirestoreClient.getFirestore();

            Query userQuery = db.collection("users")
                    .whereEqualTo("email", userEmail)
                    .whereEqualTo("houseCode", houseCode);

            QuerySnapshot userSnapshot = userQuery.get().get();
            if (userSnapshot.isEmpty()) {
                throw new ChoreServiceException("User not found", null);
            }

            String userId = userSnapshot.getDocuments().get(0).getId();

            Query choreQuery = db.collection("apartments")
                    .document(houseCode)
                    .collection("chores")
                    .whereEqualTo("taskName", taskName);

            QuerySnapshot choreSnapshot = choreQuery.get().get();
            if (choreSnapshot.isEmpty()) {
                throw new ChoreServiceException("Chore not found", null);
            }

            DocumentSnapshot choreDoc = choreSnapshot.getDocuments().get(0);
            choreDoc.getReference().update("assignedTo", userId).get();

            return choreDoc.toObject(ChoreRequests.class);

        } catch (Exception e) {
            throw new ChoreServiceException("Failed to update assignment", e);
        }
    }

    /* =========================
       ADD CHORE WITH ASSIGNMENT
       ========================= */
    public ChoreRequests addChoreWithAssignment(
            String houseCode,
            String userEmail,
            ChoreRequests chore
    ) {

        try {
            Firestore db = FirestoreClient.getFirestore();

            chore.sanitize();

            Query userQuery = db.collection("users")
                    .whereEqualTo("email", userEmail)
                    .whereEqualTo("houseCode", houseCode);

            QuerySnapshot userSnapshot = userQuery.get().get();
            if (userSnapshot.isEmpty()) {
                throw new ChoreServiceException("User not found", null);
            }

            String assigneeId = userSnapshot.getDocuments().get(0).getId();

            // 2. Setup Chore Object
            chore.setAssignedTo(assigneeId);
            chore.setHouseCode(houseCode);
            chore.setCreatedAt(Timestamp.now());
            if (chore.getScheduledDate() == null || chore.getScheduledDate().isBlank()) {
                chore.setScheduledDate(Instant.now().toString());
            }

// 3. Save Chore to Firestore
            CollectionReference choresRef = db.collection("apartments")
                    .document(houseCode).collection("chores");
            DocumentReference docRef = choresRef.document();
            chore.setId(docRef.getId());
            docRef.set(chore).get();

            // 4. Create Calendar Event
            CalendarEventDTO.Create event = new CalendarEventDTO.Create();
            event.setType(CalendarEventDTO.EventType.CHORE);
            event.setTitle(chore.getTaskName());
            event.setDescription(chore.getDescription());
            event.setLocation(chore.getRoom());
            event.setEstDuration(chore.getEstDurationMin());
            event.setHouseCode(houseCode);
            event.setAssignedTo(assigneeId);
            event.setRelatedChoreId(chore.getId());
            event.setStartDate(chore.getScheduledDate());
            event.setEndDate(chore.getScheduledDate());
            event.setAllDay(false);

            // Handle Frequency/Recurrence
            if (chore.getFrequencyPerWeek() > 0) {
                CalendarEventDTO.Recurrence rec = new CalendarEventDTO.Recurrence();
                if (chore.getFrequencyPerWeek() == 1) {
                    rec.setFrequency("WEEKLY");
                } else {
                    rec.setFrequency("MONTHLY");
                }
                rec.setInterval(1);
                event.setRecurrence(rec);
            }

            String creatorId = (chore.getCreatedBy() != null) ? chore.getCreatedBy() : assigneeId;

            calendarService.create(event, creatorId);

            return chore;

        } catch (Exception e) {
            throw new ChoreServiceException("Failed to add chore with assignment: " + e.getMessage(), e);
        }
    }

    public ChoreRequests updateChoreStatus(String houseCode, String choreId, String status, int actualDuration, String newAssigneeId) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            // 1. Get current chore data
            DocumentReference choreRef = db.collection("apartments").document(houseCode).collection("chores").document(choreId);
            DocumentSnapshot choreSnap = choreRef.get().get();
            ChoreRequests oldChore = choreSnap.toObject(ChoreRequests.class);

            // 2. Mark CURRENT chore as COMPLETED
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", status);
            updates.put("actualDuration", actualDuration);
            if (newAssigneeId != null) updates.put("assignedTo", newAssigneeId);
            choreRef.update(updates).get();

            // 3. Update the Calendar Event to stop it from repeating
            List<QueryDocumentSnapshot> events = db.collection("calendar_events")
                    .whereEqualTo("relatedChoreId", choreId).get().get().getDocuments();

            if (!events.isEmpty()) {
                String eventId = events.get(0).getId();
                Map<String, Object> calUpdates = new HashMap<>();
                calUpdates.put("status", status);
                calUpdates.put("actualDuration", actualDuration);

                if ("COMPLETED".equals(status)) {
                    calUpdates.put("endDate", Timestamp.now()); // Set completion date

                    //  Delete the recurrence field from the COMPLETED record.
                    // This prevents the "Green Completed" card from appearing on future dates.
                    calUpdates.put("recurrence", FieldValue.delete());

                    // 4. Create a FRESH chore record for the NEXT week/month
                    if (oldChore != null && oldChore.getFrequencyPerWeek() > 0) {
                        spawnNextOccurrence(houseCode, oldChore);
                    }
                }
                db.collection("calendar_events").document(eventId).update(calUpdates);
            }
            return new ChoreRequests();
        } catch (Exception e) {
            throw new ChoreServiceException("Update failed", e);
        }
    }

    private void spawnNextOccurrence(String houseCode, ChoreRequests oldChore) {
        // 1. Calculate the next date
        Instant currentScheduled = Instant.parse(oldChore.getScheduledDate());
        Instant nextScheduledDate = (oldChore.getFrequencyPerWeek() == 1)
                ? currentScheduled.plus(java.time.Duration.ofDays(7))  // +7 days
                : currentScheduled.plus(java.time.Duration.ofDays(30)); // +30 days

        // 2. Create a totally new Chore object with a different ID
        ChoreRequests nextChore = new ChoreRequests();
        nextChore.setTaskName(oldChore.getTaskName());
        nextChore.setRoom(oldChore.getRoom());
        nextChore.setDescription(oldChore.getDescription());
        nextChore.setDifficultyScore(oldChore.getDifficultyScore());
        nextChore.setEstDurationMin(oldChore.getEstDurationMin());
        nextChore.setFrequencyPerWeek(oldChore.getFrequencyPerWeek());
        nextChore.setAssignedTo(oldChore.getAssignedTo());
        nextChore.setScheduledDate(nextScheduledDate.toString()); // Set to future date
        nextChore.setStatus("NOT_STARTED"); // NEW task starts fresh

        // 3. Save it as a new document (Creates new ID and new Calendar event)
        this.addChore(houseCode, nextChore, oldChore.getCreatedBy());
    }
}
