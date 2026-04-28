package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.CalendarApi;
import com.example.uninest.model.Calendar;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CalendarShowAllByApartmentActivity extends AppCompatActivity {

    private LinearLayout calendarContainer;
    private CalendarApi calendarApi;
    private String houseCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar);

        houseCode = getIntent().getStringExtra("EXTRA_HOUSE_CODE");

        calendarContainer = findViewById(R.id.layoutCalendarList);
        TextView tvTitle = findViewById(R.id.tvCalendarTitle);
        tvTitle.setText("Calendar Events for " + houseCode);

        calendarApi = ApiClient.getCalendarApi();

        loadCalendarEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCalendarEvents();
    }


    private void loadCalendarEvents() {
        calendarContainer.removeAllViews();

        calendarApi.getByApartment(houseCode).enqueue(new Callback<List<Calendar>>() {
            @Override
            public void onResponse(Call<List<Calendar>> call, Response<List<Calendar>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Calendar> events = response.body();

                    // DEBUG LOG: See if the list contains the maintenance items
                    Log.d("CalendarCheck", "Items received: " + events.size());

                    for (Calendar event : events) {
                        if (event != null) {
                            displayEventCard(event);
                        }
                    }
                } else {
                    Toast.makeText(CalendarShowAllByApartmentActivity.this, "Server Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Calendar>> call, Throwable t) {
                Log.e("CalendarCheck", "Network Error", t);
            }
        });
    }

    private void displayEventCard(Calendar event) {
        // Inflate a simple layout for each calendar event
        View cardView = getLayoutInflater().inflate(R.layout.item_calendar_card, calendarContainer, false);

        TextView tvTitle = cardView.findViewById(R.id.tvEventTitle);
        TextView tvTime = cardView.findViewById(R.id.tvEventTime);
        TextView tvDetails = cardView.findViewById(R.id.tvEventDetails);


        tvTitle.setText(event.getTitle());

        String start = event.getStartDate() != null ? event.getStartDate().toDate().toString() : "N/A";
        String end = event.getEndDate() != null ? event.getEndDate().toDate().toString() : "N/A";
        tvTime.setText(start + " - " + end);


        calendarContainer.addView(cardView);
    }
}
