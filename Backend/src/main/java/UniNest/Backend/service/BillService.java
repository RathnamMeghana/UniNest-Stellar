package UniNest.Backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import UniNest.Backend.dto.BillRequest;
import UniNest.Backend.dto.BillSplitRequest;

@Service
public class BillService {
    private static final String BILL_COLLECTION = "bills";
    private static final String BILL_SPLIT_COLLECTION = "billSplits";

    public BillRequest createBill(BillRequest request) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            String billId = UUID.randomUUID().toString();

            // 1. Handle Solo Split if none provided
            if (request.getSplits() == null || request.getSplits().isEmpty()) {
                BillSplitRequest soloSplit = new BillSplitRequest();
                soloSplit.setUserId(request.getCreatorId());
                soloSplit.setAmountOwed(request.getTotalAmount());
                soloSplit.setBillId(billId);
                soloSplit.setBillTitle(request.getBillTitle());
                soloSplit.setPaid(false);

                request.setSplits(new ArrayList<>(List.of(soloSplit)));

                // Also save roommateIds as just the creator if none provided
                if (request.getRoommateIds() == null || request.getRoommateIds().isEmpty()) {
                    request.setRoommateIds(new ArrayList<>(List.of(request.getCreatorId())));
                }
            } else {
                validateSplits(request);

                // Ensure roommateIds contains all split userIds
                List<String> splitUserIds = request.getSplits().stream()
                        .map(BillSplitRequest::getUserId)
                        .collect(Collectors.toList());
                request.setRoommateIds(splitUserIds);
            }

            // 2. Prepare Main Bill Object
            BillRequest bill = new BillRequest();
            bill.setId(billId);
            bill.setTitle(request.getTitle());
            bill.setTotalAmount(request.getTotalAmount());
            bill.setBillType(request.getBillType());
            bill.setFrequency(request.getFrequency());
            bill.setHouseCode(request.getHouseCode());
            bill.setDueDate(request.getDueDate());
            bill.setCreatorId(request.getCreatorId());
            bill.setActive(true);

            // ✅ Save roommateIds
            bill.setRoommateIds(request.getRoommateIds());

            // 3. Prepare Splits with Title Denormalization
            if (request.getSplits() != null) {
                for (BillSplitRequest split : request.getSplits()) {
                    split.setBillId(billId);
                    split.setBillTitle(request.getTitle()); // Save title into split
                    split.setPaid(false);
                }
            }
            bill.setSplits(request.getSplits());

            // 4. Save Main Bill
            db.collection(BILL_COLLECTION)
                    .document(billId)
                    .set(bill)
                    .get();

            // 5. Create Separate Bill Split Documents
            for (BillSplitRequest split : request.getSplits()) {
                createBillSplit(billId, split, db);
            }

