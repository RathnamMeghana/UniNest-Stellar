package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.BillsApi;
import com.example.uninest.model.BillsRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewBillsActivity extends AppCompatActivity {

    private RecyclerView rvActive, rvPaid;
    private BillAdapter activeAdapter;
    private PaidBillAdapter paidAdapter;
    private BillsApi billsApi;

    private String userId = "BpkiEXWj9kXTjZ04GfFsGtZ2icq1"; // Hardcoded for testing
    private TextView tvTotalOwed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_bills);

        billsApi = ApiClient.getBillsApi();

        tvTotalOwed = findViewById(R.id.tvTotalOwed);

        // Setup Active Bills RecyclerView
        rvActive = findViewById(R.id.rvBills);
        rvActive.setLayoutManager(new LinearLayoutManager(this));
        activeAdapter = new BillAdapter(new ArrayList<>(), userId, (bill, position) -> markBillAsPaid(bill));
        rvActive.setAdapter(activeAdapter);

        // Setup Paid Bills RecyclerView
        rvPaid = findViewById(R.id.rvPaidBills);
        rvPaid.setLayoutManager(new LinearLayoutManager(this));
        paidAdapter = new PaidBillAdapter(new ArrayList<>());
        rvPaid.setAdapter(paidAdapter);

        // Fetch data
        fetchTotalOwed();
        fetchActiveBills();
        fetchPaidSplits();
    }

    // ---------------- ACTIVE BILLS ----------------
    private void fetchActiveBills() {
        billsApi.getBills(userId).enqueue(new Callback<List<BillsRequest>>() {
            @Override
            public void onResponse(Call<List<BillsRequest>> call, Response<List<BillsRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BillsRequest> bills = response.body();
                    activeAdapter.updateData(bills);
                } else {
                    Log.e("ViewBills", "Active Bills Server Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<BillsRequest>> call, Throwable t) {
                Log.e("ViewBills", "Active Bills Network Failure: " + t.getMessage());
            }
        });
    }

    // ---------------- PAID BILLS ----------------
    private void fetchPaidSplits() {
        billsApi.getPaidHistory(userId).enqueue(new Callback<List<BillsRequest>>() {
            @Override
            public void onResponse(Call<List<BillsRequest>> call, Response<List<BillsRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BillsRequest> bills = response.body();
                    List<BillsRequest.Split> paidSplits = new ArrayList<>();

                    for (BillsRequest bill : bills) {
                        if (bill.getSplits() != null) {
                            for (BillsRequest.Split split : bill.getSplits()) {
                                if (split.isPaid() && userId.equals(split.getUserId())) {
                                    // Fill bill info for display
                                    split.setBillTitle(bill.getTitle());
                                    split.setBillId(bill.getId());
                                    paidSplits.add(split);
                                }
                            }
                        }
                    }

                    paidAdapter.updateData(paidSplits);

                } else {
                    Log.e("ViewBills", "Paid Splits Server Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<BillsRequest>> call, Throwable t) {
                Log.e("ViewBills", "Paid Splits Network Failure: " + t.getMessage());
            }
        });
    }

    // ---------------- MARK BILL AS PAID ----------------
    private void markBillAsPaid(BillsRequest bill) {
        if (bill == null) return;

        billsApi.markBillPaid(bill.getId(), userId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ViewBillsActivity.this, "Bill marked as paid!", Toast.LENGTH_SHORT).show();
                    // Refresh
                    fetchActiveBills();
                    fetchPaidSplits();
                    fetchTotalOwed();
                } else {
                    Toast.makeText(ViewBillsActivity.this, "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ViewBillsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------- TOTAL OWED ----------------
    private void fetchTotalOwed() {
        billsApi.getTotalOwed(userId).enqueue(new Callback<Double>() {
            @Override
            public void onResponse(Call<Double> call, Response<Double> response) {
                if (response.isSuccessful() && response.body() != null) {
                    double totalOwed = response.body();
                    tvTotalOwed.setText(String.format(Locale.getDefault(), "Total Owed: €%.2f", totalOwed));
                } else {
                    Log.e("ViewBills", "Failed to fetch total owed, code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Double> call, Throwable t) {
                Log.e("ViewBills", "Network error fetching total owed: " + t.getMessage());
            }
        });
    }
}
