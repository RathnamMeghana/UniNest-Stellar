package UniNest.Backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.*;
import java.util.stream.Collectors;

import UniNest.Backend.dto.BillRequest;
import UniNest.Backend.dto.OwedToUserResponse;

@Service
public class BillService {



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

            // 1. Determine how many occurrences to create
            // RECURRING creates 6 months of data, ONE_TIME creates exactly 1
            int occurrences = (request.getBillType() == BillRequest.BillType.RECURRING) ? 6 : 1;

            // 2. Set the starting date
            Date baseDate = request.getDueDate() != null ? request.getDueDate() : new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(baseDate);

            for (int i = 0; i < occurrences; i++) {
                // Generate a unique ID for this specific occurrence
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

                // Set Roommate IDs (for the 'whereArrayContains' queries)
                billCopy.setRoommateIds(new ArrayList<>(request.getRoommateIds()));

                // Set specific dates for this instance
                billCopy.setDueDate(cal.getTime());
                billCopy.setStartDate(new Date()); // Current timestamp for creation

                // 3. Process Splits
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

                // 4. Save to Firestore
                db.collection(BILL_COLLECTION).document(uniqueBillId).set(billCopy).get();
                createdBills.add(billCopy);

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

            return createdBills;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create bill: " + e.getMessage(), e);
        }
    }

    // ------------------- MARK SPLIT AS PAID -------------------
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
                            split.setBillId(bill.getId());
                            split.setBillTitle(bill.getTitle());
                            updated = true;
                        }
                    }
                    if (updated) {
                        // Persist the updated splits to Firestore
                        billRef.update("splits", bill.getSplits()).get();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to update payment status: " + e.getMessage(), e);
        }
    }

    // ------------------- GET UNPAID BILLS -------------------

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
                if (bill != null && bill.getSplits() != null) {
                    List<BillRequest.Split> mySplit = bill.getSplits().stream()
                            .filter(s -> s.getUserId().equals(userId) && !s.isPaid())
                            .collect(Collectors.toList());
                    if (!mySplit.isEmpty()) {
                        bill.setSplits(mySplit);
                        results.add(bill);
                    }
                }
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching unpaid bills", e);
        }
    }

    // ------------------- GET PAID HISTORY -------------------
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
                if (bill != null && bill.getSplits() != null) {
                    List<BillRequest.Split> paidSplits = bill.getSplits().stream()
                            .filter(s -> s.getUserId().equals(userId) && s.isPaid())
                            .collect(Collectors.toList());
                    if (!paidSplits.isEmpty()) {
                        bill.setSplits(paidSplits);
                        results.add(bill);
                    }
                }
            }

            // Sort by paidAt descending
            results.sort((a, b) -> {
                Date aDate = a.getSplits().get(0).getPaidAt();
                Date bDate = b.getSplits().get(0).getPaidAt();
                if (aDate == null || bDate == null) return 0;
                return bDate.compareTo(aDate);
            });

            return results;

        } catch (Exception e) {
            throw new RuntimeException("Error fetching paid history", e);
        }
    }

    // ------------------- DELETE BILL -------------------
    public void deleteBill(String billId) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            db.collection(BILL_COLLECTION).document(billId).delete().get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete bill: " + e.getMessage(), e);
        }
    }

    // ------------------- VALIDATE SPLITS -------------------
    private void validateSplits(BillRequest request) {
        if (request.getSplits() == null || request.getSplits().isEmpty()) return;

        double total = request.getSplits().stream()
                .mapToDouble(BillRequest.Split::getAmountOwed)
                .sum();

        if (Math.abs(total - request.getTotalAmount()) > 0.01) {
            throw new IllegalArgumentException("Split amounts must equal total bill");
        }
    }

    // ------------------- TOTAL OWED BY USER -------------------
    public double getTotalOwedByUserId(String userId) {
        try {
            List<BillRequest> bills = getBillsByUserId(userId);

            return bills.stream()
                    .flatMap(bill -> bill.getSplits().stream())
                    .filter(split -> !split.isPaid())
                    .mapToDouble(BillRequest.Split::getAmountOwed)
                    .sum();
        } catch (Exception e) {
            throw new RuntimeException("Error calculating total owed for user: " + userId, e);
        }
    }

    // ------------------- GET WHAT IS OWED TO USER -------------------
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
                            owed.setBillId(bill.getId());
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

            // Convert map to list and sort by due date (soonest first)
            List<OwedToUserResponse> results = new ArrayList<>(latestSplits.values());
            results.sort(Comparator.comparing(OwedToUserResponse::getDueDate, Comparator.nullsLast(Date::compareTo)));

            return results;

        } catch (Exception e) {
            throw new RuntimeException("Error fetching amounts owed to user", e);
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
                if (bill != null) results.add(bill);
            }


            results.sort((a, b) -> {
                if (a.getDueDate() == null || b.getDueDate() == null) return 0;
                return b.getDueDate().compareTo(a.getDueDate());
            });

            return results;

        } catch (Exception e) {
            throw new RuntimeException("Error fetching bills created by user", e);
        }
    }

    private boolean shouldShowBillOccurrence(BillRequest bill, Date weekStart, Date weekEnd, Date monthStart, Date monthEnd) {

        if (bill.getBillType() == BillRequest.BillType.ONE_TIME) {
            return true; // keep your current behavior for one-time
        }

        Date due = bill.getDueDate();
        if (due == null) return false;

        BillRequest.BillFrequency freq = bill.getFrequency();
        if (freq == null) return false;

        switch (freq) {
            case WEEKLY:
            case BIWEEKLY:
                return !due.before(weekStart) && !due.after(weekEnd);

            case MONTHLY:
                return !due.before(monthStart) && !due.after(monthEnd);

            default:
                return true;
        }
    }

    private Date startOfWeek(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date endOfWeek(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startOfWeek(date));
        cal.add(Calendar.DAY_OF_WEEK, 6);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    private Date startOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date endOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startOfMonth(date));
        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.MILLISECOND, -1);
        return cal.getTime();
    }

}
