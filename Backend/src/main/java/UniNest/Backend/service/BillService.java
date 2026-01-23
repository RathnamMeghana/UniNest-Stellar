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

    // ------------------- CREATE BILL -------------------
    public BillRequest createBill(BillRequest request) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            String billId = UUID.randomUUID().toString();
            request.setId(billId);
            request.setActive(true);

            // If no splits provided, default to creator
            if (request.getSplits() == null || request.getSplits().isEmpty()) {
                BillRequest.Split defaultSplit = new BillRequest.Split();
                defaultSplit.setUserId(request.getCreatorId());
                defaultSplit.setAmountOwed(request.getTotalAmount());
                defaultSplit.setPaid(false);
                defaultSplit.setPaidAt(null);
                defaultSplit.setBillId(billId);
                defaultSplit.setBillTitle(request.getTitle());

                request.setSplits(new ArrayList<>(List.of(defaultSplit)));

                if (request.getRoommateIds() == null || request.getRoommateIds().isEmpty()) {
                    request.setRoommateIds(new ArrayList<>(List.of(request.getCreatorId())));
                }
            } else {
                validateSplits(request);

                // Ensure splits have billId and billTitle
                for (BillRequest.Split split : request.getSplits()) {
                    split.setBillId(billId);
                    split.setBillTitle(request.getTitle());
                    split.setPaid(false);
                    split.setPaidAt(null);
                }

                // Ensure roommateIds contains all split userIds
                List<String> splitUserIds = request.getSplits().stream()
                        .map(BillRequest.Split::getUserId)
                        .collect(Collectors.toList());
                request.setRoommateIds(splitUserIds);
            }

            // Save bill with embedded splits
            db.collection(BILL_COLLECTION)
                    .document(billId)
                    .set(request)
                    .get();

            return request;

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
