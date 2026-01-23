package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.util.Log;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewBillsActivity extends AppCompatActivity {

    private RecyclerView rvActive, rvPaid;
    private BillAdapter activeAdapter;
    private PaidBillAdapter paidAdapter;
    private BillsApi billsApi;

    // Hardcoded User ID for testing
    private String userId = "BpkiEXWj9kXTjZ04GfFsGtZ2icq1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_bills);

        billsApi = ApiClient.getBillsApi();

        // Setup Active Bills RecyclerView
        rvActive = findViewById(R.id.rvBills);
        rvActive.setLayoutManager(new LinearLayoutManager(this));
        activeAdapter = new BillAdapter(new ArrayList<>(), (bill, position) -> {
            markBillAsPaid(bill);
        });
        rvActive.setAdapter(activeAdapter);

        // Setup Paid Splits RecyclerView
        rvPaid = findViewById(R.id.rvPaidBills);
        rvPaid.setLayoutManager(new LinearLayoutManager(this));
        paidAdapter = new PaidBillAdapter(new ArrayList<>());
        rvPaid.setAdapter(paidAdapter);

        // Load data from backend
        fetchActiveBills();
        fetchPaidSplits();
    }

    /**
     * Fetch all bills and populate active bills RecyclerView
     */
    private void fetchActiveBills() {
        Log.d("ViewBills", "Fetching active bills for: " + userId);
        billsApi.getBills(userId).enqueue(new Callback<List<BillsRequest>>() {
            @Override
            public void onResponse(Call<List<BillsRequest>> call, Response<List<BillsRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BillsRequest> bills = response.body();
                    Log.d("ViewBills", "Active bills count: " + bills.size());
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


    private void fetchPaidSplits() {
        Log.d("ViewBills", "Fetching paid splits for: " + userId);
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
                                    paidSplits.add(split);
                                }
                            }
                        }
                    }

                    Log.d("ViewBills", "Paid splits count: " + paidSplits.size());
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


    /**
     * Marks a bill as paid for this user and refreshes both RecyclerViews
     */
    private void markBillAsPaid(BillsRequest bill) {
        if (bill == null) return;

        billsApi.markBillPaid(bill.getId(), userId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ViewBillsActivity.this, "Bill marked as paid!", Toast.LENGTH_SHORT).show();
                    // Refresh lists
                    fetchActiveBills();
                    fetchPaidSplits();
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
}
