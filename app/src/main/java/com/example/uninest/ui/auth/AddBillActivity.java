package com.example.uninest.ui.auth;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.BillsApi;
import com.example.uninest.model.BillsRequest;
import com.example.uninest.model.User;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddBillActivity extends AppCompatActivity {

    private EditText etBillTitle, etAmount;
    private Spinner spinnerBillType, spinnerFrequency;
    private TextView tvFrequencyLabel, tvSplitAmount;
    private Button btnPickDate, btnSave;

    private RecyclerView rvRoommates;
    private RoommateSplitAdapter roommateAdapter;
    private List<String> selectedRoommateIds = new ArrayList<>();

    private BillsApi billsApi;
    private FirebaseAuth mAuth;

    private Date selectedDueDate;
    private boolean isSubmitting = false;

    // Hardcoded for testing
    private String houseCode = "APT-02BF8C3";
    private String testUserId = "BpkiEXWj9kXTjZ04GfFsGtZ2icq1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_bill);

        mAuth = FirebaseAuth.getInstance();
        billsApi = ApiClient.getBillsApi();

        etBillTitle = findViewById(R.id.etBillTitle);
        etAmount = findViewById(R.id.etAmount);
        spinnerBillType = findViewById(R.id.spinnerBillType);
        spinnerFrequency = findViewById(R.id.spinnerFrequency);
        tvFrequencyLabel = findViewById(R.id.tvFrequencyLabel);
        tvSplitAmount = findViewById(R.id.tvSplitAmount);
        btnPickDate = findViewById(R.id.btnPickDate);
        btnSave = findViewById(R.id.btnSubmitBill);
        rvRoommates = findViewById(R.id.rvRoommates);

        setupSpinners();
        setupDatePicker();
        setupRecyclerView();
        setupAmountWatcher();

        fetchRoommates(houseCode);

        btnSave.setOnClickListener(v -> saveBill());
    }

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

    private void setupAmountWatcher() {
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSplitDisplay();
            }
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
            tvSplitAmount.setText(String.format("Each pays: €%.2f", split));
        } catch (NumberFormatException e) {
            tvSplitAmount.setText("Each pays: €0.00");
        }
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
                int visibility = (selectedType == BillsRequest.BillType.RECURRING) ? View.VISIBLE : View.GONE;
                tvFrequencyLabel.setVisibility(visibility);
                spinnerFrequency.setVisibility(visibility);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDatePicker() {
        btnPickDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar selectedCal = Calendar.getInstance();
                        selectedCal.set(year, month, dayOfMonth, 0, 0, 0);
                        selectedCal.set(Calendar.MILLISECOND, 0);
                        selectedDueDate = selectedCal.getTime();
                        btnPickDate.setText(DateFormat.getDateInstance().format(selectedDueDate));
                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });
    }

    private void fetchRoommates(String houseCode) {
        ApiClient.getApartmentApi().getUsersForApartment(houseCode).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    roommateAdapter.updateList(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Log.e("AddBill", "API Error: " + t.getMessage());
            }
        });
    }

    private void saveBill() {
        if (isSubmitting) return;

        String title = etBillTitle.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();

        if (title.isEmpty() || amountStr.isEmpty() || selectedDueDate == null || selectedRoommateIds.isEmpty()) {
            Toast.makeText(this, "Please fill all fields and select roommates", Toast.LENGTH_SHORT).show();
            return;
        }

        isSubmitting = true;
        btnSave.setEnabled(false);

        double total = Double.parseDouble(amountStr);
        double perPerson = total / selectedRoommateIds.size();

        BillsRequest request = new BillsRequest();
        request.setTitle(title);
        request.setTotalAmount(total);
        request.setCreatorId(testUserId);
        request.setDueDate(selectedDueDate);
        request.setRoommateIds(selectedRoommateIds);
        request.setHouseCode(houseCode);
        request.setActive(true);

        // Create embedded splits
        List<BillsRequest.Split> splits = new ArrayList<>();
        for (String userId : selectedRoommateIds) {
            BillsRequest.Split split = new BillsRequest.Split();
            split.setUserId(userId);
            split.setAmountOwed(perPerson);
            split.setPaid(false);
            splits.add(split);
        }
        request.setSplits(splits);

        BillsRequest.BillType type = (BillsRequest.BillType) spinnerBillType.getSelectedItem();
        request.setBillType(type);
        if (type == BillsRequest.BillType.RECURRING) {
            request.setFrequency((BillsRequest.BillFrequency) spinnerFrequency.getSelectedItem());
            request.setStartDate(new Date());
        }

        billsApi.createBill(request).enqueue(new Callback<List<BillsRequest>>() {
            @Override
            public void onResponse(Call<List<BillsRequest>> call, Response<List<BillsRequest>> response) {
                isSubmitting = false;
                btnSave.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(AddBillActivity.this, "Bill Created Successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(AddBillActivity.this, ViewBillsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(AddBillActivity.this, "Server Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<BillsRequest>> call, Throwable t) {
                isSubmitting = false;
                btnSave.setEnabled(true);
                Toast.makeText(AddBillActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
