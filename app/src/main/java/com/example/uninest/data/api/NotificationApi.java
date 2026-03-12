package com.example.uninest.data.api;

import com.example.uninest.model.AppNotification;
import com.example.uninest.model.RegisterTokenRequest;
import com.example.uninest.model.SendNotificationRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface NotificationApi {

    @POST("notifications/register-token")
    Call<Void> registerToken(@Body RegisterTokenRequest req);

    @GET("notifications/my")
    Call<List<AppNotification>> getMyNotifications();

    @POST("notifications/send")
    Call<Integer> sendNotification(@Body SendNotificationRequest req);
}