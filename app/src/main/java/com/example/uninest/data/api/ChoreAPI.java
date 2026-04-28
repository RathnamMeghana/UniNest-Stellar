package com.example.uninest.data.api;

import com.example.uninest.model.Chore;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ChoreAPI {

    @GET("chores/getAll/{houseCode}")
    Call<List<Chore>> getAllChoreByApartment(@Path("houseCode") String houseCode);

    @POST("chores/addWithSmartAssign")
    Call<Chore> addWithSmartAssign(
            @Query("houseCode") String houseCode,
            @Body Chore chore
    );

    @POST("chores/addWithAssignment")
    Call<Chore> addWithAssignment(
            @Query("houseCode") String houseCode,
            @Query("userEmail") String userEmail,
            @Body Chore chore
    );

    @PATCH("chores/updateStatus")
    Call<Void> updateChoreStatus(
            @Query("houseCode") String houseCode,
            @Query("choreId") String choreId,
            @Query("status") String status,
            @Query("actualDuration") int actualDuration,
            @Query("assignedTo") String assignedTo // Nullable
    );
}