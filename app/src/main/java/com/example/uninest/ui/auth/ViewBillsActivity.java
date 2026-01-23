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
import com.example.uninest.model.BillSplitRequest; // Ensure this model exists for the split history
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ViewBillsActivity extends AppCompatActivity {

    private RecyclerView rvActive, rvPaid;
    private BillAdapter activeAdapter;
    private PaidBillAdapter paidAdapter; // Adapter for BillSplitRequest
    private BillsApi billsApi;

    // Hardcoded User ID for testing
    private String userId = "BpkiEXWj9kXTjZ04GfFsGtZ2icq1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_bills);

        billsApi = ApiClient.getBillsApi();

        // 1. Setup Active/Pending Bills RecyclerView
        // Note: Using rvBills from your XML for the main list
        rvActive = findViewById(R.id.rvBills);
        rvActive.setLayoutManager(new LinearLayoutManager(this));
        activeAdapter = new BillAdapter(new ArrayList<>(), (bill, position) -> {
            markBillAsPaid(bill); // your existing method
        });
        rvActive.setAdapter(activeAdapter);


        // 2. Setup Paid History RecyclerView
        rvPaid = findViewById(R.id.rvPaidBills);
        rvPaid.setLayoutManager(new LinearLayoutManager(this));
        paidAdapter = new PaidBillAdapter(new ArrayList<>());
        rvPaid.setAdapter(paidAdapter);

        // Load data from both endpoints
        fetchActiveBills();
        fetchPaidHistory();
    }

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

    private void fetchPaidHistory() {
        Log.d("ViewBills", "Fetching paid history for: " + userId);
        billsApi.getPaidHistory(userId).enqueue(new Callback<List<BillSplitRequest>>() {
            @Override
            public void onResponse(Call<List<BillSplitRequest>> call, Response<List<BillSplitRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BillSplitRequest> history = response.body();
                    Log.d("ViewBills", "Paid history count: " + history.size());
                    paidAdapter.updateData(history);
                } else {
                    Log.e("ViewBills", "Paid History Server Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<BillSplitRequest>> call, Throwable t) {
                Log.e("ViewBills", "Paid History Network Failure: " + t.getMessage());
            }
        });
    }

    private void markBillAsPaid(BillsRequest bill) {
        if (bill == null) return;

        // Assuming the user paying is always `userId`
        billsApi.markBillPaid(bill.getId(), userId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ViewBillsActivity.this, "Bill marked as paid!", Toast.LENGTH_SHORT).show();
                    fetchActiveBills();    // Refresh active bills
                    fetchPaidHistory();    // Refresh paid history
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