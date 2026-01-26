package com.example.uninest.ui.auth;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.SessionManager;
import com.example.uninest.R;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.BillsRequest;
import com.example.uninest.model.Calendar;
import com.example.uninest.model.CalendarRequest;
import com.example.uninest.model.Chore;
import com.example.uninest.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddCalendarEventActivity extends AppCompatActivity {

    // Common UI Components
    private Spinner spinnerCategory, spinnerFrequency, spinnerReminderType;
    private EditText etTitle, etDescription, etAmount;
    private TextView tvSelectDate, tvSelectTime, tvSplitAmount, tvFrequencyLabel;
    private Button btnSave;
    private CheckBox cbAllDay;

    // Chore Specific Components
    private Spinner spinnerAssignType, spinnerAssignedTo;
    private EditText etRoom, etDuration, etDifficulty;
    private LinearLayout containerChore, containerEvent, containerReminder, containerBillSplitting;

    // Bill/Split Specific Components
    private RecyclerView rvRoommates;
    private RoommateSplitAdapter roommateAdapter;
    private List<String> selectedRoommateIds = new ArrayList<>();

    // Data & State
    private final java.util.Calendar selectedCal = java.util.Calendar.getInstance();
    private SessionManager sessionManager;
    private String houseCode;
    private String currentUserId;
    private List<User> roommateList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_calendar_event);

        // 1. Initialize Session & State
        sessionManager = new SessionManager(this);
        houseCode = sessionManager.fetchHouseCode();
        currentUserId = sessionManager.getUserId();

        if (houseCode == null || currentUserId == null) {
            Toast.makeText(this, "Session Error. Re-login required.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Setup UI
        initViews();
        setupSpinners();
        setupPickers();
        setupRecyclerView();
        setupAmountWatcher();
        loadRoommates();

        // 3. Save Trigger
        btnSave.setOnClickListener(v -> handleSave());
    }

    private void initViews() {
        // Main Form
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerFrequency = findViewById(R.id.spinnerFrequency);
        spinnerReminderType = findViewById(R.id.spinnerReminderType);
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etAmount = findViewById(R.id.etAmount);
        tvSelectDate = findViewById(R.id.tvSelectDate);
        tvSelectTime = findViewById(R.id.tvSelectTime);
        tvFrequencyLabel = findViewById(R.id.tvFrequencyLabel);
        cbAllDay = findViewById(R.id.cbAllDay);
        btnSave = findViewById(R.id.btnSaveCalendar);

        // Containers
        containerChore = findViewById(R.id.containerChore);
        containerEvent = findViewById(R.id.containerEvent);
        containerReminder = findViewById(R.id.containerReminder);
        containerBillSplitting = findViewById(R.id.containerBillSplitting);

        // Chore Specific
        spinnerAssignType = findViewById(R.id.spinnerAssignType);
        spinnerAssignedTo = findViewById(R.id.spinnerAssignedTo);
        etRoom = findViewById(R.id.etRoom);
        etDuration = findViewById(R.id.etDuration);
        etDifficulty = findViewById(R.id.etDifficulty);

        // Bill Specific
        rvRoommates = findViewById(R.id.rvRoommates);
        tvSplitAmount = findViewById(R.id.tvSplitAmount);
    }

    private void setupSpinners() {
        setAdapter(spinnerCategory, new String[]{"Chore", "Event", "Reminder/Bill"});
        setAdapter(spinnerAssignType, new String[]{"Smart Assign (AI)", "Manual Assign"});
        setAdapter(spinnerFrequency, new String[]{"One Time", "Weekly", "Monthly"});
        setAdapter(spinnerReminderType, new String[]{"Bill Due", "Maintenance", "General Reminder"});

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = (String) parent.getItemAtPosition(position);
                containerChore.setVisibility(selected.equals("Chore") ? View.VISIBLE : View.GONE);
                containerEvent.setVisibility(selected.equals("Event") ? View.VISIBLE : View.GONE);
                containerReminder.setVisibility(selected.equals("Reminder/Bill") ? View.VISIBLE : View.GONE);
                updateVisibilityLogic();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerReminderType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateVisibilityLogic();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateVisibilityLogic() {
        String cat = spinnerCategory.getSelectedItem().toString();
        String subType = spinnerReminderType.getSelectedItem().toString();

        boolean isBill = cat.equals("Reminder/Bill") && subType.equals("Bill Due");

        // Show Splitting UI if it's a bill
        containerBillSplitting.setVisibility(isBill ? View.VISIBLE : View.GONE);

        // Show Frequency if it's a Chore OR a Bill
        if (isBill || cat.equals("Chore")) {
            tvFrequencyLabel.setVisibility(View.VISIBLE);
            spinnerFrequency.setVisibility(View.VISIBLE);
        } else {
            tvFrequencyLabel.setVisibility(View.GONE);
            spinnerFrequency.setVisibility(View.GONE);
        }
    }

    private void handleSave() {
        btnSave.setEnabled(false);
        String cat = spinnerCategory.getSelectedItem().toString();

        if (cat.equals("Chore")) {
            saveChore();
        } else if (cat.equals("Event")) {
            saveEventOrReminder("EVENT");
        } else {
            String subType = spinnerReminderType.getSelectedItem().toString();
            if (subType.equals("Bill Due")) {
                saveBillWithCalendar();
            } else {
                saveEventOrReminder(subType.toUpperCase().replace(" ", "_"));
            }
        }
    }

    private void saveBillWithCalendar() {
        String title = etTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String freqStr = spinnerFrequency.getSelectedItem().toString();

        if (title.isEmpty() || amountStr.isEmpty() || selectedRoommateIds.isEmpty()) {
            Toast.makeText(this, "Check Title, Amount, and Roommates", Toast.LENGTH_SHORT).show();
            btnSave.setEnabled(true);
            return;
        }

        double total = Double.parseDouble(amountStr);
        double perPerson = total / selectedRoommateIds.size();

        BillsRequest billReq = new BillsRequest();
        billReq.setTitle(title);
        billReq.setTotalAmount(total);
        billReq.setCreatorId(currentUserId);
        billReq.setDueDate(selectedCal.getTime());
        billReq.setHouseCode(houseCode);
        billReq.setRoommateIds(selectedRoommateIds);
        billReq.setActive(true);

        // Recurring Logic
        if (freqStr.equals("One Time")) {
            billReq.setBillType(BillsRequest.BillType.ONE_TIME);
        } else {
            billReq.setBillType(BillsRequest.BillType.RECURRING);
            billReq.setStartDate(new Date());
            if (freqStr.equals("Weekly")) billReq.setFrequency(BillsRequest.BillFrequency.WEEKLY);
            else if (freqStr.equals("Monthly")) billReq.setFrequency(BillsRequest.BillFrequency.MONTHLY);
        }

        List<BillsRequest.Split> splits = new ArrayList<>();
        for (String uid : selectedRoommateIds) {
            BillsRequest.Split split = new BillsRequest.Split();
            split.setUserId(uid);
            split.setAmountOwed(perPerson);
            split.setPaid(false);
            splits.add(split);
        }
        billReq.setSplits(splits);

        ApiClient.getBillsApi().createBill(billReq).enqueue(new Callback<List<BillsRequest>>() {
            @Override
            public void onResponse(Call<List<BillsRequest>> call, Response<List<BillsRequest>> response) {
                if (response.isSuccessful()) {
                    saveEventOrReminder("BILL_DUE");
                } else {
                    Toast.makeText(AddCalendarEventActivity.this, "Bill Error Code: " + response.code(), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                }
            }
            @Override public void onFailure(Call<List<BillsRequest>> call, Throwable t) {
                btnSave.setEnabled(true);
            }
        });
    }

    private void saveChore() {
        Chore chore = new Chore();
        chore.setHouseCode(houseCode);
        chore.setCreatedBy(currentUserId);
        chore.setTaskName(etTitle.getText().toString().trim());
        chore.setRoom(etRoom.getText().toString().trim());
        chore.setDescription(etDescription.getText().toString().trim());
        chore.setEstDurationMin(parseInt(etDuration.getText().toString(), 30));
        chore.setDifficultyScore(parseInt(etDifficulty.getText().toString(), 1));

        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        chore.setScheduledDate(iso.format(selectedCal.getTime()));

        String assignType = spinnerAssignType.getSelectedItem().toString();
        if (assignType.contains("Manual")) {
            User selectedUser = (User) spinnerAssignedTo.getSelectedItem();
            if (selectedUser != null) {
                ApiClient.getChoreApi().addWithAssignment(houseCode, selectedUser.getEmail(), chore).enqueue(choreCallback);
            }
        } else {
            ApiClient.getChoreApi().addWithSmartAssign(houseCode, chore).enqueue(choreCallback);
        }
    }

    private void saveEventOrReminder(String type) {
        CalendarRequest req = new CalendarRequest();
        req.setHouseCode(houseCode);
        req.setType(type);
        req.setTitle(etTitle.getText().toString().trim());
        req.setDescription(etDescription.getText().toString().trim());
        req.setCreatedBy(currentUserId);

        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        req.setStartDate(iso.format(selectedCal.getTime()));
        req.setEndDate(iso.format(new Date(selectedCal.getTimeInMillis() + 3600000)));
        req.setAllDay(cbAllDay.isChecked());
        req.setAssignedTo(currentUserId);

        ApiClient.getCalendarApi().createEvent(req, currentUserId).enqueue(new Callback<Calendar>() {
            @Override
            public void onResponse(Call<Calendar> call, Response<Calendar> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddCalendarEventActivity.this, "Success!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    btnSave.setEnabled(true);
                }
            }
            @Override public void onFailure(Call<Calendar> call, Throwable t) { btnSave.setEnabled(true); }
        });
    }

    // --- Helpers & UI setup ---

    private void setupRecyclerView() {
        rvRoommates.setLayoutManager(new LinearLayoutManager(this));
        roommateAdapter = new RoommateSplitAdapter(new ArrayList<>(), (userId, isChecked) -> {
            if (isChecked) {
                if (!selectedRoommateIds.contains(userId)) selectedRoommateIds.add(userId);
            } else {
                selectedRoommateIds.remove(userId);
            }
            updateSplitDisplay();
        });
        rvRoommates.setAdapter(roommateAdapter);
    }

    private void loadRoommates() {
        ApiClient.getUserApi().getRoommates(houseCode).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    roommateList = response.body();
                    roommateAdapter.updateList(roommateList);
                    ArrayAdapter<User> adapter = new ArrayAdapter<>(AddCalendarEventActivity.this, android.R.layout.simple_spinner_item, roommateList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerAssignedTo.setAdapter(adapter);
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) {}
        });
    }

    private void setupAmountWatcher() {
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateSplitDisplay(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void updateSplitDisplay() {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty() || selectedRoommateIds.isEmpty()) {
            tvSplitAmount.setText("Each pays: €0.00");
            return;
        }
        try {
            double total = Double.parseDouble(amountStr);
            double split = total / selectedRoommateIds.size();
            tvSplitAmount.setText(String.format(Locale.getDefault(), "Each pays: €%.2f", split));
        } catch (NumberFormatException e) {
            tvSplitAmount.setText("Each pays: €0.00");
        }
    }

    private void setupPickers() {
        updateDateLabel();
        tvSelectDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, day) -> {
                selectedCal.set(java.util.Calendar.YEAR, year);
                selectedCal.set(java.util.Calendar.MONTH, month);
                selectedCal.set(java.util.Calendar.DAY_OF_MONTH, day);
                updateDateLabel();
            }, selectedCal.get(java.util.Calendar.YEAR), selectedCal.get(java.util.Calendar.MONTH), selectedCal.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        tvSelectTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, minute) -> {
                selectedCal.set(java.util.Calendar.HOUR_OF_DAY, hour);
                selectedCal.set(java.util.Calendar.MINUTE, minute);
                tvSelectTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
            }, 12, 0, true).show();
        });
    }

    private void updateDateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault());
        tvSelectDate.setText(sdf.format(selectedCal.getTime()));
    }

    private final Callback<Chore> choreCallback = new Callback<Chore>() {
        @Override public void onResponse(Call<Chore> call, Response<Chore> response) {
            if (response.isSuccessful()) finish(); else btnSave.setEnabled(true);
        }
        @Override public void onFailure(Call<Chore> call, Throwable t) { btnSave.setEnabled(true); }
    };

    private void setAdapter(Spinner s, String[] data) {
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, data);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
    }

    private int parseInt(String val, int def) {
        try { return Integer.parseInt(val); } catch (Exception e) { return def; }
    }
}