package UniNest.Backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
            event.setHouseCode(houseCode);
            event.setAssignedTo(chore.getAssignedTo());
            event.setRelatedChoreId(chore.getId());
            event.setStartDate(eventTimestamp);
            event.setEndDate(eventTimestamp);
            event.setAllDay(false);

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

            String userId = userSnapshot.getDocuments().get(0).getId();

            chore.setAssignedTo(userId);
            chore.setHouseCode(houseCode);
            chore.setCreatedAt(Timestamp.now());

            CollectionReference choresRef = db.collection("apartments")
                    .document(houseCode)
                    .collection("chores");

            DocumentReference docRef = choresRef.document();
            chore.setId(docRef.getId());

            docRef.set(chore).get();

            return chore;

        } catch (Exception e) {
            throw new ChoreServiceException("Failed to add chore with assignment", e);
        }
    }
}
