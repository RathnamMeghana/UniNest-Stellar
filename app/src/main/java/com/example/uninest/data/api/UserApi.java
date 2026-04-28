package com.example.uninest.data.api;

import com.example.uninest.model.User;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface UserApi {
    @GET("users/roommates/{houseCode}")
    Call<List<User>> getRoommates(@Path("houseCode") String houseCode);
}