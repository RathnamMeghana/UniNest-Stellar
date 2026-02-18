package UniNest.Backend.flow;

import UniNest.Backend.config.SecurityConfig;
import UniNest.Backend.controller.CalendarController;
import UniNest.Backend.controller.GlobalExceptionHandler;
import UniNest.Backend.dto.CalendarEventDTO;
import UniNest.Backend.security.FirebaseTokenFilter;
import UniNest.Backend.service.CalendarService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.util.StringUtils.delete;

//  Point to CalendarController
@WebMvcTest(controllers = CalendarController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = true)
public class CalendarSecurityFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Mock CalendarService
    @MockBean
    private CalendarService calendarService;

    @MockBean
    private FirebaseTokenFilter firebaseTokenFilter;


    private CalendarEventDTO.Create validRequest;

    @BeforeEach
    public void setup() throws Exception {
        // Fix: Assign to class-level field so it's not null in the test
        validRequest = new CalendarEventDTO.Create();
        validRequest.setTitle("Test Event");
        validRequest.setHouseCode("HOUSE123");
        validRequest.setAssignedTo("user123");
        validRequest.setType(CalendarEventDTO.EventType.EVENT);
        validRequest.setAllDay(false);

        String nowIso = Instant.now().toString();
        validRequest.setStartDate(nowIso);
        validRequest.setEndDate(nowIso);

        // Filter Bypass
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(firebaseTokenFilter).doFilter(any(), any(), any());

        // Stub service to avoid 500 errors
        when(calendarService.create(any(), any())).thenReturn(new CalendarEventDTO.Response());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    public void createEvent_AsTenant_Allowed() throws Exception {

        mockMvc.perform(post("/calendar/create")
                        .param("userId", "user123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(calendarService, times(1)).create(any(), eq("user123"));
    }

    @Test
    @WithMockUser(roles = "LETTINGAGENT")
    public void createEvent_AsAgent_Allowed() throws Exception {
        mockMvc.perform(post("/calendar/create")
                        .param("userId", "agent123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("Security Flow: Event title and description are sanitized")
    public void createEvent_SanitizationCheck() throws Exception {
        CalendarEventDTO.Create unsafeRequest = new CalendarEventDTO.Create();
        unsafeRequest.setTitle("Party <script>alert('XSS')</script>");
        unsafeRequest.setDescription("Join us <b>now</b>!");
        unsafeRequest.setHouseCode("H123");
        unsafeRequest.setType(CalendarEventDTO.EventType.EVENT);

        mockMvc.perform(post("/calendar/create")
                        .param("userId", "user123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unsafeRequest)))
                .andExpect(status().isOk());

        // Capture the object passed to the service
        ArgumentCaptor<CalendarEventDTO.Create> captor = ArgumentCaptor.forClass(CalendarEventDTO.Create.class);
        verify(calendarService).create(captor.capture(), eq("user123"));

        // Verify sanitization logic was triggered
        assertFalse(captor.getValue().getTitle().contains("<script>"), "Scripts should be stripped");
        assertFalse(captor.getValue().getDescription().contains("<b>"), "HTML tags should be stripped");
    }

    @Test
    @WithMockUser(roles = "USER") // Neither TENANT nor LETTINGAGENT
    @DisplayName("Security Flow: Standard User is FORBIDDEN from creating events")
    public void createEvent_AsGenericUser_Forbidden() throws Exception {
        mockMvc.perform(post("/calendar/create")
                        .param("userId", "user123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden()); // Blocks before reaching controller

        verify(calendarService, never()).create(any(), any());
    }
    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("Logic Flow: Correct service method called when date range is provided")
    public void getEvents_WithDateRange_CallsRangeService() throws Exception {
        String start = "2024-01-01";
        String end = "2024-01-07";

        mockMvc.perform(get("/calendar/getByApartment/HOUSE123")
                        .param("start", start)
                        .param("end", end))
                .andExpect(status().isOk());

        // Verify the "Range" method was called, NOT the "getForApartment" method
        verify(calendarService, times(1)).getForRange("HOUSE123", start, end);
        verify(calendarService, never()).getForapartment(anyString());
    }

    @Test
    @WithMockUser(roles = "TENANT")
    @DisplayName("Security Flow: Tenant can delete an event")
    public void deleteEvent_AsTenant_Allowed() throws Exception {
        mockMvc.perform(delete("/calendar/delete/event123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Event deleted")));

        verify(calendarService, times(1)).delete("event123");
    }
}