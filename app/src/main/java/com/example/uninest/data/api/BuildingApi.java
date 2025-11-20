package com.example.uninest.data.api;

import com.example.uninest.model.Building;
import com.example.uninest.model.BuildingRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface BuildingApi {

    @GET("/buildings/getAll")
    Call<List<Building>> getAllBuildings();

    @POST("/buildings/create")
    Call<String> createBuilding(@Body BuildingRequest request);
}
