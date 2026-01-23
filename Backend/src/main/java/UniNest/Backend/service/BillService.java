package UniNest.Backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import UniNest.Backend.dto.BillRequest;

@Service
public class BillService {
    private static final String BILL_COLLECTION = "bills";

    public List<BillRequest> createBill(BillRequest request) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            List<BillRequest> createdBills = new ArrayList<>();
            int months = request.getBillType() == BillRequest.BillType.RECURRING ? 6 : 1;

            Date startDate = request.getStartDate() != null ? request.getStartDate() : request.getDueDate();
            if (startDate == null) startDate = new Date();

            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);

            for (int i = 0; i < months; i++) {
                BillRequest billCopy = new BillRequest();
                billCopy.setId(UUID.randomUUID().toString());
                billCopy.setTitle(request.getTitle());
                billCopy.setTotalAmount(request.getTotalAmount());
                billCopy.setCreatorId(request.getCreatorId());
                billCopy.setBillType(request.getBillType());
                billCopy.setFrequency(request.getFrequency());
                billCopy.setActive(true);
                billCopy.setHouseCode(request.getHouseCode());

                // Copy splits
                List<BillRequest.Split> splitsCopy = new ArrayList<>();
                if (request.getSplits() != null) {
                    for (BillRequest.Split s : request.getSplits()) {
                        BillRequest.Split splitCopy = new BillRequest.Split();
                        splitCopy.setUserId(s.getUserId());
                        splitCopy.setAmountOwed(s.getAmountOwed());
                        splitCopy.setPaid(s.isPaid());
                        splitsCopy.add(splitCopy);
                    }
                }
                billCopy.setSplits(splitsCopy);

                // Roommates
                billCopy.setRoommateIds(new ArrayList<>(request.getRoommateIds()));

                // Dates
                billCopy.setStartDate(cal.getTime());
                billCopy.setDueDate(cal.getTime()); // can adjust if needed

                // Save to Firestore
                db.collection(BILL_COLLECTION).document(billCopy.getId()).set(billCopy).get();

                createdBills.add(billCopy);

                // Move calendar to next month for recurring bills
                cal.add(Calendar.MONTH, 1);
            }

            return createdBills;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create bill", e);
        }
    }


    // ------------------- MARK SPLIT AS PAID -------------------
    public void markAsPaid(String billId, String userId) {
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
    public List<BillRequest> getBillsByUserId(String userId) {
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
    public List<BillRequest> getPaidHistory(String userId) {
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
}
