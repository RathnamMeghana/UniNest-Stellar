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
    // ------------------- CREATE BILL -------------------
    public List<BillRequest> createBill(BillRequest request) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            List<BillRequest> savedBills = new ArrayList<>();

            // If no splits provided, default to creator
            if (request.getSplits() == null || request.getSplits().isEmpty()) {
                BillRequest.Split defaultSplit = new BillRequest.Split();
                defaultSplit.setUserId(request.getCreatorId());
                defaultSplit.setAmountOwed(request.getTotalAmount());
                defaultSplit.setPaid(false);
                defaultSplit.setPaidAt(null);

                request.setSplits(new ArrayList<>(List.of(defaultSplit)));

                if (request.getRoommateIds() == null || request.getRoommateIds().isEmpty()) {
                    request.setRoommateIds(new ArrayList<>(List.of(request.getCreatorId())));
                }
            } else {
                validateSplits(request);

                // Ensure roommateIds contains all split userIds
                List<String> splitUserIds = request.getSplits().stream()
                        .map(BillRequest.Split::getUserId)
                        .collect(Collectors.toList());
                request.setRoommateIds(splitUserIds);
            }

            // Determine number of bills to create (1 for one-time, 6 for recurring)
            int repeatCount = 1;
            if (request.getBillType() == BillRequest.BillType.RECURRING) {
                repeatCount = 6; // 6 months
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(request.getStartDate());

            for (int i = 0; i < repeatCount; i++) {
                String billId = UUID.randomUUID().toString();

                BillRequest newBill = new BillRequest();
                newBill.setId(billId);
                newBill.setTitle(request.getTitle());
                newBill.setTotalAmount(request.getTotalAmount());
                newBill.setCreatorId(request.getCreatorId());
                newBill.setBillType(request.getBillType());
                newBill.setFrequency(request.getFrequency());
                newBill.setActive(true);
                newBill.setRoommateIds(request.getRoommateIds());

                // Set start and due dates
                newBill.setStartDate(cal.getTime());

                // If dueDate is provided, calculate relative to startDate
                if (request.getDueDate() != null) {
                    long diff = request.getDueDate().getTime() - request.getStartDate().getTime();
                    newBill.setDueDate(new Date(cal.getTimeInMillis() + diff));
                }

                // Copy splits
                List<BillRequest.Split> newSplits = new ArrayList<>();
                for (BillRequest.Split split : request.getSplits()) {
                    BillRequest.Split newSplit = new BillRequest.Split();
                    newSplit.setUserId(split.getUserId());
                    newSplit.setAmountOwed(split.getAmountOwed());
                    newSplit.setPaid(false);
                    newSplits.add(newSplit);
                }
                newBill.setSplits(newSplits);

                // Save to Firestore
                db.collection(BILL_COLLECTION)
                        .document(billId)
                        .set(newBill)
                        .get();

                savedBills.add(newBill);

                // Increment start date for next recurring bill
                if (request.getBillType() == BillRequest.BillType.RECURRING) {
                    if (request.getFrequency() == BillRequest.BillFrequency.MONTHLY) {
                        cal.add(Calendar.MONTH, 1);
                    } else if (request.getFrequency() == BillRequest.BillFrequency.WEEKLY) {
                        cal.add(Calendar.WEEK_OF_YEAR, 1);
                    } else if (request.getFrequency() == BillRequest.BillFrequency.BIWEEKLY) {
                        cal.add(Calendar.WEEK_OF_YEAR, 2);
                    }
                }
            }

            return savedBills;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create bill(s)", e);
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
