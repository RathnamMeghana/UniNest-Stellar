package UniNest.Backend.controller;

import UniNest.Backend.dto.BillRequest;
import UniNest.Backend.dto.OwedToUserResponse;
import UniNest.Backend.service.BillService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BillController.class)
class BillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BillService billService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createBill_returnsCreatedBills() throws Exception {
        BillRequest.Split split = new BillRequest.Split();
        split.setUserId("user1");
        split.setAmountOwed(50);
        split.setPaid(false);

        BillRequest request = new BillRequest();
        request.setTitle("Electricity");
        request.setHouseCode("HOUSE123");
        request.setCreatorId("creator1");
        request.setBillType(BillRequest.BillType.ONE_TIME);
        request.setTotalAmount(50);
        request.setRoommateIds(List.of("user1"));
        request.setSplits(List.of(split));

        Mockito.when(billService.createBill(Mockito.any()))
                .thenReturn(List.of(request));

        mockMvc.perform(post("/bills/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Electricity"));
    }


    @Test
    void markAsPaid_success() throws Exception {
        mockMvc.perform(patch("/bills/123/user1/pay"))
                .andExpect(status().isOk())
                .andExpect(content().string("Split marked as paid successfully"));
    }

    @Test
    void getBillsByUserId_returnsBills() throws Exception {
        Mockito.when(billService.getBillsByUserId("user1"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/bills/getBills/user1"))
                .andExpect(status().isOk());
    }

    @Test
    void getPaidHistory_returnsHistory() throws Exception {
        Mockito.when(billService.getPaidHistory("user1"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/bills/paidHistory/user1"))
                .andExpect(status().isOk());
    }

    @Test
    void getTotalOwed_returnsAmount() throws Exception {
        Mockito.when(billService.getTotalOwedByUserId("user1"))
                .thenReturn(120.50);

        mockMvc.perform(get("/bills/totalOwed/user1"))
                .andExpect(status().isOk())
                .andExpect(content().string("120.5"));
    }

    @Test
    void getOwedToUser_returnsList() throws Exception {
        Mockito.when(billService.getWhatIsOwedToUser("user1"))
                .thenReturn(List.of(new OwedToUserResponse()));

        mockMvc.perform(get("/bills/owedToMe/user1"))
                .andExpect(status().isOk());
    }

    @Test
    void createBill_missingRequiredFields_returnsBadRequest() throws Exception {
        BillRequest invalidRequest = new BillRequest();
        invalidRequest.setTitle("Electricity");

        mockMvc.perform(post("/bills/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

}

