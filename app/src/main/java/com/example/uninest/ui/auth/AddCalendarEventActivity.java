package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.Calendar;
import com.example.uninest.model.CalendarRequest;
import com.example.uninest.model.Chore;
import com.example.uninest.model.DateUtils;

import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddCalendarEventActivity extends AppCompatActivity {

    private EditText etTitle, etDescription, etAssignedTo, etRoom, etDuration, etDifficulty, etFrequency;
    private Spinner typeSpinner;
    private Button btnSave;

    private LinearLayout choreFieldsLayout;

    private String houseCode = "APT-E2DE614";       // TODO: pass dynamically
    private String createdBy = "PSXFJ5KPNyXqrJZFTGAD7OmmwqX2"; // TODO: pass Firebase UID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_calendar_event);

        // Main fields
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etAssignedTo = findViewById(R.id.etAssignedTo);

        // CHORE fields
        choreFieldsLayout = findViewById(R.id.choreFieldsLayout);
        etRoom = findViewById(R.id.etRoom);
        etDuration = findViewById(R.id.etDuration);
        etDifficulty = findViewById(R.id.etDifficulty);
        etFrequency = findViewById(R.id.etFrequency);

        typeSpinner = findViewById(R.id.typeSpinner);
        btnSave = findViewById(R.id.btnSaveCalendar);

        // Spinner values with both CHORE options
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{
                        "CHORE (Smart Assign)",
                        "CHORE (Manual Assign)",
                        "MOVE_OUT",
                        "MOVE_IN",
                        "BILL_DUE",
                        "MAINTENANCE",
                        "OTHER",
                        "CUSTOM"
                }
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);

        // Show/hide CHORE fields dynamically
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = typeSpinner.getSelectedItem().toString();
                boolean isChore = selected.startsWith("CHORE");
                choreFieldsLayout.setVisibility(isChore ? View.VISIBLE : View.GONE);
                etAssignedTo.setEnabled(selected.equals("CHORE (Manual Assign)"));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnSave.setOnClickListener(v -> saveEvent());
    }

    private void saveEvent() {
        String selectedType = typeSpinner.getSelectedItem().toString();
        boolean isSmartAssign = selectedType.equals("CHORE (Smart Assign)");
        boolean isManualAssign = selectedType.equals("CHORE (Manual Assign)");
        boolean isChore = selectedType.startsWith("CHORE");

        // Default start/end times
        Date start = new Date();
        Date end = new Date(start.getTime() + 60 * 60 * 1000);

        if (isChore) {
            Chore chore = new Chore();
            chore.setHouseCode(houseCode);
            chore.setTaskName(etTitle.getText().toString());
            chore.setRoom(etRoom.getText().toString().isEmpty() ? "General" : etRoom.getText().toString());
            chore.setEstDurationMin(etDuration.getText().toString().isEmpty() ? 30 : Integer.parseInt(etDuration.getText().toString()));
            chore.setDifficultyScore(etDifficulty.getText().toString().isEmpty() ? 1 : Integer.parseInt(etDifficulty.getText().toString()));
            chore.setFrequencyPerWeek(etFrequency.getText().toString().isEmpty() ? 1 : Integer.parseInt(etFrequency.getText().toString()));

            if (isSmartAssign) {
                // Smart assign
                ApiClient.getChoreApi().addWithSmartAssign(houseCode, chore)
                        .enqueue(new Callback<Chore>() {
                            @Override
                            public void onResponse(Call<Chore> call, Response<Chore> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    Chore createdChore = response.body();
                                    createCalendarEvent("CHORE", start, end, createdChore.getAssignedTo(), createdChore.getId());
                                } else {
                                    Toast.makeText(AddCalendarEventActivity.this, "Failed to create chore", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<Chore> call, Throwable t) {
                                Toast.makeText(AddCalendarEventActivity.this, "Network error while creating chore", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                // Manual assign
                String assignedEmail = etAssignedTo.getText().toString().trim();
                if (assignedEmail.isEmpty()) {
                    Toast.makeText(this, "Please enter assigned email", Toast.LENGTH_SHORT).show();
                    return;
                }
                ApiClient.getChoreApi().addWithAssignment(houseCode, assignedEmail, chore)
                        .enqueue(new Callback<Chore>() {
                            @Override
                            public void onResponse(Call<Chore> call, Response<Chore> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    Chore createdChore = response.body();
                                    createCalendarEvent("CHORE", start, end, createdChore.getAssignedTo(), createdChore.getId());
                                } else {
                                    Toast.makeText(AddCalendarEventActivity.this, "Failed to create chore", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<Chore> call, Throwable t) {
                                Toast.makeText(AddCalendarEventActivity.this, "Network error while creating chore", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        } else {
            // Non-CHORE event
            String assignedToEmail = etAssignedTo.getText().toString().trim();
            if (assignedToEmail.isEmpty()) assignedToEmail = createdBy;
            createCalendarEvent(selectedType, start, end, assignedToEmail, null);
        }
    }

    private void createCalendarEvent(String type, Date start, Date end, String assignedTo, String relatedChoreId) {
        CalendarRequest req = new CalendarRequest();
        req.setHouseCode(houseCode);
        req.setTitle(etTitle.getText().toString());
        req.setDescription(etDescription.getText().toString());
        req.setType(type);
        req.setCreatedBy(createdBy);
        req.setAssignedTo(assignedTo);
        req.setAllDay(false);
        req.setStartSeconds(DateUtils.toSeconds(start));
        req.setEndSeconds(DateUtils.toSeconds(end));
        req.setRelatedChoreId(relatedChoreId);

        ApiClient.getCalendarApi().createEvent(req, createdBy)
                .enqueue(new Callback<Calendar>() {
                    @Override
                    public void onResponse(Call<Calendar> call, Response<Calendar> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(AddCalendarEventActivity.this, "Calendar event added!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AddCalendarEventActivity.this, "Error creating calendar event", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Calendar> call, Throwable t) {
                        Toast.makeText(AddCalendarEventActivity.this, "Network error while creating calendar event", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
