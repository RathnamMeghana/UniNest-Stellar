package com.example.uninest.data.api;

import com.example.uninest.model.Calendar;
import com.example.uninest.model.CalendarRequest;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CalendarApi {

    // Get all calendar events for a house
    @GET("calendar/getByApartment/{houseCode}")
    Call<List<Calendar>> getByApartment(@Path("houseCode") String houseCode);

    // Get all calendar events for a specific user
    @GET("calendar/getForUser/{userId}")
    Call<List<Calendar>> getEventsForUser(@Path("userId") String userId);

    // Create a new calendar event if a chore a chore will also be created
    @POST("calendar/create")
    Call<Calendar> createEvent(
            @Body CalendarRequest event,
            @Query("userId") String userId
    );




    @POST("calendar/delete")
    Call<Void> deleteEvent(
            @Query("eventId") String eventId
    );
}
