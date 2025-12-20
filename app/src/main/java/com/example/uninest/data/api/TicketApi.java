package com.example.uninest.data.api;

import com.example.uninest.model.Ticket;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;


public interface TicketApi {
        @POST("api/tickets/create")
        Call<String> createTicket(@Body Ticket ticket);
    }
