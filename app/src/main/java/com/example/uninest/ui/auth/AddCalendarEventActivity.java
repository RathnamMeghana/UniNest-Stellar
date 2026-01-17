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

    // TODO: Pass these dynamically from SessionManager/Firebase Auth
    private String houseCode = "APT-E2DE614";
    private String createdBy = "PSXFJ5KPNyXqrJZFTGAD7OmmwqX2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_calendar_event);

        // Initialize Views
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etAssignedTo = findViewById(R.id.etAssignedTo);
        choreFieldsLayout = findViewById(R.id.choreFieldsLayout);
        etRoom = findViewById(R.id.etRoom);
        etDuration = findViewById(R.id.etDuration);
        etDifficulty = findViewById(R.id.etDifficulty);
        etFrequency = findViewById(R.id.etFrequency);
        typeSpinner = findViewById(R.id.typeSpinner);
        btnSave = findViewById(R.id.btnSaveCalendar);

        // Setup Spinner
        String[] spinnerValues = {
                "CHORE (Smart Assign)",
                "CHORE (Manual Assign)",
                "MOVE_OUT",
                "MOVE_IN",
                "BILL_DUE",
                "MAINTENANCE",
                "OTHER",
                "CUSTOM"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerValues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);

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

        btnSave.setOnClickListener(v -> {
            btnSave.setEnabled(false);
            saveEvent();
        });
    }

    private void saveEvent() {
        String selectedType = typeSpinner.getSelectedItem().toString();
        boolean isChore = selectedType.startsWith("CHORE");
        boolean isManualAssign = selectedType.equals("CHORE (Manual Assign)");

        Date start = new Date();
        Date end = new Date(start.getTime() + 60 * 60 * 1000); // Default 1 hour duration

        if (isChore) {
            final String roomInput = etRoom.getText().toString().trim().isEmpty() ? "General" : etRoom.getText().toString().trim();
            final Chore chore = new Chore();
            chore.setHouseCode(houseCode);
            chore.setTaskName(etTitle.getText().toString().trim());
            chore.setRoom(roomInput);
            chore.setEstDurationMin(safeParseInt(etDuration.getText().toString(), 30));
            chore.setDifficultyScore(safeParseInt(etDifficulty.getText().toString(), 1));
            chore.setFrequencyPerWeek(safeParseInt(etFrequency.getText().toString(), 1));

            if (isManualAssign) {
                String assignedEmail = etAssignedTo.getText().toString().trim();
                if (assignedEmail.isEmpty()) {
                    Toast.makeText(this, "Please enter assigned email", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    return;
                }
                // Backend creates both Chore and Calendar event
                ApiClient.getChoreApi().addWithAssignment(houseCode, assignedEmail, chore)
                        .enqueue(new ChoreCallback());
            } else {
                // Smart assign: Backend creates both Chore and Calendar event
                ApiClient.getChoreApi().addWithSmartAssign(houseCode, chore)
                        .enqueue(new ChoreCallback());
            }
        } else {
            // Non-CHORE events: Backend DOES NOT auto-create these, so we call Calendar API directly
            String assignedToEmail = etAssignedTo.getText().toString().trim();
            if (assignedToEmail.isEmpty()) assignedToEmail = createdBy;
            createCalendarEvent(selectedType, start, end, assignedToEmail, null);
        }
    }

    /**
     * Specialized Callback for Chore creation.
     * We do NOT call createCalendarEventFromChore here because the
     * Backend Service already handles that logic.
     */
    private class ChoreCallback implements Callback<Chore> {
        @Override
        public void onResponse(Call<Chore> call, Response<Chore> response) {
            btnSave.setEnabled(true);
            if (response.isSuccessful()) {
                Toast.makeText(AddCalendarEventActivity.this, "Chore and Calendar event added!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(AddCalendarEventActivity.this, "Server error: Failed to create chore", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onFailure(Call<Chore> call, Throwable t) {
            btnSave.setEnabled(true);
            Toast.makeText(AddCalendarEventActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Direct call to Calendar API for non-chore events (Bills, Move-in, etc.)
     */
    private void createCalendarEvent(String type, Date start, Date end, String assignedTo, String relatedChoreId) {
        CalendarRequest req = new CalendarRequest();
        req.setHouseCode(houseCode);
        req.setTitle(etTitle.getText().toString().trim());
        req.setDescription(etDescription.getText().toString().trim());
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
                        btnSave.setEnabled(true);
                        if (response.isSuccessful()) {
                            Toast.makeText(AddCalendarEventActivity.this, "Event added to calendar!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AddCalendarEventActivity.this, "Error creating calendar event", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Calendar> call, Throwable t) {
                        btnSave.setEnabled(true);
                        Toast.makeText(AddCalendarEventActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private int safeParseInt(String str, int defaultValue) {
        if (str == null || str.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}