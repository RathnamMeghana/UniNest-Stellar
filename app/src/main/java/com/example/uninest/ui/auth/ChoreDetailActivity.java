package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.SessionManager; // Import SessionManager
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.User;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChoreDetailActivity extends AppCompatActivity {

    private TextView tvChoreTitle, tvDescription, tvEstTime, tvRoomInfo, tvAssigneeInfo, tvCreatedByInfo;
    private Spinner spinnerStatus, spinnerSwap;
    private EditText etActualTime;
    private Button btnUpdate;
    private TextView tvDueDate;
    private ImageView btnBack;

    private String choreId, houseCode, currentUserId;
    private SessionManager sessionManager; // Add SessionManager

    // We use a specific list for the spinner that DOES NOT contain "Me"
    private List<User> swapCandidates = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chore_detail);

        // 1. Init Session to get My ID
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();

        // 2. Get Intent Data
        choreId = getIntent().getStringExtra("CHORE_ID");
        houseCode = getIntent().getStringExtra("HOUSE_CODE");
        String title = getIntent().getStringExtra("TITLE");
        String description = getIntent().getStringExtra("DESCRIPTION");
        String location = getIntent().getStringExtra("LOCATION");
        int estDuration = getIntent().getIntExtra("EST_DURATION", 0);
        String currentStatus = getIntent().getStringExtra("CURRENT_STATUS");
        String currentAssignee = getIntent().getStringExtra("ASSIGNED_TO_NAME");
        String createdBy = getIntent().getStringExtra("CREATED_BY_NAME");
        String dueDateStr = getIntent().getStringExtra("DUE_DATE");


        // 3. Init Views
        btnBack = findViewById(R.id.btnBack);
        tvChoreTitle = findViewById(R.id.tvChoreTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvEstTime = findViewById(R.id.tvEstTime);
        tvRoomInfo = findViewById(R.id.tvRoomInfo);
        tvAssigneeInfo = findViewById(R.id.tvAssigneeInfo);
        tvCreatedByInfo = findViewById(R.id.tvCreatedByInfo);
        tvDueDate = findViewById(R.id.tvDueDateInfo);


        spinnerStatus = findViewById(R.id.spinnerStatus);
        spinnerSwap = findViewById(R.id.spinnerSwapUser);
        etActualTime = findViewById(R.id.etActualTime);
        btnUpdate = findViewById(R.id.btnUpdateChore);

        // 4. Set UI Data
        tvChoreTitle.setText(title);

        if (description != null && !description.isEmpty()) {
            tvDescription.setText(description);
        } else {
            tvDescription.setText("No description provided.");
        }

        tvEstTime.setText(estDuration > 0 ? estDuration + " Mins" : "--");

        if (location != null && !location.isEmpty()) {
            tvRoomInfo.setText(location);
        } else {
            tvRoomInfo.setText("General");
        }

        tvAssigneeInfo.setText("" + (currentAssignee != null ? currentAssignee : "Unassigned"));
        tvCreatedByInfo.setText("Created by: " + (createdBy != null ? createdBy : "Unknown"));

        if (dueDateStr != null) {
            tvDueDate.setText(dueDateStr);
        } else {
            tvDueDate.setText("No date set");
        }

        // 5. Listeners
        btnBack.setOnClickListener(v -> finish());

        setupStatusSpinner(currentStatus);
        loadRoommates();

        btnUpdate.setOnClickListener(v -> updateChore());
    }

    private void setupStatusSpinner(String currentStatus) {
        String[] statuses = {"Not Started", "In Progress", "Completed"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapter);

        if (currentStatus != null) {
            if (currentStatus.equals("IN_PROGRESS")) {
                spinnerStatus.setSelection(1);
            } else if (currentStatus.equals("COMPLETED")) {
                spinnerStatus.setSelection(2);
            } else {
                spinnerStatus.setSelection(0);
            }
        }
    }

    private void loadRoommates() {
        ApiClient.getUserApi().getRoommates(houseCode).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if(response.isSuccessful() && response.body() != null) {

                    swapCandidates.clear();
                    List<String> displayNames = new ArrayList<>();

                    displayNames.add("Keep Current Assignee");

                    // Filter: Add ONLY if user ID != currentUserId
                    for(User u : response.body()) {
                        if (u.getId() != null && !u.getId().equals(currentUserId)) {
                            swapCandidates.add(u); // Add to logic list
                            displayNames.add(u.getFullName()); // Add to spinner text list
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(ChoreDetailActivity.this, android.R.layout.simple_spinner_item, displayNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerSwap.setAdapter(adapter);
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) {}
        });
    }

    private void updateChore() {
        btnUpdate.setEnabled(false);
        btnUpdate.setText("Saving...");

        // Get Status
        String statusUi = spinnerStatus.getSelectedItem().toString();
        String statusApi = "NOT_STARTED";
        if(statusUi.equals("In Progress")) statusApi = "IN_PROGRESS";
        if(statusUi.equals("Completed")) statusApi = "COMPLETED";

        // Get Time
        int time = 0;
        try { time = Integer.parseInt(etActualTime.getText().toString()); } catch (Exception e){}

        // Get Swap ID
        String assignedToId = null;
        int swapIndex = spinnerSwap.getSelectedItemPosition();

        // Index 0 is "Keep Current", so Index 1 matches swapCandidates.get(0)
        if(swapIndex > 0 && !swapCandidates.isEmpty()) {
            assignedToId = swapCandidates.get(swapIndex - 1).getId();
        }

        ApiClient.getChoreApi().updateChoreStatus(houseCode, choreId, statusApi, time, assignedToId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if(response.isSuccessful()) {
                    Toast.makeText(ChoreDetailActivity.this, "Updated!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    btnUpdate.setEnabled(true);
                    btnUpdate.setText("Save Changes");
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnUpdate.setEnabled(true);
                btnUpdate.setText("Save Changes");
            }
        });
    }
}