package com.example.uninest.data.api;

import com.example.uninest.model.Ticket;
import com.example.uninest.model.UpdateTicketPriorityRequest;
import com.example.uninest.model.UpdateTicketStatusRequest;
import com.example.uninest.model.UpdateTicketAgentDataRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;


public interface TicketApi {
    @POST("tickets/create")
    Call<String> createTicket(@Body Ticket ticket);

    @GET("tickets/building")
    Call<List<Ticket>> getTicketsByBuilding(@Query("name") String building);

    @GET("tickets/apartment")
    Call<List<Ticket>> getTicketsByApartment(@Query("name") String houseCode);

    @GET("tickets/landlord")
    Call<List<Ticket>> getTicketsByLandlord(@Query("id") String landlordId);

    @retrofit2.http.PUT("tickets/status")
    Call<String> updateStatus(@Body UpdateTicketStatusRequest body);

    @retrofit2.http.PUT("tickets/priority")
    Call<String> updatePriority(@Body UpdateTicketPriorityRequest body);

    @PUT("tickets/agent-update")
    Call<String> updateAgentData(@Body UpdateTicketAgentDataRequest body);
}
