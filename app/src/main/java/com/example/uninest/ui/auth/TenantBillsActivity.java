package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.BillsRequest;
import com.example.uninest.model.OwedToUser;
import com.example.uninest.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TenantBillsActivity extends AppCompatActivity {

    private RecyclerView rvActive, rvHistory;
    private TenantActiveBillsAdapter activeAdapter;
    private TenantPaidHistoryAdapter historyAdapter;
    private TenantOwedToMeAdapter owedToMeAdapter;

    private TextView tvSummary, tvHistoryHeader, tvActiveHeader;
    private TabLayout tabLayout;
    private SessionManager sessionManager;
    private String currentUserId, houseCode;
    private Map<String, String> roommateNameMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_bills);

        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();
        houseCode = sessionManager.fetchHouseCode();

        initViews();
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchRoommatesThenBills();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.billTabLayout);
        tvSummary = findViewById(R.id.tvTotalSummary);
        tvActiveHeader = findViewById(R.id.tvActiveHeader);
        tvHistoryHeader = findViewById(R.id.tvHistoryHeader);

        rvActive = findViewById(R.id.rvBillsActive);
        rvHistory = findViewById(R.id.rvBillsHistory);

        rvActive.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setLayoutManager(new LinearLayoutManager(this));


        rvActive.setNestedScrollingEnabled(false);
        rvHistory.setNestedScrollingEnabled(false);

        activeAdapter = new TenantActiveBillsAdapter(new ArrayList<>(), currentUserId, roommateNameMap, this::markBillPaid);
        historyAdapter = new TenantPaidHistoryAdapter(new ArrayList<>(), roommateNameMap, currentUserId);
        owedToMeAdapter = new TenantOwedToMeAdapter(new ArrayList<>(), roommateNameMap);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                refreshData();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        findViewById(R.id.btnAddBill).setOnClickListener(v ->
                startActivity(new Intent(this, CreateTenantBillActivity.class)));
    }

    private void refreshData() {
        if (tabLayout.getSelectedTabPosition() == 0) {
            tvActiveHeader.setText("Bills to Pay");
            tvHistoryHeader.setText("Paid History");
            findViewById(R.id.btnAddBill).setVisibility(View.VISIBLE);
            fetchActiveBills();
            fetchPaidHistory();
        } else {
            tvActiveHeader.setText("Money Owed to You");
            tvHistoryHeader.setText("Received History");
            findViewById(R.id.btnAddBill).setVisibility(View.GONE);
            fetchOwedToMe();
            fetchReceivedHistory();
        }
    }

    private void fetchActiveBills() {
        ApiClient.getBillsApi().getBills(currentUserId).enqueue(new Callback<List<BillsRequest>>() {
            @Override
            public void onResponse(Call<List<BillsRequest>> call, Response<List<BillsRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    Map<String, BillsRequest> filtered = new HashMap<>();
                    for (BillsRequest b : response.body()) {
                        if (!filtered.containsKey(b.getTitle()) || b.getDueDate().compareTo(filtered.get(b.getTitle()).getDueDate()) < 0) {
                            filtered.put(b.getTitle(), b);
                        }
                    }
                    List<BillsRequest> list = new ArrayList<>(filtered.values());
                    activeAdapter.setData(list);
                    rvActive.setAdapter(activeAdapter);

                    double total = 0;
                    for(BillsRequest b : list) {
                        for(BillsRequest.Split s : b.getSplits()) {
                            if(s.getUserId().equals(currentUserId)) total += s.getAmountOwed();
                        }
                    }
                    tvSummary.setText(String.format(Locale.getDefault(), "Total You Owe: €%.2f", total));
                }
            }
            @Override public void onFailure(Call<List<BillsRequest>> call, Throwable t) {}
        });
    }

    private void fetchPaidHistory() {
        ApiClient.getBillsApi().getPaidHistory(currentUserId).enqueue(new Callback<List<BillsRequest>>() {
            @Override
            public void onResponse(Call<List<BillsRequest>> call, Response<List<BillsRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BillsRequest.Split> paid = new ArrayList<>();
                    for (BillsRequest bill : response.body()) {
                        for (BillsRequest.Split s : bill.getSplits()) {
                            if (s.isPaid() && currentUserId.equals(s.getUserId())) {
                                s.setBillTitle(bill.getTitle());
                                s.setBillId(bill.getId());
                                s.setCreatorId(bill.getCreatorId());
                                paid.add(s);
                            }

                        }
                    }
                    historyAdapter.setData(paid);
                    rvHistory.setAdapter(historyAdapter);

                    boolean show = !paid.isEmpty();
                    tvHistoryHeader.setVisibility(show ? View.VISIBLE : View.GONE);
                    rvHistory.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void onFailure(Call<List<BillsRequest>> call, Throwable t) {}
        });
    }

    private void fetchOwedToMe() {
        ApiClient.getBillsApi().getOwedToMe(currentUserId).enqueue(new Callback<List<OwedToUser>>() {
            @Override
            public void onResponse(Call<List<OwedToUser>> call, Response<List<OwedToUser>> response) {
                if (response.isSuccessful() && response.body() != null) {


                    List<OwedToUser> list = response.body();

                    owedToMeAdapter.setData(list);
                    rvActive.setAdapter(owedToMeAdapter);

                    double total = 0;
                    for (OwedToUser o : list) total += o.getAmountOwed();

                    tvSummary.setText(String.format(Locale.getDefault(), "Total Owed to You: €%.2f", total));

                } else {
                    Toast.makeText(TenantBillsActivity.this, "OwedToMe error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<OwedToUser>> call, Throwable t) {
                Toast.makeText(TenantBillsActivity.this, "OwedToMe failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                t.printStackTrace();
            }
        });
    }


    private void fetchReceivedHistory() {
        ApiClient.getBillsApi().getBillsCreatedBy(currentUserId).enqueue(new Callback<List<BillsRequest>>() {
            @Override
            public void onResponse(Call<List<BillsRequest>> call, Response<List<BillsRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    List<BillsRequest.Split> received = new ArrayList<>();

                    for (BillsRequest bill : response.body()) {
                        if (bill.getSplits() == null) continue;

                        for (BillsRequest.Split s : bill.getSplits()) {
                            if (s.isPaid() && !s.getUserId().equals(currentUserId)) {
                                s.setBillTitle(bill.getTitle());
                                s.setBillId(bill.getId());
                                s.setCreatorId(bill.getCreatorId());
                                received.add(s);
                            }

                        }
                    }

                    historyAdapter.setData(received);
                    rvHistory.setAdapter(historyAdapter);

                    boolean show = !received.isEmpty();
                    tvHistoryHeader.setVisibility(show ? View.VISIBLE : View.GONE);
                    rvHistory.setVisibility(show ? View.VISIBLE : View.GONE);

                } else {
                    Toast.makeText(TenantBillsActivity.this, "Received history error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<BillsRequest>> call, Throwable t) {
                Toast.makeText(TenantBillsActivity.this, "Received history failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                t.printStackTrace();
            }
        });
    }


    // Helper to format date string to "29 Jan 2026"
    public String formatDate(String raw) {
        if (raw == null) return "--";
        try {
            String clean = raw.split("T")[0];
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat out = new SimpleDateFormat("dd MMM yyyy", Locale.US);
            return out.format(in.parse(clean));
        } catch (Exception e) { return raw; }
    }

    private void fetchRoommatesThenBills() {
        ApiClient.getUserApi().getRoommates(houseCode).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    roommateNameMap.clear();
                    for (User u : response.body()) roommateNameMap.put(u.getId(), u.getFullName());
                }
                refreshData();
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) { refreshData(); }
        });
    }

    private void markBillPaid(BillsRequest bill) {
        ApiClient.getBillsApi().markBillPaid(bill.getId(), currentUserId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TenantBillsActivity.this, "Paid!", Toast.LENGTH_SHORT).show();
                    refreshData();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }


    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNavigationView);
        nav.setSelectedItemId(R.id.nav_bills);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_bills) return true;

            if (id == R.id.nav_home) {
                startActivity(new Intent(this, TenantHomeActivity.class));
            } else if (id == R.id.nav_calendar) {
                startActivity(new Intent(this, TenantCalendarActivity.class));
            } else if (id == R.id.nav_tickets) {
                startActivity(new Intent(this, TenantTicketsActivity.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, TenantProfileActivity.class));
            }
            overridePendingTransition(0, 0);
            finish();
            return true;
        });
    }
}