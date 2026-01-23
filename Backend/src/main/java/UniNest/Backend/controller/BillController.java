package UniNest.Backend.controller;

import UniNest.Backend.dto.BillRequest;
import UniNest.Backend.service.BillService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bills")
@CrossOrigin(origins = "*")
public class BillController {

    @Autowired
    private BillService billService;

    //  Create Bill
    @PostMapping("/create")
    public BillRequest createBill(@RequestBody BillRequest request) {
        return billService.createBill(request);
    }

    //  Mark Bill Split as Paid
    @PatchMapping("/{billId}/{userId}/pay")
    public ResponseEntity<String> markAsPaid(
            @PathVariable String billId,
            @PathVariable String userId) {
        try {
            billService.markAsPaid(billId, userId);
            return ResponseEntity.ok("Split marked as paid successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating payment: " + e.getMessage());
        }
    }

    //  Get unpaid bills for a user
    @GetMapping("/getBills/{userId}")
    public ResponseEntity<List<BillRequest>> getBillsByUserId(
            @PathVariable String userId) {
        try {
            return ResponseEntity.ok(billService.getBillsByUserId(userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get paid bills (payment history)
    @GetMapping("/paidHistory/{userId}")
    public ResponseEntity<List<BillRequest>> getPaidBills(
            @PathVariable String userId) {
        try {
            return ResponseEntity.ok(billService.getPaidHistory(userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
