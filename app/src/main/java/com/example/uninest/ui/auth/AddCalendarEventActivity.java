package com.example.uninest.ui.auth;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.SessionManager;
import com.example.uninest.model.FirestoreTimestamp;

import com.example.uninest.R;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.Calendar;
import com.example.uninest.model.CalendarRequest;
import com.example.uninest.model.Chore;
import com.example.uninest.model.DateUtils;
import com.example.uninest.data.api.UserApi;
import com.example.uninest.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddCalendarEventActivity extends AppCompatActivity {

    // Inputs
    private Spinner spinnerCategory, spinnerAssignType, spinnerFrequency, spinnerReminderType;
    private Spinner spinnerAssignedTo;
    private EditText etTitle, etDescription, etRoom, etDuration, etDifficulty, etAmount;
    private TextView tvSelectDate, tvSelectTime;
    private CheckBox cbAllDay;
    private Button btnSave;

    // Containers
    private LinearLayout containerChore, containerEvent, containerReminder;
    private LinearLayout containerAssignTo, containerAmount;

    // Data
    private final java.util.Calendar selectedCal = java.util.Calendar.getInstance();
    // Dynamic Data
    private SessionManager sessionManager;
    private String houseCode;
    private String currentUserId;
    private UserApi userApi;
    private List<User> roommateList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_calendar_event);

        // Get Session Data
        sessionManager = new SessionManager(this);
        houseCode = sessionManager.fetchHouseCode();
        currentUserId = sessionManager.getUserId();

        if (houseCode == null || currentUserId == null) {
            Toast.makeText(this, "Session Error. Re-login required.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userApi = ApiClient.getUserApi();

        initViews();
        setupSpinners();
        setupPickers();

        btnSave.setOnClickListener(v -> handleSave());

        loadRoommates();
    }

    private void initViews() {
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerAssignType = findViewById(R.id.spinnerAssignType);
        spinnerFrequency = findViewById(R.id.spinnerFrequency);
        spinnerReminderType = findViewById(R.id.spinnerReminderType);

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        spinnerAssignedTo = findViewById(R.id.spinnerAssignedTo);
        etRoom = findViewById(R.id.etRoom);
        etDuration = findViewById(R.id.etDuration);
        etDifficulty = findViewById(R.id.etDifficulty);
        etAmount = findViewById(R.id.etAmount); // Only for bills

        tvSelectDate = findViewById(R.id.tvSelectDate);
        tvSelectTime = findViewById(R.id.tvSelectTime);
        cbAllDay = findViewById(R.id.cbAllDay);
        btnSave = findViewById(R.id.btnSaveCalendar);

        containerChore = findViewById(R.id.containerChore);
        containerEvent = findViewById(R.id.containerEvent);
        containerReminder = findViewById(R.id.containerReminder);
        containerAssignTo = findViewById(R.id.containerAssignTo);
        containerAmount = findViewById(R.id.containerAmount);
    }

    private void loadRoommates() {
        userApi.getRoommates(houseCode).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    roommateList = response.body();

                    for (User u : roommateList) {
                        // Check if this user is the logged-in user
                        if (u.getId() != null && u.getId().equals(currentUserId)) {
                            u.setFirstName("Me");
                            u.setLastName("");
                        }
                    }

                    // Create Adapter
                    ArrayAdapter<User> adapter = new ArrayAdapter<>(
                            AddCalendarEventActivity.this,
                            android.R.layout.simple_spinner_item,
                            roommateList
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerAssignedTo.setAdapter(adapter);
                }
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(AddCalendarEventActivity.this, "Failed to load roommates", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSpinners() {
        // 1. Master Category
        String[] cats = {"Chore", "Event", "Reminder/Bill"};
        setAdapter(spinnerCategory, cats);

        // 2. Chore Assignment
        String[] assignTypes = {"Smart Assign (AI)", "Manual Assign"};
        setAdapter(spinnerAssignType, assignTypes);

        // 3. Frequency
        String[] freqs = {"One Time", "Weekly", "Monthly"};
        setAdapter(spinnerFrequency, freqs);

        // 4. Reminder Types
        String[] reminders = {"Bill Due", "Maintenance", "General Reminder"};
        setAdapter(spinnerReminderType, reminders);

        // --- LISTENER TO SWITCH LAYOUTS ---
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = cats[position];
                containerChore.setVisibility(selected.equals("Chore") ? View.VISIBLE : View.GONE);
                containerEvent.setVisibility(selected.equals("Event") ? View.VISIBLE : View.GONE);
                containerReminder.setVisibility(selected.equals("Reminder/Bill") ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Toggle "Assign To" based on Assign Type
        spinnerAssignType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = assignTypes[position];
                containerAssignTo.setVisibility(selected.equals("Manual Assign") ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Toggle "Amount" based on Reminder Type
        spinnerReminderType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = reminders[position];
                containerAmount.setVisibility(selected.equals("Bill Due") ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupPickers() {
        updateDateLabel();
        tvSelectDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, day) -> {
                selectedCal.set(java.util.Calendar.YEAR, year);
                selectedCal.set(java.util.Calendar.MONTH, month);
                selectedCal.set(java.util.Calendar.DAY_OF_MONTH, day);
                updateDateLabel();
            },
                    selectedCal.get(java.util.Calendar.YEAR),
                    selectedCal.get(java.util.Calendar.MONTH),
                    selectedCal.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        tvSelectTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, minute) -> {
                selectedCal.set(java.util.Calendar.HOUR_OF_DAY, hour);
                selectedCal.set(java.util.Calendar.MINUTE, minute);
                tvSelectTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
            }, 12, 0, true).show();
        });
    }

    private void handleSave() {
        btnSave.setEnabled(false);
        String cat = spinnerCategory.getSelectedItem().toString();

        if (cat.equals("Chore")) {
            saveChore();
        } else if (cat.equals("Event")) {
            saveEventOrReminder("EVENT");
        } else {
            // Check specific reminder type
            String subType = spinnerReminderType.getSelectedItem().toString();
            String backendType = "REMINDER";
            if(subType.contains("Bill")) backendType = "BILL_DUE";
            if(subType.contains("Maintenance")) backendType = "MAINTENANCE";

            saveEventOrReminder(backendType);
        }
    }

    // --- LOGIC 1: SAVING CHORES (Green) ---
    private void saveChore() {
        Chore chore = new Chore();
        chore.setHouseCode(houseCode);
        chore.setCreatedBy(currentUserId);
        chore.setTaskName(etTitle.getText().toString().trim());
        chore.setRoom(etRoom.getText().toString().trim());
        chore.setDescription(etDescription.getText().toString().trim());

        // Safety parsing
        chore.setEstDurationMin(parseInt(etDuration.getText().toString(), 30));
        chore.setDifficultyScore(parseInt(etDifficulty.getText().toString(), 1));

        // Frequency Logic
        String freqStr = spinnerFrequency.getSelectedItem().toString();
        int freq = 0; // Once
        if(freqStr.equals("Weekly")) freq = 1;
        if(freqStr.equals("Monthly")) freq = 4; // Or handle differently in backend
        chore.setFrequencyPerWeek(freq);

        // Date Logic (ISO String for Backend)
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        chore.setScheduledDate(iso.format(selectedCal.getTime()));

        String assignType = spinnerAssignType.getSelectedItem().toString();

        if (assignType.contains("Manual")) {
            User selectedUser = (User) spinnerAssignedTo.getSelectedItem();
            if (selectedUser == null) {
                Toast.makeText(this, "Please select a roommate", Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
                return;
            }
            String email = selectedUser.getEmail();

            ApiClient.getChoreApi().addWithAssignment(houseCode, email, chore).enqueue(choreCallback);
        } else {
            // Smart assign
            ApiClient.getChoreApi().addWithSmartAssign(houseCode, chore).enqueue(choreCallback);
        }
    }


    // --- LOGIC 2: SAVING EVENTS & REMINDERS (Pink/Orange) ---
    private void saveEventOrReminder(String type) {
        CalendarRequest req = new CalendarRequest();
        req.setHouseCode(houseCode);
        req.setType(type); // "EVENT", "BILL_DUE", "MAINTENANCE"
        req.setTitle(etTitle.getText().toString().trim());
        req.setDescription(etDescription.getText().toString().trim());
        req.setCreatedBy(currentUserId);

        if (type.equals("BILL_DUE")) {
            try {
                double amt = Double.parseDouble(etAmount.getText().toString());
                req.setAmount(amt);
            } catch (NumberFormatException e) {
                req.setAmount(0.0);
            }
        }

        // Date Logic
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

        // Start
        req.setStartDate(iso.format(selectedCal.getTime()));

        // End (Start + 1 hour)
        long endMillis = selectedCal.getTimeInMillis() + 3600000;
        req.setEndDate(iso.format(new Date(endMillis)));


        req.setAllDay(cbAllDay.isChecked());

        req.setAssignedTo(currentUserId);

        ApiClient.getCalendarApi().createEvent(req, currentUserId).enqueue(new Callback<Calendar>() {
            @Override
            public void onResponse(Call<Calendar> call, Response<Calendar> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddCalendarEventActivity.this, "Saved!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddCalendarEventActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                }
            }
            @Override
            public void onFailure(Call<Calendar> call, Throwable t) {
                Toast.makeText(AddCalendarEventActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
            }
        });
    }

    // --- HELPERS ---
    private final Callback<Chore> choreCallback = new Callback<Chore>() {
        @Override
        public void onResponse(Call<Chore> call, Response<Chore> response) {
            if (response.isSuccessful()) {
                Toast.makeText(AddCalendarEventActivity.this, "Chore Added!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(AddCalendarEventActivity.this, "Chore Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                btnSave.setEnabled(true);
            }
        }
        @Override
        public void onFailure(Call<Chore> call, Throwable t) {
            Toast.makeText(AddCalendarEventActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(true);
        }
    };

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
        tvSelectDate.setText(sdf.format(selectedCal.getTime()));
    }

    private void setAdapter(Spinner s, String[] data) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, data);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
    }

    private int parseInt(String val, int def) {
        try { return Integer.parseInt(val); } catch (Exception e) { return def; }
    }
}