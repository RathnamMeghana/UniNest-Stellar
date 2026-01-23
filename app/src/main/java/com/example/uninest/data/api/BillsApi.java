package com.example.uninest.data.api;

import com.example.uninest.model.BillSplitRequest;
import com.example.uninest.model.BillsRequest;

import java.util.List;


import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface BillsApi {
    @POST("/bills/create")
    Call<BillsRequest> createBill(@Body BillsRequest request);

    @GET("/bills/getBills/{userId}")
    Call<List<BillsRequest>> getBills(@Path("userId") String userId);

    @GET("/bills/paidHistory/{userId}")
    Call<List<BillsRequest>> getPaidHistory(@Path("userId") String userId);

    @PATCH("/bills/{billId}/{userId}/pay")
    Call<Void> markBillPaid(@Path("billId") String billId, @Path("userId") String userId);




}
