package com.example.uninest.data.api;

import com.example.uninest.model.Calendar;
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

    // Create a new calendar event (e.g., from a chore)
    @POST("calendar/create")
    Call<Calendar> createEvent(
            @Body Calendar event,
            @Query("userId") String userId
    );

    // Optional: delete a calendar event by ID
    @POST("calendar/delete")
    Call<Void> deleteEvent(
            @Query("eventId") String eventId
    );
}
