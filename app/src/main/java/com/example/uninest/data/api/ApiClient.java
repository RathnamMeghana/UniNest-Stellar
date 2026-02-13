package com.example.uninest.data.api;

import com.example.uninest.AuthInterceptor; // Make sure this path is correct
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "http://192.168.1.70:8080/";

    private static Retrofit retrofit;

    private static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            // 1. Setup Logging
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // 2. Setup the AuthInterceptor (The 403 Fixer)
            AuthInterceptor authInterceptor = new AuthInterceptor();

            // 3. Configure OkHttpClient with BOTH interceptors
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor) // Automatically adds the Firebase Token
                    .addInterceptor(logging)         // Logs the request/response for debugging
                    .build();

            // 4. Setup Custom GSON
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                    .create();

            // 5. Build Retrofit
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    // --- API Service Getters remain the same ---
    public static BuildingApi getBuildingApi() { return getRetrofitInstance().create(BuildingApi.class); }
    public static ApartmentApi getApartmentApi(){ return getRetrofitInstance().create(ApartmentApi.class); }
    public static TicketApi getTicketApi(){ return getRetrofitInstance().create(TicketApi.class); }
    public static ChoreAPI getChoreApi(){ return getRetrofitInstance().create(ChoreAPI.class); }
    public static CalendarApi getCalendarApi(){ return getRetrofitInstance().create(CalendarApi.class); }
    public static BillsApi getBillsApi(){ return getRetrofitInstance().create(BillsApi.class); }
    public static UserApi getUserApi() { return getRetrofitInstance().create(UserApi.class); }
}