            return bill;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create bill", e);
        }
    }


    private void createBillSplit(String billId, BillSplitRequest split, Firestore db) {
        try {
            BillSplitRequest billSplit = new BillSplitRequest();
            billSplit.setBillId(billId);
            billSplit.setUserId(split.getUserId());
            billSplit.setBillTitle(split.getBillTitle()); // Correctly mapping the passed title
            billSplit.setAmountOwed(split.getAmountOwed());
            billSplit.setPaid(false);

            db.collection(BILL_SPLIT_COLLECTION)
                    .add(billSplit)
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create split", e);
        }
    }

    public void markAsPaid(String billId, String userId) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            // Update the split in the billSplits collection
            ApiFuture<QuerySnapshot> future = db.collection(BILL_SPLIT_COLLECTION)
                    .whereEqualTo("billId", billId)
                    .whereEqualTo("userId", userId)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            if (documents.isEmpty()) {
                throw new RuntimeException("No split found for this user and bill");
            }

            DocumentReference splitRef = documents.get(0).getReference();
            splitRef.update("paid", true, "paidAt", Timestamp.now()).get();

            // Sync update inside the main bill document (Nested Array)
            DocumentReference billRef = db.collection(BILL_COLLECTION).document(billId);
            DocumentSnapshot billSnap = billRef.get().get();

            if (billSnap.exists()) {
                BillRequest bill = billSnap.toObject(BillRequest.class);
                if (bill != null && bill.getSplits() != null) {
                    List<BillSplitRequest> updatedSplits = new ArrayList<>(bill.getSplits());
                    boolean updated = false;

                    for (BillSplitRequest split : updatedSplits) {
                        if (split.getUserId().equals(userId)) {
                            split.setPaid(true);
                            split.setPaidAt(new Date());
                            updated = true;
                        }
                    }

                    if (updated) {
                        billRef.update("splits", updatedSplits).get();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to update payment status: " + e.getMessage());
        }
    }

    public List<BillRequest> getBillsByUserId(String userId) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            List<BillRequest> results = new ArrayList<>();

            ApiFuture<QuerySnapshot> splitQuery = db.collection(BILL_SPLIT_COLLECTION)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("paid", false) // Usually you only want unpaid here
                    .get();

            for (QueryDocumentSnapshot splitDoc : splitQuery.get().getDocuments()) {
                String billId = splitDoc.getString("billId");

                if (billId != null) {
                    DocumentSnapshot billSnap = db.collection(BILL_COLLECTION).document(billId).get().get();

                    if (billSnap.exists()) {
                        BillRequest bill = billSnap.toObject(BillRequest.class);
                        if (bill != null && bill.getSplits() != null) {
                            List<BillSplitRequest> mySplit = bill.getSplits().stream()
                                    .filter(s -> s.getUserId().equals(userId))
                                    .collect(Collectors.toList());
                            bill.setSplits(mySplit);
                            results.add(bill);
                        }
                    }
                }
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching personal bills", e);
        }
    }

    public List<BillSplitRequest> getPaidHistory(String userId) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -6);
            Date sixMonthsAgo = cal.getTime();

            ApiFuture<QuerySnapshot> future = db.collection(BILL_SPLIT_COLLECTION)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("paid", true)
                    .whereGreaterThanOrEqualTo("paidAt", sixMonthsAgo)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            return documents.stream()
                    .map(doc -> doc.toObject(BillSplitRequest.class))
                    .sorted((a, b) -> {
                        if (a.getPaidAt() == null || b.getPaidAt() == null) return 0;
                        return b.getPaidAt().compareTo(a.getPaidAt());
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("Error fetching payment history: " + e.getMessage());
        }
    }

    public void deleteBill(String billId) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            ApiFuture<QuerySnapshot> splitQuery = db.collection(BILL_SPLIT_COLLECTION)
                    .whereEqualTo("billId", billId)
                    .get();

            WriteBatch batch = db.batch();
            for (QueryDocumentSnapshot doc : splitQuery.get().getDocuments()) {
                batch.delete(doc.getReference());
            }

            DocumentReference billRef = db.collection(BILL_COLLECTION).document(billId);
            batch.delete(billRef);
            batch.commit().get();

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete bill: " + e.getMessage());
        }
    }

    private void validateSplits(BillRequest request) {
        if (request.getSplits() == null || request.getSplits().isEmpty()) return;

        double total = request.getSplits().stream()
                .mapToDouble(BillSplitRequest::getAmountOwed)
                .sum();

        if (Math.abs(total - request.getTotalAmount()) > 0.01) {
            throw new IllegalArgumentException("Split amounts must equal total bill");
        }
    }

    public double getTotalOwedByUserId(String userId) {
        try {
            // Use the existing getBillsByUserId method to get all active personal bills
            List<BillRequest> bills = getBillsByUserId(userId);

            // Calculate the sum of amountOwed from the user's splits
            return bills.stream()
                    .flatMap(bill -> bill.getSplits().stream()) // Flatten the lists of splits
                    .filter(split -> !split.isPaid())           // Only count unpaid splits
                    .mapToDouble(BillSplitRequest::getAmountOwed)
                    .sum();

        } catch (Exception e) {
            throw new RuntimeException("Error calculating total owed for user: " + userId, e);
        }
    }
}