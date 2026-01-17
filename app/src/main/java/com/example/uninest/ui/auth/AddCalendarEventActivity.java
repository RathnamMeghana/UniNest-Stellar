package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.CalendarRequest;
import com.example.uninest.model.Calendar;
import com.example.uninest.model.DateUtils;

import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddCalendarEventActivity extends AppCompatActivity {

    private EditText etTitle, etDescription, etAssignedTo;
    private Spinner typeSpinner;
    private Button btnSave;

    private String houseCode = "APT-E2DE614";   // pass by Intent later
    private String createdBy = "USER_ID_HERE";  // Firebase UID later

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_calendar_event);

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etAssignedTo = findViewById(R.id.etAssignedTo);
        typeSpinner = findViewById(R.id.typeSpinner);
        btnSave = findViewById(R.id.btnSaveCalendar);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{ "CHORE", "MOVE_OUT", "MOVE_IN", "BILL_DUE", "MAINTENANCE", "OTHER", "CUSTOM"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);

        btnSave.setOnClickListener(v -> saveCalendarEvent());
    }

    private void saveCalendarEvent() {

        CalendarRequest req = new CalendarRequest();

        req.setHouseCode(houseCode);
        req.setTitle(etTitle.getText().toString());
        req.setDescription(etDescription.getText().toString());
        req.setType(typeSpinner.getSelectedItem().toString());
        req.setCreatedBy(createdBy);
        req.setAssignedTo(etAssignedTo.getText().toString());
        req.setAllDay(false);

        // Default time: now → +1 hour
        Date start = new Date();
        Date end = new Date(start.getTime() + 60 * 60 * 1000);

        req.setStartSeconds(DateUtils.toSeconds(start));
        req.setEndSeconds(DateUtils.toSeconds(end));


        req.setRelatedChoreId(null);

        //need to add other field for chores including est time and room

        ApiClient.getCalendarApi()
                .createEvent(req, createdBy)
                .enqueue(new Callback<Calendar>() {

                    @Override
                    public void onResponse(Call<Calendar> call, Response<Calendar> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(
                                    AddCalendarEventActivity.this,
                                    "Calendar event added!",
                                    Toast.LENGTH_SHORT
                            ).show();
                            finish();
                        } else {
                            Toast.makeText(
                                    AddCalendarEventActivity.this,
                                    "Error creating event",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Calendar> call, Throwable t) {
                        Toast.makeText(
                                AddCalendarEventActivity.this,
                                "Network error",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
}
