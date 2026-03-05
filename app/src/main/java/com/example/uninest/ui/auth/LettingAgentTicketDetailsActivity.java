package com.example.uninest.ui.auth;

import android.app.DatePickerDialog;
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
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.TicketApi;
import com.example.uninest.model.Ticket;
import com.example.uninest.model.UpdateTicketAgentDataRequest;
import com.example.uninest.model.UpdateTicketPriorityRequest;
import com.example.uninest.model.UpdateTicketStatusRequest;

import java.util.Calendar;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LettingAgentTicketDetailsActivity extends AppCompatActivity {

    private Ticket ticket;
    private TicketApi ticketApi;

    // UI Elements
    private TextView tvTicketCategory, tvLocationInfo, tvDescription, tvDateSelector;
    private Spinner spinnerPriority, spinnerStatus;
    private EditText etAgentResponse;
    private ImageView ivTicketImage;

    // Variables
    private String selectedDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letting_agent_ticket_details);

        ticketApi = ApiClient.getTicketApi();

        // 1. Receive Data
        if (getIntent().hasExtra("TICKET_OBJ")) {
            ticket = (Ticket) getIntent().getSerializableExtra("TICKET_OBJ");
        }

        // 2. Init Views
        tvTicketCategory = findViewById(R.id.tvTicketCategory);
        tvLocationInfo = findViewById(R.id.tvLocationInfo);
        tvDescription = findViewById(R.id.tvDescription);

        spinnerPriority = findViewById(R.id.spinnerPriority);
        spinnerStatus = findViewById(R.id.spinnerStatus);

        tvDateSelector = findViewById(R.id.tvDateSelector);
        etAgentResponse = findViewById(R.id.etAgentResponse);
        ivTicketImage = findViewById(R.id.ivTicketImage);

        Button btnSave = findViewById(R.id.btnSaveChanges);
        View btnBack = findViewById(R.id.btnBack);

        // 3. Populate Data
        if(ticket != null) {

            if (ticket.getImageUrl() != null && !ticket.getImageUrl().isEmpty()) {
                ivTicketImage.setVisibility(View.VISIBLE);

                try {
                    // Convert Base64 String to byte array
                    byte[] imageBytes = android.util.Base64.decode(ticket.getImageUrl(), android.util.Base64.DEFAULT);

                    // Load using Glide
                    com.bumptech.glide.Glide.with(this)
                            .asBitmap()
                            .load(imageBytes)
                            .placeholder(android.R.drawable.progress_horizontal)
                            .error(android.R.drawable.ic_menu_report_image)
                            .into(ivTicketImage);
                } catch (Exception e) {
                    ivTicketImage.setVisibility(View.GONE);
                    android.util.Log.e("IMAGE_ERROR", "Failed to decode image", e);
                }
            } else {
                ivTicketImage.setVisibility(View.GONE); // Ensure it's hidden if no image
            }

            tvTicketCategory.setText(ticket.getCategory() != null ? ticket.getCategory() : "Maintenance Ticket");

            String building = (ticket.getBuilding() != null && !ticket.getBuilding().isEmpty()) ? ticket.getBuilding() : "Unknown Building";

            String apt = ticket.getApartmentName() != null ? ticket.getApartmentName() : "Unit";
            String room = ticket.getRoom() != null ? ticket.getRoom() : "General";
            //tvLocationInfo.setText(apt + " - " + room);
            tvLocationInfo.setText(building + " | " + apt + " - " + room);
            // Set Description
            tvDescription.setText(ticket.getDescription());

            // Set Agent Response if exists
            if(ticket.getAgentResponse() != null) {
                etAgentResponse.setText(ticket.getAgentResponse());
            }

            // Set Date if exists
            if(ticket.getArrivalDate() != null) {
                selectedDate = ticket.getArrivalDate();
                tvDateSelector.setText("Scheduled: " + selectedDate);
            }
        }

        setupSpinners();

        // 4. Listeners
        btnBack.setOnClickListener(v -> finish());
        tvDateSelector.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> saveAllChanges());
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                    tvDateSelector.setText("Scheduled: " + selectedDate);
                },
                year, month, day);
        datePickerDialog.show();
    }

    private void saveAllChanges() {
        if (ticket == null) return;

        String newPriority = spinnerPriority.getSelectedItem().toString();
        String newStatus = spinnerStatus.getSelectedItem().toString();

        // 1. Update Priority ONLY if it changed
        if (!newPriority.equalsIgnoreCase(ticket.getPriority())) {
            UpdateTicketPriorityRequest pReq = new UpdateTicketPriorityRequest();
            pReq.setTicketId(ticket.getId());
            pReq.setPriority(newPriority);
            ticketApi.updatePriority(pReq).enqueue(new Callback<String>() {
                @Override public void onResponse(Call<String> c, Response<String> r){}
                @Override public void onFailure(Call<String> c, Throwable t){}
            });
        }

        // 2. Update Status
        UpdateTicketStatusRequest sReq = new UpdateTicketStatusRequest();
        sReq.setTicketId(ticket.getId());
        sReq.setStatus(newStatus);
        ticketApi.updateStatus(sReq).enqueue(new Callback<String>() {
            @Override public void onResponse(Call<String> c, Response<String> r){}
            @Override public void onFailure(Call<String> c, Throwable t){}
        });

        // 3. Update Agent Response & Date
        UpdateTicketAgentDataRequest dataReq = new UpdateTicketAgentDataRequest();
        dataReq.setTicketId(ticket.getId());
        dataReq.setResponse(etAgentResponse.getText().toString());
        dataReq.setArrivalDate(selectedDate);

        ticketApi.updateAgentData(dataReq).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if(response.isSuccessful()) {
                    Toast.makeText(LettingAgentTicketDetailsActivity.this, "Details Updated Successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(LettingAgentTicketDetailsActivity.this, "Update Failed", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(LettingAgentTicketDetailsActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSpinners() {
        String[] priorities = {"Low", "Medium", "High"};
        ArrayAdapter<String> pAdapt = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, priorities);
        spinnerPriority.setAdapter(pAdapt);
        if(ticket != null && ticket.getPriority() != null) {
            spinnerPriority.setSelection(pAdapt.getPosition(ticket.getPriority()));
        }

        String[] statuses = {"Open", "In_Process", "Resolved", "Closed"};
        ArrayAdapter<String> sAdapt = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statuses);
        spinnerStatus.setAdapter(sAdapt);
        if(ticket != null && ticket.getStatus() != null) {
            spinnerStatus.setSelection(sAdapt.getPosition(ticket.getStatus()));
        }
    }
}