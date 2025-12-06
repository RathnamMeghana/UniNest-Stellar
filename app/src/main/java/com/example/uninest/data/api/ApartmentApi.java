
package com.example.uninest.data.api;

import com.example.uninest.model.Apartment;
import com.example.uninest.model.ApartmentRequest;
import com.example.uninest.model.User;


import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;


public interface ApartmentApi {

    @GET("/apartments/getAll")
    Call<List<Apartment>> getAllApartments();

    @POST("/apartments/create")
    Call<String> createApartment(@Body ApartmentRequest request);


    @GET("apartments/{houseCode}/users")
    Call<List<User>> getUsersForApartment(
            @Path("houseCode") String houseCode
    );

    @DELETE("apartments/tenants/{email}/remove")
    Call<Void> removeTenant(@Path(value = "email", encoded = true) String email);

}
