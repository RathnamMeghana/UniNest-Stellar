package com.example.uninest.data.api;

import com.example.uninest.model.Building;
import com.example.uninest.model.BuildingRequest;



import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;


public interface BuildingApi {

    @GET("/buildings/getAll")
    Call<List<Building>> getAllBuildings();

    @POST("/buildings/create")
    Call<String> createBuilding(@Body BuildingRequest request);

    @GET("/buildings/byLandlord")
    Call<List<Building>> getBuildingsByLandlord(@Query("landlordId") String landlordId);

    @GET("buildings/{id}")
    Call<Building> getBuilding(@Path("id") String buildingId);

    @retrofit2.http.DELETE("buildings/{id}")
    Call<Void> deleteBuilding(@Path("id") String buildingId);
}
