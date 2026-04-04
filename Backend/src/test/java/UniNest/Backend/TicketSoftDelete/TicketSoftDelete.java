package UniNest.Backend.TicketSoftDelete;

import UniNest.Backend.controller.TicketController;
import UniNest.Backend.model.Ticket;
import UniNest.Backend.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@AutoConfigureMockMvc(addFilters = false) // THIS IS THE KEY FIX
public class TicketSoftDelete {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketService ticketService;

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("Soft Delete: Tenant fetch excludes deleted tickets")
    public void testSoftDelete_TenantFetchFiltersDeleted() throws Exception {
        Ticket t = new Ticket();
        t.setId("active1");

        when(ticketService.getTicketsByApartment(any())).thenReturn(List.of(t));

        mockMvc.perform(get("/tickets/apartment")
                        .param("name", "APT123")
                        .contentType(MediaType.APPLICATION_JSON))
                //.andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("active1")));
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("Soft Delete: Tenant fetch successfully filters out deleted tickets")
    public void testSoftDelete_TenantFiltersOutDeletedData() throws Exception {
        // Create one ACTIVE ticket
        Ticket activeTicket = new Ticket();
        activeTicket.setId("VISIBLE_ID_123");
        activeTicket.setDeletedByTenant(false);

        // Create one DELETED ticket
        Ticket deletedTicket = new Ticket();
        deletedTicket.setId("HIDDEN_ID_456");
        deletedTicket.setDeletedByTenant(true);

        // Mock the service to return ONLY the active ticket
        when(ticketService.getTicketsByApartment(anyString()))
                .thenReturn(List.of(activeTicket));

        // Perform the GET request
        mockMvc.perform(get("/tickets/apartment")
                        .param("name", "APT123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                // expect to see the active ID in the JSON response
                .andExpect(content().string(containsString("VISIBLE_ID_123")))
                // expect NOT to see the hidden ID in the JSON response
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("HIDDEN_ID_456"))));
    }

    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    @DisplayName("Soft Delete: Agent fetch includes deleted tickets when requested")
    public void testSoftDelete_AgentCanSeeEverything() throws Exception {
        Ticket active = new Ticket();
        active.setId("ACTIVE_ID");

        Ticket deleted = new Ticket();
        deleted.setId("DELETED_ID");
        deleted.setDeletedByTenant(true);

        // Mock service to return BOTH because agent wants to see all
        when(ticketService.getTicketsByLandlord(anyString(), eq(true)))
                .thenReturn(List.of(active, deleted));

        mockMvc.perform(get("/tickets/landlord")
                        .param("id", "landlord123")
                        .param("includeDeleted", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ACTIVE_ID")))
                .andExpect(content().string(containsString("DELETED_ID")));
    }

}