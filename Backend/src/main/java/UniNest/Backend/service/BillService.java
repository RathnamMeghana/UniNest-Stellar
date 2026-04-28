package UniNest.Backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.*;
import java.util.stream.Collectors;

import UniNest.Backend.dto.BillRequest;
import UniNest.Backend.dto.OwedToUserResponse;
import UniNest.Backend.exception.BillServiceException;

@Service
public class BillService {

    @Autowired
    private NotificationService notificationService;

    private void checkOwnership(String userId) throws AccessDeniedException {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!currentUser.equals(userId)) {
            throw new AccessDeniedException("Cannot access another user's bills");
        }
    }

    private static final String BILL_COLLECTION = "bills";

    public List<BillRequest> createBill(BillRequest request) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            List<BillRequest> createdBills = new ArrayList<>();

            int occurrences = (request.getBillType() == BillRequest.BillType.RECURRING) ? 6 : 1;

            Date baseDate = request.getDueDate() != null ? request.getDueDate() : new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(baseDate);

            List<String> tenantIds = new ArrayList<>();
            if (request.getRoommateIds() != null) {
                tenantIds.addAll(request.getRoommateIds());
            }

            Long firstEventTime = null;

            for (int i = 0; i < occurrences; i++) {
                String uniqueBillId = UUID.randomUUID().toString();

                BillRequest billCopy = new BillRequest();
                billCopy.setId(uniqueBillId);
                billCopy.setTitle(request.getTitle());
                billCopy.setTotalAmount(request.getTotalAmount());
                billCopy.setCreatorId(request.getCreatorId());
                billCopy.setBillType(request.getBillType());
                billCopy.setFrequency(request.getFrequency());
                billCopy.setHouseCode(request.getHouseCode());
                billCopy.setActive(true);
                billCopy.setRoommateIds(new ArrayList<>(request.getRoommateIds()));
                billCopy.setDueDate(cal.getTime());
                billCopy.setStartDate(new Date());

                List<BillRequest.Split> splitsCopy = new ArrayList<>();
                if (request.getSplits() != null) {
                    for (BillRequest.Split s : request.getSplits()) {
                        BillRequest.Split splitInstance = new BillRequest.Split();
                        splitInstance.setUserId(s.getUserId());
                        splitInstance.setAmountOwed(s.getAmountOwed());
                        splitInstance.setPaid(false);
                        splitInstance.setBillId(uniqueBillId);
                        splitInstance.setBillTitle(request.getTitle());
                        splitsCopy.add(splitInstance);
                    }
                }
                billCopy.setSplits(splitsCopy);

                db.collection(BILL_COLLECTION).document(uniqueBillId).set(billCopy).get();
                createdBills.add(billCopy);

                if (firstEventTime == null && billCopy.getDueDate() != null) {
                    firstEventTime = billCopy.getDueDate().getTime();
                }

                if (occurrences > 1 && request.getFrequency() != null) {
                    switch (request.getFrequency()) {
                        case WEEKLY:
                            cal.add(Calendar.DAY_OF_YEAR, 7);
                            break;
                        case BIWEEKLY:
                            cal.add(Calendar.DAY_OF_YEAR, 14);
                            break;
                        case MONTHLY:
                            cal.add(Calendar.MONTH, 1);
                            break;
                    }
                }
            }

            if (!tenantIds.isEmpty()) {
                String pushTitle;
                String pushBody;

                if (occurrences > 1) {
                    pushTitle = "Recurring bill created";
                    pushBody = request.getTitle() + " has been scheduled";
                } else {
                    pushTitle = "Rent Due!";
                    pushBody = request.getTitle() + " is due soon";
                }

                String firstBillId = createdBills.isEmpty() ? null : createdBills.get(0).getId();

                notificationService.createRentNotification(
                        tenantIds,
                        pushTitle,
                        pushBody,
                        firstBillId,
                        firstEventTime
                );

                notificationService.sendToUsers(pushTitle, pushBody, tenantIds, "BILLS", firstBillId);
            }

            return createdBills;
        } catch (Exception e) {
            throw new BillServiceException("Failed to create bill: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void markAsPaid(String billId, String userId) throws AccessDeniedException {
        checkOwnership(userId);
        try {
            Firestore db = FirestoreClient.getFirestore();
            DocumentReference billRef = db.collection(BILL_COLLECTION).document(billId);
            DocumentSnapshot billSnap = billRef.get().get();

            if (billSnap.exists()) {
                BillRequest bill = billSnap.toObject(BillRequest.class);
                if (bill != null && bill.getSplits() != null) {
                    boolean updated = false;
                    for (BillRequest.Split split : bill.getSplits()) {
                        if (split.getUserId().equals(userId)) {
                            split.setPaid(true);
                            split.setPaidAt(new Date());
                            updated = true;
                        }
                    }
                    if (updated) {
                        billRef.update("splits", bill.getSplits()).get();

                        String title = "Bill paid";
                        String body = bill.getTitle() + " was marked paid";

                        List<String> creatorIds = new ArrayList<>();
                        if (bill.getCreatorId() != null && !bill.getCreatorId().isBlank() && !bill.getCreatorId().equals(userId)) {
                            creatorIds.add(bill.getCreatorId());
                        }

                        if (!creatorIds.isEmpty()) {
                            notificationService.createRentNotification(
                                    creatorIds,
                                    title,
                                    body,
                                    bill.getId(),
                                    System.currentTimeMillis()
                            );
                            notificationService.sendToUsers(title, body, creatorIds, "BILLS", bill.getId());
                        }

                        if (bill.getHouseCode() != null && !bill.getHouseCode().isBlank()) {
                            List<String> agentIds = notificationService.getAgentUserIdsByHouseCode(bill.getHouseCode());
                            if (!agentIds.isEmpty()) {
                                notificationService.createRentNotification(
                                        agentIds,
                                        title,
                                        body,
                                        bill.getId(),
                                        System.currentTimeMillis()
                                );
                                notificationService.sendToUsers(title, body, agentIds, "AGENT_BILLS", bill.getId());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new BillServiceException("Error marking as paid: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<BillRequest> getBillsByUserId(String userId) throws AccessDeniedException {
        checkOwnership(userId);
        try {
            Firestore db = FirestoreClient.getFirestore();
            List<BillRequest> results = new ArrayList<>();

            ApiFuture<QuerySnapshot> future = db.collection(BILL_COLLECTION)
                    .whereArrayContains("roommateIds", userId)
                    .get();

            for (QueryDocumentSnapshot doc : future.get().getDocuments()) {
                BillRequest bill = doc.toObject(BillRequest.class);
                if (bill != null) {
                    bill.setId(doc.getId());

                    if (bill.getSplits() != null) {
                        List<BillRequest.Split> mySplit = bill.getSplits().stream()
                                .filter(s -> s.getUserId().equals(userId) && !s.isPaid())
                                .collect(Collectors.toList());
                        if (!mySplit.isEmpty()) {
                            bill.setSplits(mySplit);
                            results.add(bill);
                        }
                    }
                }
            }
            return results;
        } catch (Exception e) {
            throw new BillServiceException("Error fetching unpaid bills", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<BillRequest> getPaidHistory(String userId) throws AccessDeniedException {
        checkOwnership(userId);
        try {
            Firestore db = FirestoreClient.getFirestore();
            List<BillRequest> results = new ArrayList<>();

            ApiFuture<QuerySnapshot> future = db.collection(BILL_COLLECTION)
                    .whereArrayContains("roommateIds", userId)
                    .get();

            for (QueryDocumentSnapshot doc : future.get().getDocuments()) {
                BillRequest bill = doc.toObject(BillRequest.class);
                if (bill != null) {
                    bill.setId(doc.getId());

                    if (bill.getSplits() != null) {
                        List<BillRequest.Split> paidSplits = bill.getSplits().stream()
                                .filter(s -> s.getUserId().equals(userId) && s.isPaid())
                                .collect(Collectors.toList());
                        if (!paidSplits.isEmpty()) {
                            bill.setSplits(paidSplits);
                            results.add(bill);
                        }
                    }
                }
            }

            results.sort((a, b) -> {
                if (a.getSplits().isEmpty() || b.getSplits().isEmpty()) return 0;
                Date aDate = a.getSplits().get(0).getPaidAt();
                Date bDate = b.getSplits().get(0).getPaidAt();
                if (aDate == null || bDate == null) return 0;
                return bDate.compareTo(aDate);
            });

            return results;
        } catch (Exception e) {
            throw new BillServiceException("Error fetching paid history", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void deleteBill(String billId) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            db.collection(BILL_COLLECTION).document(billId).delete().get();
        } catch (Exception e) {
            throw new BillServiceException("Failed to delete bill", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public double getTotalOwedByUserId(String userId) {
        try {
            List<BillRequest> bills = getBillsByUserId(userId);
            return bills.stream()
                    .flatMap(bill -> bill.getSplits().stream())
                    .filter(split -> !split.isPaid())
                    .mapToDouble(BillRequest.Split::getAmountOwed)
                    .sum();
        } catch (Exception e) {
            return 0.0;
        }
    }

    public List<OwedToUserResponse> getWhatIsOwedToUser(String userId) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            Map<String, OwedToUserResponse> latestSplits = new HashMap<>();

            ApiFuture<QuerySnapshot> future = db.collection(BILL_COLLECTION)
                    .whereEqualTo("creatorId", userId)
                    .get();

            for (QueryDocumentSnapshot doc : future.get().getDocuments()) {
                BillRequest bill = doc.toObject(BillRequest.class);
                if (bill == null || bill.getSplits() == null) continue;

                for (BillRequest.Split split : bill.getSplits()) {
                    if (!split.getUserId().equals(userId) && !split.isPaid()) {
                        String groupKey = bill.getTitle() + "_" + split.getUserId();
                        Date currentDueDate = bill.getDueDate();

                        if (!latestSplits.containsKey(groupKey) ||
                                (currentDueDate != null && currentDueDate.before(latestSplits.get(groupKey).getDueDate()))) {

                            OwedToUserResponse owed = new OwedToUserResponse();
                            owed.setBillId(doc.getId());
                            owed.setBillTitle(bill.getTitle());
                            owed.setDebtorUserId(split.getUserId());
                            owed.setAmountOwed(split.getAmountOwed());
                            owed.setDueDate(currentDueDate);
                            owed.setBillType(bill.getBillType());
                            owed.setFrequency(bill.getFrequency());

                            latestSplits.put(groupKey, owed);
                        }
                    }
                }
            }
            List<OwedToUserResponse> results = new ArrayList<>(latestSplits.values());
            results.sort(Comparator.comparing(OwedToUserResponse::getDueDate, Comparator.nullsLast(Date::compareTo)));
            return results;
        } catch (Exception e) {
            throw new BillServiceException("Error fetching amounts owed to user", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public double getTotalOwedToUser(String userId) {
        return getWhatIsOwedToUser(userId).stream()
                .mapToDouble(OwedToUserResponse::getAmountOwed)
                .sum();
    }

    public List<BillRequest> getBillsCreatedBy(String userId) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            List<BillRequest> results = new ArrayList<>();

            ApiFuture<QuerySnapshot> future = db.collection(BILL_COLLECTION)
                    .whereEqualTo("creatorId", userId)
                    .get();

            for (QueryDocumentSnapshot doc : future.get().getDocuments()) {
                BillRequest bill = doc.toObject(BillRequest.class);
                if (bill != null) {
                    bill.setId(doc.getId());
                    results.add(bill);
                }
            }

            results.sort((a, b) -> {
                if (a.getDueDate() == null || b.getDueDate() == null) return 0;
                return b.getDueDate().compareTo(a.getDueDate());
            });

            return results;
        } catch (Exception e) {
            throw new BillServiceException("Error fetching bills created by user", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}