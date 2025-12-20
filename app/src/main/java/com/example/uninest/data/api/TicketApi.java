package com.example.uninest.data.api;

import com.example.uninest.model.Ticket;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;


public interface TicketApi {
        @POST("api/tickets/create")
        Call<String> createTicket(@Body Ticket ticket);

    @GET("api/tickets/building")
    Call<List<Ticket>> getTicketsByBuilding(@Query("name") String building);
}
