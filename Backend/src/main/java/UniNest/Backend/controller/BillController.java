package UniNest.Backend.controller;

import UniNest.Backend.dto.BillRequest;
import UniNest.Backend.service.BillService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bills")
@CrossOrigin(origins = "*")
public class BillController {

    @Autowired
    private BillService billService;


    @PostMapping("/create")
    public BillRequest createBill(@RequestBody BillRequest request) {
        return billService.createBill(request);
    }

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

}
