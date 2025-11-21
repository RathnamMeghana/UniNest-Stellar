
package com.example.uninest.data.api;

import com.example.uninest.model.Apartment;
import com.example.uninest.model.ApartmentRequest;



import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;


public interface ApartmentApi {

    @GET("/apartments/getAll")
    Call<List<Apartment>> getAllApartments();

    @POST("/apartments/create")
    Call<String> createApartment(@Body ApartmentRequest request);
}
