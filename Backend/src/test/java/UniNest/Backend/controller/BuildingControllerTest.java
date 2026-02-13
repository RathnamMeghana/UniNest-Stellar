package UniNest.Backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;


import UniNest.Backend.dto.BuildingRequest;
import UniNest.Backend.model.Building;
import UniNest.Backend.service.BuildingService;



@WebMvcTest(BuildingController.class)
public class BuildingControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private BuildingService buildingService;
    @Autowired
    private ObjectMapper objectMapper;

    // create building success
    @Test
    void createBuilding_success() throws Exception {
        BuildingRequest request = new BuildingRequest();
        request.setName("Building A");
        request.setAddressLine1("123 Main St");
        request.setCity("Dundalk");
        request.setPostcode("A92 333");
        request.setCountry("Ireland");
        request.setLandlordId("landlord123");
        request.setActive(true);
        when(buildingService.createBuilding(any()))
                .thenReturn("Building created");
        mockMvc.perform(post("/buildings/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Building created"));
    }

    //create building validationError
    @Test
    void createBuilding_validationError() throws Exception {
        BuildingRequest request = new BuildingRequest(); // missing required fields

        mockMvc.perform(post("/buildings/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // get all buildings success
    @Test
    void getAllBuildings_success() throws Exception {
        Building building = new Building();
        building.setName("Building A");

        when(buildingService.getAllBuildings())
                .thenReturn(List.of(building));

        mockMvc.perform(get("/buildings/getAll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Building A"));
    }

}
