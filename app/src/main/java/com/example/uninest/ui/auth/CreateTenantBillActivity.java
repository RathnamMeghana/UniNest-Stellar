package com.example.uninest.ui.auth;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

    private EditText etTitle, etAmount;
    private TextView tvSplitPreview, tvFrequencyLabel,btnDate;
    private MaterialButton btnSubmit;
    private Spinner spinnerBillType, spinnerFrequency;
    private RecyclerView rvRoommates;
    private BillSplittingAdapter roommateAdapter;

    private List<String> selectedIds = new ArrayList<>();
    private SessionManager session;
    private Date dueDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_bill_create);
        session = new SessionManager(this);

        // Standard Fields
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
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) { updateSplit(); }
            @Override public void afterTextChanged(Editable s) {}
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
                int visibility = (selectedType == BillsRequest.BillType.RECURRING) ? View.VISIBLE : View.GONE;
                tvFrequencyLabel.setVisibility(visibility);
                spinnerFrequency.setVisibility(visibility);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupList() {
        rvRoommates.setLayoutManager(new LinearLayoutManager(this));
        roommateAdapter = new BillSplittingAdapter(new ArrayList<>(), session.getUserId(), (userId, isChecked) -> {
            if (isChecked) {
                if (!selectedIds.contains(userId)) selectedIds.add(userId);
            } else {
                selectedIds.remove(userId);
            }
            updateSplit();
        });
        rvRoommates.setAdapter(roommateAdapter);
    }

    private void setupDatePicker() {
        btnDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> {
                Calendar cal = Calendar.getInstance();
                cal.set(y, m, d);
                dueDate = cal.getTime();
                btnDate.setText(DateFormat.getDateInstance().format(dueDate));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void loadBuildingRoommates() {
        ApiClient.getUserApi().getRoommates(session.fetchHouseCode()).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful()) roommateAdapter.updateList(response.body());
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) {}
        });
    }

    private void updateSplit() {
        String val = etAmount.getText().toString();
        if (val.isEmpty() || selectedIds.isEmpty()) {
            tvSplitPreview.setText("Each pays: €0.00");
            return;
        }
        try {
            double total = Double.parseDouble(val);
            double split = total / selectedIds.size();
            tvSplitPreview.setText(String.format("Each pays: €%.2f", split));
        } catch (NumberFormatException e) {
            tvSplitPreview.setText("Each pays: €0.00");
        }
    }

    private void saveBill() {
        String title = etTitle.getText().toString().trim();
        String amtStr = etAmount.getText().toString().trim();

        if (title.isEmpty() || amtStr.isEmpty() || dueDate == null || selectedIds.isEmpty()) {
            Toast.makeText(this, "Complete all fields and select roommates", Toast.LENGTH_SHORT).show();
            return;
        }


        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        String isoDate = isoFormat.format(dueDate);


        double total = Double.parseDouble(amtStr);
        double perPerson = total / selectedIds.size();

        BillsRequest req = new BillsRequest();
        req.setTitle(title);
        req.setTotalAmount(total);
        req.setDueDate(isoDate);
        req.setHouseCode(session.fetchHouseCode());
        req.setCreatorId(session.getUserId());
        req.setRoommateIds(new ArrayList<>(selectedIds));
        req.setActive(true);

        List<BillsRequest.Split> splits = new ArrayList<>();
        for (String id : selectedIds) {
            BillsRequest.Split s = new BillsRequest.Split();
            s.setUserId(id);
            s.setAmountOwed(perPerson);
            s.setPaid(false);
            splits.add(s);
        }
        req.setSplits(splits);


        BillsRequest.BillType type = (BillsRequest.BillType) spinnerBillType.getSelectedItem();
        req.setBillType(type);
        if (type == BillsRequest.BillType.RECURRING) {
            req.setFrequency((BillsRequest.BillFrequency) spinnerFrequency.getSelectedItem());
            req.setStartDate(isoFormat.format(new Date())); // Matches her logic
        }

        ApiClient.getBillsApi().createBill(req).enqueue(new Callback<List<BillsRequest>>() {
            @Override
            public void onResponse(Call<List<BillsRequest>> call, Response<List<BillsRequest>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CreateTenantBillActivity.this, "Split Requested!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CreateTenantBillActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<List<BillsRequest>> call, Throwable t) {
                Toast.makeText(CreateTenantBillActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}