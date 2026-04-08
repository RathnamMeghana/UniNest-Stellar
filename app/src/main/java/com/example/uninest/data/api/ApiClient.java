package com.example.uninest.data.api;


import com.example.uninest.AuthInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.example.uninest.data.api.NotificationApi;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import io.sentry.android.okhttp.SentryOkHttpInterceptor;

public class ApiClient {

    //private static final String BASE_URL = "http://192.168.1.89:8080/";

    // Make sure this is just the IP and Port

    //public static final String BASE_URL = "http://192.168.1.90:8080/";

    public static final String BASE_URL = "https://uninest-backend-369284273825.europe-west1.run.app/";
    private static Retrofit retrofit;

    private static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            // Logging interceptor
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Auth interceptor
            AuthInterceptor authInterceptor = new AuthInterceptor();

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                   // .addInterceptor(new SentryOkHttpInterceptor())
                    .addInterceptor(logging)
                    .build();

            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }


    private static NotificationApi notificationApi;


    // --- API Services ---
    public static BuildingApi getBuildingApi() { return getRetrofitInstance().create(BuildingApi.class); }
    public static ApartmentApi getApartmentApi(){ return getRetrofitInstance().create(ApartmentApi.class); }
    public static TicketApi getTicketApi(){ return getRetrofitInstance().create(TicketApi.class); }
    public static ChoreAPI getChoreApi(){ return getRetrofitInstance().create(ChoreAPI.class); }
    public static CalendarApi getCalendarApi(){ return getRetrofitInstance().create(CalendarApi.class); }
    public static BillsApi getBillsApi(){ return getRetrofitInstance().create(BillsApi.class); }
    public static UserApi getUserApi() { return getRetrofitInstance().create(UserApi.class); }
    public static NotificationApi getNotificationApi() {return getRetrofitInstance().create(NotificationApi.class);}
    }

