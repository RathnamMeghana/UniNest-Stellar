package UniNest.Backend.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import UniNest.Backend.dto.BillRequest;
import UniNest.Backend.dto.BillSplitRequest;

@Service
public class BillService {
    private static final String BILL_COLLECTION = "bills";
    private static final String BILL_SPLIT_COLLECTION = "billSplits";

    public BillRequest createBill(BillRequest request) {
        try {
            validateSplits(request);

            Firestore db = FirestoreClient.getFirestore();

            String billId = UUID.randomUUID().toString();

            BillRequest bill = new BillRequest();
            bill.setId(billId);
            bill.setTitle(request.getTitle());
            bill.setTotalAmount(request.getTotalAmount());
            bill.setBillType(request.getBillType());
            bill.setFrequency(request.getFrequency());
            bill.setHouseCode(request.getHouseCode());
            bill.setDueDate(request.getDueDate());
            bill.setActive(true);

            if (request.getSplits() != null) {
                for (BillSplitRequest split : request.getSplits()) {
                    split.setBillId(billId);
                    split.setPaid(false);    // default state
                }
            }
            bill.setSplits(request.getSplits());


            db.collection(BILL_COLLECTION)
                    .document(billId)
                    .set(bill)
                    .get();

            // Create bill splits
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
            billSplit.setAmountOwed(split.getAmountOwed());
            billSplit.setPaid(false);

            db.collection(BILL_SPLIT_COLLECTION)
                    .add(billSplit)
                    .get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create split", e);
        }
    }
    private void validateSplits(BillRequest request) {

        if (request.getSplits() == null || request.getSplits().isEmpty()) {
            throw new IllegalArgumentException("Bill splits are required");
        }

        double total = request.getSplits()
                .stream()
                .mapToDouble(BillSplitRequest::getAmountOwed)
                .sum();

        if (Double.compare(total, request.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("Split amounts must equal total bill");
        }
    }

    public void markAsPaid(String billId, String userId) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            // Find the split in the separate collection
            ApiFuture<QuerySnapshot> future = db.collection(BILL_SPLIT_COLLECTION)
                    .whereEqualTo("billId", billId)
                    .whereEqualTo("userId", userId)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            if (documents.isEmpty()) {
                throw new RuntimeException("No split found for this user and bill");
            }

            // Update the separate split document
            DocumentReference splitRef = documents.get(0).getReference();
            splitRef.update("paid", true, "paidAt", com.google.cloud.Timestamp.now()).get();


            DocumentReference billRef = db.collection(BILL_COLLECTION).document(billId);
            DocumentSnapshot billSnap = billRef.get().get();

            if (billSnap.exists()) {
                BillRequest bill = billSnap.toObject(BillRequest.class);
                if (bill != null && bill.getSplits() != null) {
                    java.util.List<BillSplitRequest> updatedSplits = new java.util.ArrayList<>(bill.getSplits());

                    boolean updated = false;
                    for (BillSplitRequest split : updatedSplits) {
                        if (split.getUserId().equals(userId)) {
                            split.setPaid(true);
                            split.setPaidAt(new java.util.Date());
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

}