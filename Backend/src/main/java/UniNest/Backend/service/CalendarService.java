package UniNest.Backend.service;

import UniNest.Backend.dto.CalendarEventDTO;
import UniNest.Backend.model.CalendarEvent;
import UniNest.Backend.model.Recurrence;
import UniNest.Backend.util.SanitizationUtil;
import UniNest.Backend.exception.CalendarServiceException;

import com.google.api.core.ApiFuture;

import com.google.cloud.firestore.*;
import com.google.cloud.Timestamp;
import com.google.firebase.cloud.FirestoreClient;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class CalendarService {

    private static final String COLLECTION = "calendar_events";

    // ---------------- CREATE ----------------
    public CalendarEventDTO.Response create(CalendarEventDTO.Create dto, String createdBy) {
        if (dto.getHouseCode() == null || dto.getHouseCode().isBlank()) {
            throw new IllegalArgumentException("houseCode is required");
        }

        try {
            Firestore db = FirestoreClient.getFirestore();

            // Create the Firestore document
            DocumentReference docRef = db.collection(COLLECTION).document();

            // Ensure createdBy is optional
            CalendarEventDTO.Response event = new CalendarEventDTO.Response();
            event.setId(docRef.getId());
            event.setHouseCode(dto.getHouseCode());
            event.setType(dto.getType());
            event.setTitle(dto.getTitle());
            event.setDescription(dto.getDescription());
            event.setStartDate(dto.getStartDate());
            event.setEndDate(dto.getEndDate());
            event.setAllDay(dto.isAllDay());
            event.setAssignedTo(dto.getAssignedTo());
            event.setRelatedChoreId(dto.getRelatedChoreId());
            event.setCreatedBy(createdBy);
            event.setRecurrence(dto.getRecurrence());

            // Save to Firestore
            docRef.set(event).get();

            return event;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CalendarServiceException("Calendar event creation interrupted", e);
        } catch (ExecutionException | FirestoreException e) {
            throw new CalendarServiceException("Failed to create calendar event", e);
        }
    }


    //  GET EVENTS FOR apartment
    public List<CalendarEventDTO.Response> getForapartment(String houseCode) {

        try {
            Firestore db = FirestoreClient.getFirestore();
            ApiFuture<QuerySnapshot> future = db.collection(COLLECTION)
                    .whereEqualTo("houseCode", houseCode)
                    .get();

            List<QueryDocumentSnapshot> docs = future.get().getDocuments();

            return docs.stream()
                    .map(d -> d.toObject(CalendarEvent.class))
                    .filter(Objects::nonNull)
                    .map(this::map)
                    .collect(Collectors.toList());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CalendarServiceException("Fetch interrupted", e);
        } catch (ExecutionException e) {
            throw new CalendarServiceException("Failed to fetch events", e);
        } catch (FirestoreException e) {
            throw new CalendarServiceException("Firestore unavailable", e);
        }
    }

    //  GET EVENTS FOR USER
    public List<CalendarEventDTO.Response> getForUser(String userId) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            ApiFuture<QuerySnapshot> future = db.collection(COLLECTION)
                    .whereEqualTo("assignedTo", userId)
                    .get();

            List<QueryDocumentSnapshot> docs = future.get().getDocuments();

            return docs.stream()
                    .map(d -> d.toObject(CalendarEvent.class))
                    .filter(Objects::nonNull)
                    .map(this::map)
                    .collect(Collectors.toList());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CalendarServiceException("Fetch interrupted", e);
        } catch (ExecutionException e) {
            throw new CalendarServiceException("Failed to fetch user events", e);
        } catch (FirestoreException e) {
            throw new CalendarServiceException("Firestore unavailable", e);
        }
    }

    // UPDATE
    public CalendarEventDTO.Response update(String eventId, CalendarEventDTO.Update dto) {
        if (eventId == null || eventId.isBlank()) throw new IllegalArgumentException("Event id required");
        dto.sanitize();

        try {
            Firestore db = FirestoreClient.getFirestore();
            DocumentReference ref = db.collection(COLLECTION).document(eventId);
            DocumentSnapshot snapshot = ref.get().get();
            CalendarEvent event = snapshot.toObject(CalendarEvent.class);

            if (event == null) throw new IllegalArgumentException("Event not found");

            if (dto.getTitle() != null) event.setTitle(dto.getTitle());
            if (dto.getDescription() != null) event.setDescription(dto.getDescription());
            if (dto.getStartDate() != null) event.setStartDate(dto.getStartDate());
            if (dto.getEndDate() != null) event.setEndDate(dto.getEndDate());
            if (dto.getAllDay() != null) event.setAllDay(dto.getAllDay());
            if (dto.getAssignedTo() != null) event.setAssignedTo(dto.getAssignedTo());
            if (dto.getRecurrence() != null) event.setRecurrence(mapRecurrence(dto.getRecurrence()));

            ref.set(event).get();
            return map(event);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CalendarServiceException("Update interrupted", e);
        } catch (ExecutionException e) {
            throw new CalendarServiceException("Failed to update event", e);
        } catch (FirestoreException e) {
            throw new CalendarServiceException("Firestore unavailable", e);
        }
    }

    // ---------------- DELETE ----------------
    public void delete(String eventId) {
        if (eventId == null || eventId.isBlank()) throw new IllegalArgumentException("Event id required");

        try {
            Firestore db = FirestoreClient.getFirestore();
            db.collection(COLLECTION).document(eventId).delete().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CalendarServiceException("Delete interrupted", e);
        } catch (ExecutionException e) {
            throw new CalendarServiceException("Failed to delete event", e);
        } catch (FirestoreException e) {
            throw new CalendarServiceException("Firestore unavailable", e);
        }
    }

    //GET EVENTS FOR HOUSE THE RANGE
    public List<CalendarEventDTO.Response> getForRange(String houseCode, String startIso, String endIso) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            Timestamp start = parse(startIso);
            Timestamp end = parse(endIso);

            ApiFuture<QuerySnapshot> future = db.collection(COLLECTION)
                    .whereEqualTo("houseCode", houseCode)
                    .whereLessThanOrEqualTo("startDate", end)
                    .whereGreaterThanOrEqualTo("endDate", start) // overlap logic
                    .get();

            List<QueryDocumentSnapshot> docs = future.get().getDocuments();

            return docs.stream()
                    .map(d -> d.toObject(CalendarEvent.class))
                    .filter(Objects::nonNull)
                    .map(this::map)
                    .collect(Collectors.toList());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CalendarServiceException("Fetch interrupted", e);
        } catch (ExecutionException e) {
            throw new CalendarServiceException("Failed to fetch events for range", e);
        } catch (FirestoreException e) {
            throw new CalendarServiceException("Firestore unavailable", e);
        }
    }


    // ---------------- HELPERS ----------------
    private Timestamp parse(String iso) {
        Instant i = Instant.parse(iso);
        return Timestamp.ofTimeSecondsAndNanos(i.getEpochSecond(), i.getNano());
    }

    private Recurrence mapRecurrence(CalendarEventDTO.Recurrence dto) {
        Recurrence r = new Recurrence();
        r.setFrequency(Recurrence.RecurrenceFrequency.valueOf(dto.getFrequency()));
        r.setInterval(dto.getInterval());
        r.setDaysOfWeek(dto.getDaysOfWeek());
        r.setEndDate(dto.getEndDate());
        return r;
    }

    private CalendarEventDTO.Response map(CalendarEvent e) {
        CalendarEventDTO.Response dto = new CalendarEventDTO.Response();
        dto.setId(e.getId());
        dto.setHouseCode(e.getHouseCode());

        // Safe enum mapping
        if (e.getType() != null) {
            try {
                dto.setType(CalendarEventDTO.EventType.valueOf(e.getType()));
            } catch (IllegalArgumentException ex) {
                // unknown value in DB → use default
                dto.setType(CalendarEventDTO.EventType.OTHER);
            }
        } else {
            dto.setType(CalendarEventDTO.EventType.OTHER); // default if null
        }

        dto.setTitle(e.getTitle());
        dto.setDescription(e.getDescription());
        dto.setStartDate(e.getStartDate());
        dto.setEndDate(e.getEndDate());
        dto.setAllDay(e.isAllDay());
        dto.setCreatedBy(e.getCreatedBy());
        dto.setAssignedTo(e.getAssignedTo());
        dto.setRelatedChoreId(e.getRelatedChoreId());
        dto.setRecurrence(null); // optionally map recurrence here
        return dto;
    }

}
