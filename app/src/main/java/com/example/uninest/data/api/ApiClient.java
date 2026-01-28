package com.example.uninest.data.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ApiClient {

    //private static final String BASE_URL = "http://127.0.0.1:8080/";
    private static final String BASE_URL = "http://192.168.1.70:8080/";
    //private static final String BASE_URL = "http://10.102.198.130:8080/";

    private static Retrofit retrofit;

    // A single private method to initialize Retrofit once with the correct settings
    private static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            // 1. Setup Logging
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            // 2. Setup Custom GSON for Date Formatting
            // This pattern matches your backend: "yyyy-MM-dd'T'HH:mm:ss.SSSX"
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                    .create();

            // 3. Build Retrofit
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(ScalarsConverterFactory.create()) // For plain text
                    .addConverterFactory(GsonConverterFactory.create(gson)) // For JSON with correct Dates
                    .build();
        }
        return retrofit;
    }

    // --- API Service Getters ---

    public static BuildingApi getBuildingApi() {
        return getRetrofitInstance().create(BuildingApi.class);
    }

    public static ApartmentApi getApartmentApi(){
        return getRetrofitInstance().create(ApartmentApi.class);
    }

    public static TicketApi getTicketApi(){
        return getRetrofitInstance().create(TicketApi.class);
    }

    public static ChoreAPI getChoreApi(){
        return getRetrofitInstance().create(ChoreAPI.class);
    }

    public static CalendarApi getCalendarApi(){
        return getRetrofitInstance().create(CalendarApi.class);
    }

    public static BillsApi getBillsApi(){
        return getRetrofitInstance().create(BillsApi.class);
    }

    public static UserApi getUserApi() {
        return getRetrofitInstance().create(UserApi.class);
    }
}