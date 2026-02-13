package UniNest.Backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import UniNest.Backend.dto.ApartmentRequests;
import UniNest.Backend.dto.RoomRequests;
import UniNest.Backend.exception.UserServiceException;
import UniNest.Backend.model.Apartment;
import UniNest.Backend.model.Room;
import UniNest.Backend.model.User;
import UniNest.Backend.service.ApartmentService;
import UniNest.Backend.service.RoomService;
import UniNest.Backend.service.UserService;

import static org.mockito.Mockito.when;


@AutoConfigureMockMvc
@WebMvcTest(ApartmentController.class)
class ApartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApartmentService apartmentService;

    @MockBean
    private UserService userService;

    @MockBean
    private RoomService roomService;

    @Autowired
    private ObjectMapper objectMapper;
   // create apartment success
   @WithMockUser(username = "testuser", roles = {"1"})
    @Test
    void createApartment_success() throws Exception {
        ApartmentRequests request = new ApartmentRequests();
        request.setName("Apartment A");
        request.setBuildingId("building123");
        request.setCode("A1");
        request.setLandlordId("landlord123");
        request.setDescription("Nice apartment");
        request.setTotalRooms("3");
        request.setActive(true);

        when(apartmentService.createApartment(any()))
                .thenReturn("Apartment created");

        mockMvc.perform(post("/apartments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Apartment created"));
    }
    @WithMockUser(username = "testuser", roles = {"1"})
    // create apartment validation Error
    @Test
    void createApartment_validationError() throws Exception {
        ApartmentRequests request = new ApartmentRequests(); // missing required fields

        mockMvc.perform(post("/apartments/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // get all apartments success
    @WithMockUser(username = "testuser", roles = {"2"})
    @Test
    void getAllApartments_success() throws Exception {
        Apartment apartment = new Apartment();
        apartment.setName("Apartment A");

        when(apartmentService.getAllApartments())
                .thenReturn(List.of(apartment));

        mockMvc.perform(get("/apartments/getAll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Apartment A"));
    }

   // get all users by the apartment success
   @WithMockUser(username = "testuser", roles = {"1"})
    @Test
    void getUsersByApartment_success() throws Exception, UserServiceException {
        User user = new User();
        user.setEmail("test@uninest.com");

        when(userService.getUsersForApartment("A1"))
                .thenReturn(List.of(user));

        mockMvc.perform(get("/apartments/A1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("test@uninest.com"));
    }

    // get all users by the apartment not found
    @Test
    void getUsersByApartment_notFound() throws Exception, UserServiceException {
        when(userService.getUsersForApartment("A1"))
                .thenReturn(List.of());

        mockMvc.perform(get("/apartments/A1/users"))
                .andExpect(status().isNotFound());
    }

    // add room to apartment success
    @Test
    void addRoom_success() throws Exception {
        RoomRequests request = new RoomRequests();
        request.setType("Single");
        request.setLabel("Room 1");

        when(roomService.addRoom(eq("A1"), any(RoomRequests.class)))
                .thenReturn("room123");

        mockMvc.perform(post("/apartments/A1/addRoom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Room added with ID: room123"));
    }

    // add room to apartment validation error
    @Test
    void addRoom_validationError() throws Exception {
        RoomRequests request = new RoomRequests(); // missing required fields

        mockMvc.perform(post("/apartments/A1/addRoom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // get rooms success

    @Test
    void getRooms_success() throws Exception {
        RoomRequests room = new RoomRequests();
        room.setLabel("Room 1");

        when(roomService.getRooms("A1"))
                .thenReturn(List.of(room));

        mockMvc.perform(get("/apartments/A1/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("Room 1"));
    }

// get rooms not found
    @Test
    void getRooms_notFound() throws Exception {
        when(roomService.getRooms("A1"))
                .thenReturn(List.of());

        mockMvc.perform(get("/apartments/A1/rooms"))
                .andExpect(status().isNotFound());
    }

    // remove tenent from apartment success

    @Test
    void removeTenant_success() throws Exception {
        mockMvc.perform(delete("/apartments/tenants/test@uninest.com/remove"))
                .andExpect(status().isOk())
                .andExpect(content().string("Tenant removed from apartment."));
    }
}
