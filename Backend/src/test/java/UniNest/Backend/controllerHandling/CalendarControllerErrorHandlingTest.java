package UniNest.Backend.controllerHandling;

import UniNest.Backend.controller.CalendarController;
import UniNest.Backend.controller.GlobalExceptionHandler;
import UniNest.Backend.dto.CalendarEventDTO;
import UniNest.Backend.exception.CalendarServiceException;
import UniNest.Backend.service.CalendarService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Nested
@WebMvcTest(controllers = {CalendarController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CalendarControllerErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CalendarService calendarService;

    @Test
    @WithMockUser
    @DisplayName("POST /calendar/create - Should return 400 when title is blank")
    public void createEvent_WhenValidationFails_Returns400() throws Exception {
        CalendarEventDTO.Create invalidRequest = new CalendarEventDTO.Create();
        invalidRequest.setTitle(""); // Invalid

        mockMvc.perform(post("/calendar/create")
                        .param("userId", "user123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").exists());
    }
    @Test
    @WithMockUser
    @DisplayName("PUT /calendar/update/{id} - Should return 404 when event ID is missing")
    public void updateEvent_WhenNotFound_Returns404() throws Exception {
        CalendarEventDTO.Update request = new CalendarEventDTO.Update();
        request.setTitle("New Title");

        // Mock the service to throw a 404
        when(calendarService.update(eq("missing-id"), any()))
                .thenThrow(new CalendarServiceException("Event not found with ID: missing-id", HttpStatus.NOT_FOUND));

        mockMvc.perform(put("/calendar/update/missing-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Event not found")));
    }


    @Test
    @WithMockUser
    @DisplayName("GET /calendar/getByUser/{userId} - Should return 500 when Firestore fails")
    public void getEvents_WhenServiceFails_Returns500() throws Exception {
        when(calendarService.getForUser("user123"))
                .thenThrow(new CalendarServiceException("Calendar database unavailable", HttpStatus.INTERNAL_SERVER_ERROR));

        mockMvc.perform(get("/calendar/getByUser/user123"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("database unavailable")));
    }



    @Test
    @WithMockUser
    @DisplayName("GET /calendar/getByApartment/{house} - Should return 400 for invalid date format")
    public void getEvents_WhenDateFormatInvalid_Returns400() throws Exception {
        // 1. Tell the mock to throw a 400 error when it gets "invalid-date"
        // Use ArgumentMatchers (eq and anyString) to match the call
        when(calendarService.getForRange(eq("HOUSE123"), eq("invalid-date"), anyString()))
                .thenThrow(new CalendarServiceException("Invalid date format provided", HttpStatus.BAD_REQUEST));

        // 2. Perform the request
        mockMvc.perform(get("/calendar/getByApartment/HOUSE123")
                        .param("start", "invalid-date")
                        .param("end", "2024-12-31"))

                .andExpect(status().isBadRequest()) // Now it will correctly receive 400
                .andExpect(content().string(containsString("Invalid date format")));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /calendar/getByUser/{userId} - Should return 404 when user doesn't exist")
    public void getEvents_WhenUserNotFound_Returns404() throws Exception {
        when(calendarService.getForUser("fake-user"))
                .thenThrow(new CalendarServiceException("User not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/calendar/getByUser/fake-user"))
                .andExpect(status().isNotFound());
    }
}
