package com.example.uninest.ui.auth;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.BillsRequest;
import com.example.uninest.model.User;
import com.google.android.material.button.MaterialButton;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateTenantBillActivity extends AppCompatActivity {

    private EditText etTitle;
    private EditText etAmount;
    private TextView tvSplitPreview;
    private TextView tvFrequencyLabel;
    private TextView btnDate;
    private MaterialButton btnSubmit;
    private Spinner spinnerBillType;
    private Spinner spinnerFrequency;
    private RecyclerView rvRoommates;
    private BillSplittingAdapter roommateAdapter;

    private final List<String> selectedIds = new ArrayList<>();
    private SessionManager session;
    private Date dueDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_bill_create);
        session = new SessionManager(this);

        etTitle = findViewById(R.id.etBillTitle);
        etAmount = findViewById(R.id.etAmount);
        tvSplitPreview = findViewById(R.id.tvSplitAmount);
        btnDate = findViewById(R.id.btnPickDate);
        btnSubmit = findViewById(R.id.btnSubmitBill);
        rvRoommates = findViewById(R.id.rvRoommates);
        spinnerBillType = findViewById(R.id.spinnerBillType);
        spinnerFrequency = findViewById(R.id.spinnerFrequency);
        tvFrequencyLabel = findViewById(R.id.tvFrequencyLabel);

        setupSpinners();
        setupList();
        setupDatePicker();

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSplit();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadBuildingRoommates();
        btnSubmit.setOnClickListener(v -> saveBill());
    }

    private void setupSpinners() {
        ArrayAdapter<BillsRequest.BillType> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, BillsRequest.BillType.values());
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBillType.setAdapter(typeAdapter);

        ArrayAdapter<BillsRequest.BillFrequency> freqAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, BillsRequest.BillFrequency.values());
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrequency.setAdapter(freqAdapter);

        spinnerBillType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                BillsRequest.BillType selectedType = (BillsRequest.BillType) spinnerBillType.getSelectedItem();
                int visibility = selectedType == BillsRequest.BillType.RECURRING ? View.VISIBLE : View.GONE;
                tvFrequencyLabel.setVisibility(visibility);
                spinnerFrequency.setVisibility(visibility);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupList() {
        rvRoommates.setLayoutManager(new LinearLayoutManager(this));
        roommateAdapter = new BillSplittingAdapter(new ArrayList<>(), session.getUserId(), (userId, isChecked) -> {
            if (isChecked) {
                if (!selectedIds.contains(userId)) {
                    selectedIds.add(userId);
                }
            } else {
                selectedIds.remove(userId);
            }
            updateSplit();
        });
        rvRoommates.setAdapter(roommateAdapter);
    }

    private void setupDatePicker() {
        btnDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth);
                dueDate = selected.getTime();
                btnDate.setText(DateFormat.getDateInstance().format(dueDate));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void loadBuildingRoommates() {
        ApiClient.getUserApi().getRoommates(session.fetchHouseCode()).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    roommateAdapter.updateList(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
            }
        });
    }

    private void updateSplit() {
        String value = etAmount.getText().toString();
        if (value.isEmpty() || selectedIds.isEmpty()) {
            tvSplitPreview.setText("Each pays: \u20AC0.00");
            return;
        }

        try {
            double total = Double.parseDouble(value);
            double split = total / selectedIds.size();
            tvSplitPreview.setText(String.format(Locale.getDefault(), "Each pays: \u20AC%.2f", split));
        } catch (NumberFormatException e) {
            tvSplitPreview.setText("Each pays: \u20AC0.00");
        }
    }

    private void saveBill() {
        String title = etTitle.getText().toString().trim();
        String amountString = etAmount.getText().toString().trim();

        if (title.isEmpty() || amountString.isEmpty() || dueDate == null || selectedIds.isEmpty()) {
            Toast.makeText(this, "Complete all fields and select roommates", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        String isoDate = isoFormat.format(dueDate);

        double total = Double.parseDouble(amountString);
        double perPerson = total / selectedIds.size();

        BillsRequest request = new BillsRequest();
        request.setTitle(title);
        request.setTotalAmount(total);
        request.setDueDate(isoDate);
        request.setHouseCode(session.fetchHouseCode());
        request.setCreatorId(session.getUserId());
        request.setRoommateIds(new ArrayList<>(selectedIds));
        request.setActive(true);

        List<BillsRequest.Split> splits = new ArrayList<>();
        for (String id : selectedIds) {
            BillsRequest.Split split = new BillsRequest.Split();
            split.setUserId(id);
            split.setAmountOwed(perPerson);
            split.setPaid(false);
            splits.add(split);
        }
        request.setSplits(splits);

        BillsRequest.BillType type = (BillsRequest.BillType) spinnerBillType.getSelectedItem();
        request.setBillType(type);
        if (type == BillsRequest.BillType.RECURRING) {
            request.setFrequency((BillsRequest.BillFrequency) spinnerFrequency.getSelectedItem());
            request.setStartDate(isoFormat.format(new Date()));
        }

        ApiClient.getBillsApi().createBill(request).enqueue(new Callback<List<BillsRequest>>() {
            @Override
            public void onResponse(Call<List<BillsRequest>> call, Response<List<BillsRequest>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CreateTenantBillActivity.this, "Split requested!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CreateTenantBillActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<BillsRequest>> call, Throwable t) {
                Toast.makeText(CreateTenantBillActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
