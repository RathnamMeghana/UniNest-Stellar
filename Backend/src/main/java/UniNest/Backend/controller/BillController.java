package UniNest.Backend.controller;

import UniNest.Backend.dto.BillRequest;
import UniNest.Backend.dto.OwedToUserResponse;
import UniNest.Backend.service.BillService;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bills")
@CrossOrigin(origins = "*")
public class BillController {

    @Autowired
    private BillService billService;

    //  Create Bill
    @PreAuthorize("hasRole('TENANT')")
    @PostMapping("/create")
    public List<BillRequest> createBill(@Valid @RequestBody BillRequest request) {
        request.sanitize();
        return billService.createBill(request);
    }


    //  Mark Bill  as Paid
    @PreAuthorize("hasRole('TENANT')")
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
    @PreAuthorize("hasRole('TENANT')")
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
    @PreAuthorize("hasRole('TENANT')")
    @GetMapping("/paidHistory/{userId}")
    public ResponseEntity<List<BillRequest>> getPaidBills(
            @PathVariable String userId) {
        try {
            return ResponseEntity.ok(billService.getPaidHistory(userId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @PreAuthorize("hasRole('TENANT')")
    @GetMapping("/totalOwed/{userId}")
    public double getTotalOwed(@PathVariable String userId) {
        return billService.getTotalOwedByUserId(userId);
    }
    @PreAuthorize("hasRole('TENANT')")
    @GetMapping("/owedToMe/{userId}")
    public List<OwedToUserResponse> getOwedToUser(@PathVariable String userId) {
        return billService.getWhatIsOwedToUser(userId);
    }

    @PreAuthorize("hasRole('TENANT')")
    @GetMapping("/owedToMe/total/{userId}")
    public double getTotalOwedToUser(@PathVariable String userId) {
        return billService.getTotalOwedToUser(userId);
    }


}
