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
    private String houseCode = "APT-E2DE614"; // Your specific house code

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendar);

        calendarContainer = findViewById(R.id.layoutCalendarList);
        TextView tvTitle = findViewById(R.id.tvCalendarTitle);
        tvTitle.setText("Calendar Events for " + houseCode);

        calendarApi = ApiClient.getCalendarApi();

        loadCalendarEvents();
    }

    private void loadCalendarEvents() {
        calendarContainer.removeAllViews();

        calendarApi.getByApartment(houseCode).enqueue(new Callback<List<Calendar>>() {
            @Override
            public void onResponse(Call<List<Calendar>> call, Response<List<Calendar>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Calendar> events = response.body();

                    if (events.isEmpty()) {
                        Toast.makeText(CalendarShowAllByApartmentActivity.this, "No events found", Toast.LENGTH_SHORT).show();
                    }

                    for (Calendar event : events) {
                        displayEventCard(event);
                    }
                } else {
                    Toast.makeText(CalendarShowAllByApartmentActivity.this, "Failed to load events", Toast.LENGTH_SHORT).show();
                    Log.e("CalendarAPI", "Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Calendar>> call, Throwable t) {
                Log.e("CalendarAPI", "Error: " + t.getMessage());
                Toast.makeText(CalendarShowAllByApartmentActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
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
