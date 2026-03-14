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
import java.util.Collections;
import java.util.Comparator;
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
    private String highlightBillId;
    private TextView tvSummary, tvHistoryHeader, tvActiveHeader, tvBillsInfo;
    private TabLayout tabLayout;
    private SessionManager sessionManager;
    private String currentUserId, houseCode;
    private final Map<String, String> roommateNameMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_bills);

        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();
        houseCode = sessionManager.fetchHouseCode();
        highlightBillId = getIntent().getStringExtra("highlight_bill_id");

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
        tvBillsInfo = findViewById(R.id.tvBillsInfo);

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

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        findViewById(R.id.btnAddBill).setOnClickListener(v ->
                startActivity(new Intent(this, CreateTenantBillActivity.class)));
    }

    private void refreshData() {
        if (tabLayout.getSelectedTabPosition() == 0) {
            tvActiveHeader.setText("Bills to Pay");
            tvHistoryHeader.setText("Paid History");
            findViewById(R.id.btnAddBill).setVisibility(View.VISIBLE);
            tvBillsInfo.setText("Only bills that involve you appear here, so other housemates do not see unrelated personal charges.");
            fetchActiveBills();
            fetchPaidHistory();
        } else {
            tvActiveHeader.setText("Money Owed to You");
            tvHistoryHeader.setText("Received History");
            findViewById(R.id.btnAddBill).setVisibility(View.GONE);
            tvBillsInfo.setText("Track shared bills you created, who still owes you, and what has already been paid back.");
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
                    for (BillsRequest bill : response.body()) {
                        BillsRequest existing = filtered.get(bill.getTitle());
                        if (existing == null || parseBillDateSafe(bill).before(parseBillDateSafe(existing))) {
                            filtered.put(bill.getTitle(), bill);
                        }
                    }

                    List<BillsRequest> list = new ArrayList<>(filtered.values());
                    Collections.sort(list, Comparator.comparing(TenantBillsActivity.this::parseBillDateSafe));

                    activeAdapter.setData(list);
                    rvActive.setAdapter(activeAdapter);

                    if (highlightBillId != null && !highlightBillId.isBlank()) {
                        for (int i = 0; i < list.size(); i++) {
                            if (highlightBillId.equals(list.get(i).getId())) {
                                rvActive.scrollToPosition(i);
                                Toast.makeText(TenantBillsActivity.this, "Opened related bill", Toast.LENGTH_SHORT).show();
                                break;
                            }
                        }
                    }

                    double total = 0;
                    for (BillsRequest bill : list) {
                        if (bill.getSplits() == null) {
                            continue;
                        }
                        for (BillsRequest.Split split : bill.getSplits()) {
                            if (split.getUserId().equals(currentUserId)) {
                                total += split.getAmountOwed();
                            }
                        }
                    }

                    tvSummary.setText(String.format(Locale.getDefault(), "Total you owe: \u20AC%.2f", total));
                }
            }

            @Override
            public void onFailure(Call<List<BillsRequest>> call, Throwable t) {}
        });
    }

    private void fetchPaidHistory() {
        ApiClient.getBillsApi().getPaidHistory(currentUserId).enqueue(new Callback<List<BillsRequest>>() {
            @Override
            public void onResponse(Call<List<BillsRequest>> call, Response<List<BillsRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<BillsRequest.Split> paid = new ArrayList<>();
                    for (BillsRequest bill : response.body()) {
                        if (bill.getSplits() == null) {
                            continue;
                        }
                        for (BillsRequest.Split split : bill.getSplits()) {
                            if (split.isPaid() && currentUserId.equals(split.getUserId())) {
                                split.setBillTitle(bill.getTitle());
                                split.setBillId(bill.getId());
                                split.setCreatorId(bill.getCreatorId());
                                paid.add(split);
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

            @Override
            public void onFailure(Call<List<BillsRequest>> call, Throwable t) {}
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
                    for (OwedToUser owedToUser : list) {
                        total += owedToUser.getAmountOwed();
                    }

                    tvSummary.setText(String.format(Locale.getDefault(), "Total owed to you: \u20AC%.2f", total));
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
                        if (bill.getSplits() == null) {
                            continue;
                        }

                        for (BillsRequest.Split split : bill.getSplits()) {
                            if (split.isPaid() && !split.getUserId().equals(currentUserId)) {
                                split.setBillTitle(bill.getTitle());
                                split.setBillId(bill.getId());
                                split.setCreatorId(bill.getCreatorId());
                                received.add(split);
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

    public String formatDate(String raw) {
        Date parsed = parseBillDate(raw);
        if (parsed == null) {
            return raw == null ? "--" : raw;
        }
        return new SimpleDateFormat("dd MMM yyyy", Locale.US).format(parsed);
    }

    private Date parseBillDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd"
        };

        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern, Locale.US).parse(raw);
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private Date parseBillDateSafe(BillsRequest request) {
        Date parsed = parseBillDate(request != null ? request.getDueDate() : null);
        return parsed != null ? parsed : new Date(Long.MAX_VALUE);
    }

    private void fetchRoommatesThenBills() {
        ApiClient.getUserApi().getRoommates(houseCode).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    roommateNameMap.clear();
                    for (User user : response.body()) {
                        roommateNameMap.put(user.getId(), user.getFullName());
                    }
                }
                refreshData();
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                refreshData();
            }
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

            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
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
