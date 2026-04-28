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
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.TicketApi;
import com.example.uninest.model.Ticket;
import com.example.uninest.model.UpdateTicketAgentDataRequest;
import com.example.uninest.model.UpdateTicketPriorityRequest;
import com.example.uninest.model.UpdateTicketStatusRequest;
import com.example.uninest.utils.ImageUtils;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LettingAgentTicketDetailsActivity extends AppCompatActivity {

    private Ticket ticket;
    private TicketApi ticketApi;

    // UI Elements
    private TextView tvTicketCategory, tvLocationInfo, tvDescription, tvDateSelector;
    private TextView tvPrioritySummary, tvStatusSummary, tvScheduleSummary, tvRaisedBySummary, tvOverviewEyebrow;
    private Spinner spinnerPriority, spinnerStatus;
    private EditText etAgentResponse;
    private ImageView ivTicketImage;
    private View overviewCard;

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
        tvPrioritySummary = findViewById(R.id.tvPrioritySummary);
        tvStatusSummary = findViewById(R.id.tvStatusSummary);
        tvScheduleSummary = findViewById(R.id.tvScheduleSummary);
        tvRaisedBySummary = findViewById(R.id.tvRaisedBySummary);
        tvOverviewEyebrow = findViewById(R.id.tvOverviewEyebrow);

        spinnerPriority = findViewById(R.id.spinnerPriority);
        spinnerStatus = findViewById(R.id.spinnerStatus);

        tvDateSelector = findViewById(R.id.tvDateSelector);
        etAgentResponse = findViewById(R.id.etAgentResponse);
        ivTicketImage = findViewById(R.id.ivTicketImage);
        overviewCard = findViewById(R.id.overviewCard);

        Button btnSave = findViewById(R.id.btnSaveChanges);
        View btnBack = findViewById(R.id.btnBack);

        // 3. Populate Data
        if(ticket != null) {

            if (ticket.getImageUrl() != null && !ticket.getImageUrl().isEmpty()) {
                ivTicketImage.setVisibility(View.VISIBLE);
                ImageUtils.loadTicketImage(ivTicketImage, ticket.getImageUrl());
            } else {
                ivTicketImage.setVisibility(View.GONE);
            }

            tvTicketCategory.setText(ticket.getCategory() != null ? ticket.getCategory() : "Maintenance Ticket");

            String building = (ticket.getBuilding() != null && !ticket.getBuilding().isEmpty()) ? ticket.getBuilding() : "Unknown Building";

            String apt = ticket.getApartmentName() != null ? ticket.getApartmentName() : "Unit";
            String room = ticket.getRoom() != null ? ticket.getRoom() : "General";
            tvLocationInfo.setText(building + " | " + apt + " - " + room);
            tvDescription.setText(ticket.getDescription() != null && !ticket.getDescription().trim().isEmpty()
                    ? ticket.getDescription()
                    : "No description was added for this ticket.");
            tvPrioritySummary.setText(ticket.getPriority() != null ? ticket.getPriority() : "Not set");
            tvStatusSummary.setText(prettyStatus(ticket.getStatus()));
            tvScheduleSummary.setText(ticket.getArrivalDate() != null && !ticket.getArrivalDate().trim().isEmpty()
                    ? ticket.getArrivalDate()
                    : "Not set");
            tvRaisedBySummary.setText(ticket.getUserName() != null && !ticket.getUserName().trim().isEmpty()
                    ? ticket.getUserName()
                    : "Tenant");
            applyOverviewStyle(ticket.getStatus());

            if(ticket.getAgentResponse() != null) {
                etAgentResponse.setText(ticket.getAgentResponse());
            }

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
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();
        builder.setTitleText("Schedule visit");
        builder.setSelection(getUtcSelectionForScheduledDate());
        builder.setTheme(R.style.ThemeOverlay_UniNest_CalendarPicker);

        MaterialDatePicker<Long> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) {
                return;
            }

            Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utcCalendar.setTimeInMillis(selection);
            selectedDate = String.format(
                    Locale.getDefault(),
                    "%d/%d/%d",
                    utcCalendar.get(Calendar.DAY_OF_MONTH),
                    utcCalendar.get(Calendar.MONTH) + 1,
                    utcCalendar.get(Calendar.YEAR)
            );
            tvDateSelector.setText("Scheduled: " + selectedDate);
        });
        picker.show(getSupportFragmentManager(), "ticket_schedule_picker");
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
                com.example.uninest.utils.NetworkErrorDialog.show(
                        LettingAgentTicketDetailsActivity.this,
                        LettingAgentTicketDetailsActivity.this::saveAllChanges
                );
            }
        });
    }

    private void setupSpinners() {
        String[] priorities = {"Low", "Medium", "High"};
        ArrayAdapter<String> pAdapt = new ArrayAdapter<>(this, R.layout.item_calendar_spinner_selected, priorities);
        pAdapt.setDropDownViewResource(R.layout.item_calendar_spinner_dropdown);
        spinnerPriority.setAdapter(pAdapt);
        if(ticket != null && ticket.getPriority() != null) {
            spinnerPriority.setSelection(pAdapt.getPosition(ticket.getPriority()));
        }

        String[] statuses = {"Open", "In_Process", "Resolved", "Closed"};
        ArrayAdapter<String> sAdapt = new ArrayAdapter<>(this, R.layout.item_calendar_spinner_selected, statuses);
        sAdapt.setDropDownViewResource(R.layout.item_calendar_spinner_dropdown);
        spinnerStatus.setAdapter(sAdapt);
        if(ticket != null && ticket.getStatus() != null) {
            spinnerStatus.setSelection(sAdapt.getPosition(ticket.getStatus()));
        }
    }

    private void applyOverviewStyle(String rawStatus) {
        String canonical = canonicalStatus(rawStatus);
        if ("progress".equals(canonical)) {
            overviewCard.setBackgroundResource(R.drawable.bg_tenant_ticket_section_progress);
            tvOverviewEyebrow.setTextColor(getColor(R.color.ticket_progress_text));
            tvStatusSummary.setTextColor(getColor(R.color.ticket_progress_text));
        } else if ("solved".equals(canonical)) {
            overviewCard.setBackgroundResource(R.drawable.bg_tenant_ticket_section_solved);
            tvOverviewEyebrow.setTextColor(getColor(R.color.ticket_solved_text));
            tvStatusSummary.setTextColor(getColor(R.color.ticket_solved_text));
        } else {
            overviewCard.setBackgroundResource(R.drawable.bg_tenant_ticket_section_raised);
            tvOverviewEyebrow.setTextColor(getColor(R.color.ticket_raised_text));
            tvStatusSummary.setTextColor(getColor(R.color.ticket_raised_text));
        }
    }

    private String prettyStatus(String rawStatus) {
        String canonical = canonicalStatus(rawStatus);
        if ("progress".equals(canonical)) {
            return "In progress";
        }
        if ("solved".equals(canonical)) {
            return "Solved";
        }
        return "Raised";
    }

    private String canonicalStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            return "raised";
        }

        String normalized = rawStatus.trim()
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ");

        if ("in progress".equals(normalized) || "in process".equals(normalized)) {
            return "progress";
        }

        if ("resolved".equals(normalized) || "closed".equals(normalized) || "solved".equals(normalized)) {
            return "solved";
        }

        return "raised";
    }

    private long getUtcSelectionForScheduledDate() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        if (selectedDate == null || selectedDate.trim().isEmpty()) {
            return calendar.getTimeInMillis();
        }

        String[] patterns = {"d/M/yyyy", "dd/MM/yyyy"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
                sdf.setLenient(false);
                Date parsed = sdf.parse(selectedDate.trim());
                if (parsed != null) {
                    calendar.setTime(parsed);
                    return calendar.getTimeInMillis();
                }
            } catch (Exception ignored) {
            }
        }
        return calendar.getTimeInMillis();
    }
}
