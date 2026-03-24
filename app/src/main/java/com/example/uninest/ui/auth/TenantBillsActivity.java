package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.BillsRequest;
import com.example.uninest.model.OwedToUser;
import com.example.uninest.model.User;
import com.example.uninest.notifications.LocalNotificationHelper;
import com.example.uninest.utils.NetworkErrorDialog;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
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
    private String highlightMessage;
    private TextView tvSummary, tvSummaryCaption, tvHistoryHeader, tvActiveHeader;
    private TextView tvEmptyTitle, tvEmptyBody;
    private TabLayout tabLayout;
    private SessionManager sessionManager;
    private String currentUserId, houseCode;
    private View emptyStateCard;
    private View layoutHistoryHeader;
    private MaterialButton btnToggleHistory;
    private final Map<String, String> roommateNameMap = new HashMap<>();
    private final List<BillsRequest> activeBills = new ArrayList<>();
    private final List<OwedToUser> owedToMe = new ArrayList<>();
    private final List<BillsRequest.Split> paidHistoryBills = new ArrayList<>();
    private final List<BillsRequest.Split> receivedHistoryBills = new ArrayList<>();
    private boolean isHistoryExpanded = false;
    private boolean activeSectionFailed = false;
    private boolean historySectionFailed = false;
    private int billsLoadToken = 0;
    private int pendingSectionLoads = 0;
    private boolean currentTabLoadHadFailure = false;
    private boolean billsLoadErrorVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_bills);

        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getUserId();
        houseCode = sessionManager.fetchHouseCode();
        highlightBillId = getIntent().getStringExtra("highlight_bill_id");
        highlightMessage = getIntent().getStringExtra(LocalNotificationHelper.EXTRA_HIGHLIGHT_MESSAGE);

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
        tvSummaryCaption = findViewById(R.id.tvSummaryCaption);
        tvActiveHeader = findViewById(R.id.tvActiveHeader);
        tvHistoryHeader = findViewById(R.id.tvHistoryHeader);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptyBody = findViewById(R.id.tvEmptyBody);
        rvActive = findViewById(R.id.rvBillsActive);
        rvHistory = findViewById(R.id.rvBillsHistory);
        emptyStateCard = findViewById(R.id.emptyStateCard);
        layoutHistoryHeader = findViewById(R.id.layoutHistoryHeader);
        btnToggleHistory = findViewById(R.id.btnToggleHistory);

        rvActive.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        rvActive.setNestedScrollingEnabled(false);
        rvHistory.setNestedScrollingEnabled(false);

        activeAdapter = new TenantActiveBillsAdapter(new ArrayList<>(), currentUserId, roommateNameMap, this::markBillPaid);
        historyAdapter = new TenantPaidHistoryAdapter(new ArrayList<>(), roommateNameMap, currentUserId);
        owedToMeAdapter = new TenantOwedToMeAdapter(new ArrayList<>(), roommateNameMap);
        btnToggleHistory.setOnClickListener(v -> {
            isHistoryExpanded = !isHistoryExpanded;
            renderCurrentTabState();
        });

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
        int loadToken = ++billsLoadToken;
        isHistoryExpanded = false;
        activeSectionFailed = false;
        historySectionFailed = false;
        pendingSectionLoads = 2;
        currentTabLoadHadFailure = false;

        if (tabLayout.getSelectedTabPosition() == 0) {
            tvActiveHeader.setText("Bills to Pay");
            tvHistoryHeader.setText("Paid History");
            tvSummaryCaption.setText("Currently Due");
            tvActiveHeader.setTextColor(ContextCompat.getColor(this, R.color.bill_due_text));
            tvHistoryHeader.setTextColor(ContextCompat.getColor(this, R.color.bill_settled_text));
            findViewById(R.id.btnAddBill).setVisibility(View.VISIBLE);
        } else {
            tvActiveHeader.setText("Money Owed to You");
            tvHistoryHeader.setText("Received History");
            tvSummaryCaption.setText("Expected Back");
            tvActiveHeader.setTextColor(ContextCompat.getColor(this, R.color.bill_awaiting_text));
            tvHistoryHeader.setTextColor(ContextCompat.getColor(this, R.color.bill_settled_text));
            findViewById(R.id.btnAddBill).setVisibility(View.GONE);
        }

        updateSummaryFromCurrentData();
        renderCurrentTabState();

        if (isBillsToPayTab()) {
            fetchActiveBills(loadToken);
            fetchPaidHistory(loadToken);
        } else {
            fetchOwedToMe(loadToken);
            fetchReceivedHistory(loadToken);
        }
    }

    private void fetchActiveBills(int loadToken) {
        ApiClient.getBillsApi().getBills(currentUserId).enqueue(new Callback<List<BillsRequest>>() {
            @Override
            public void onResponse(Call<List<BillsRequest>> call, Response<List<BillsRequest>> response) {
                if (loadToken != billsLoadToken) {
                    return;
                }

                if (response.isSuccessful()) {
                    Map<String, BillsRequest> filtered = new HashMap<>();
                    if (response.body() != null) {
                        for (BillsRequest bill : response.body()) {
                            BillsRequest existing = filtered.get(bill.getTitle());
                            if (existing == null || parseBillDateSafe(bill).before(parseBillDateSafe(existing))) {
                                filtered.put(bill.getTitle(), bill);
                            }
                        }
                    }

                    List<BillsRequest> list = new ArrayList<>(filtered.values());
                    Collections.sort(list, Comparator.comparing(TenantBillsActivity.this::parseBillDateSafe));

                    activeSectionFailed = false;
                    activeBills.clear();
                    activeBills.addAll(list);

                    boolean consumedHighlight = false;
                    if (highlightBillId != null && !highlightBillId.isBlank()) {
                        for (int i = 0; i < list.size(); i++) {
                            if (highlightBillId.equals(list.get(i).getId())) {
                                rvActive.scrollToPosition(i);
                                Toast.makeText(
                                        TenantBillsActivity.this,
                                        highlightMessage != null && !highlightMessage.trim().isEmpty()
                                                ? highlightMessage
                                                : "View bills here",
                                        Toast.LENGTH_SHORT
                                ).show();
                                consumedHighlight = true;
                                highlightBillId = null;
                                highlightMessage = null;
                                break;
                            }
                        }
                    }

                    if (!consumedHighlight && highlightMessage != null && !highlightMessage.trim().isEmpty()) {
                        Toast.makeText(TenantBillsActivity.this, highlightMessage, Toast.LENGTH_SHORT).show();
                        highlightBillId = null;
                        highlightMessage = null;
                    }
                } else {
                    activeSectionFailed = true;
                }

                updateSummaryFromCurrentData();
                renderCurrentTabState();
                onSectionLoadFinished(loadToken, activeSectionFailed);
            }

            @Override
            public void onFailure(Call<List<BillsRequest>> call, Throwable t) {
                if (loadToken != billsLoadToken) {
                    return;
                }

                activeSectionFailed = true;
                updateSummaryFromCurrentData();
                renderCurrentTabState();
                onSectionLoadFinished(loadToken, true);
            }
        });
    }

    private void fetchPaidHistory(int loadToken) {
        ApiClient.getBillsApi().getPaidHistory(currentUserId).enqueue(new Callback<List<BillsRequest>>() {
            @Override
            public void onResponse(Call<List<BillsRequest>> call, Response<List<BillsRequest>> response) {
                if (loadToken != billsLoadToken) {
                    return;
                }

                if (response.isSuccessful()) {
                    List<BillsRequest.Split> paid = new ArrayList<>();
                    if (response.body() != null) {
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
                    }
                    historySectionFailed = false;
                    paidHistoryBills.clear();
                    paidHistoryBills.addAll(paid);
                } else {
                    historySectionFailed = true;
                }

                renderCurrentTabState();
                onSectionLoadFinished(loadToken, historySectionFailed);
            }

            @Override
            public void onFailure(Call<List<BillsRequest>> call, Throwable t) {
                if (loadToken != billsLoadToken) {
                    return;
                }

                historySectionFailed = true;
                renderCurrentTabState();
                onSectionLoadFinished(loadToken, true);
            }
        });
    }

    private void fetchOwedToMe(int loadToken) {
        ApiClient.getBillsApi().getOwedToMe(currentUserId).enqueue(new Callback<List<OwedToUser>>() {
            @Override
            public void onResponse(Call<List<OwedToUser>> call, Response<List<OwedToUser>> response) {
                if (loadToken != billsLoadToken) {
                    return;
                }

                if (response.isSuccessful()) {
                    List<OwedToUser> list = response.body() != null
                            ? response.body()
                            : new ArrayList<>();
                    activeSectionFailed = false;
                    owedToMe.clear();
                    owedToMe.addAll(list);
                } else {
                    activeSectionFailed = true;
                }

                updateSummaryFromCurrentData();
                renderCurrentTabState();
                onSectionLoadFinished(loadToken, activeSectionFailed);
            }

            @Override
            public void onFailure(Call<List<OwedToUser>> call, Throwable t) {
                if (loadToken != billsLoadToken) {
                    return;
                }

                activeSectionFailed = true;
                updateSummaryFromCurrentData();
                renderCurrentTabState();
                onSectionLoadFinished(loadToken, true);
            }
        });
    }

    private void fetchReceivedHistory(int loadToken) {
        ApiClient.getBillsApi().getBillsCreatedBy(currentUserId).enqueue(new Callback<List<BillsRequest>>() {
            @Override
            public void onResponse(Call<List<BillsRequest>> call, Response<List<BillsRequest>> response) {
                if (loadToken != billsLoadToken) {
                    return;
                }

                if (response.isSuccessful()) {
                    List<BillsRequest.Split> received = new ArrayList<>();

                    if (response.body() != null) {
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
                    }
                    historySectionFailed = false;
                    receivedHistoryBills.clear();
                    receivedHistoryBills.addAll(received);
                } else {
                    historySectionFailed = true;
                }

                renderCurrentTabState();
                onSectionLoadFinished(loadToken, historySectionFailed);
            }

            @Override
            public void onFailure(Call<List<BillsRequest>> call, Throwable t) {
                if (loadToken != billsLoadToken) {
                    return;
                }

                historySectionFailed = true;
                renderCurrentTabState();
                onSectionLoadFinished(loadToken, true);
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

    private boolean isBillsToPayTab() {
        return tabLayout.getSelectedTabPosition() == 0;
    }

    private void renderCurrentTabState() {
        int activeCount;
        if (isBillsToPayTab()) {
            activeAdapter.setData(new ArrayList<>(activeBills));
            rvActive.setAdapter(activeAdapter);
            activeCount = activeBills.size();
        } else {
            owedToMeAdapter.setData(new ArrayList<>(owedToMe));
            rvActive.setAdapter(owedToMeAdapter);
            activeCount = owedToMe.size();
        }

        List<BillsRequest.Split> currentHistoryBills = getCurrentHistoryBills();
        historyAdapter.setData(new ArrayList<>(currentHistoryBills));
        rvHistory.setAdapter(historyAdapter);

        tvActiveHeader.setVisibility(activeCount > 0 ? View.VISIBLE : View.GONE);
        rvActive.setVisibility(activeCount > 0 ? View.VISIBLE : View.GONE);

        updateHistorySection(currentHistoryBills);
        updateEmptyState(activeCount, currentHistoryBills.size());
    }

    private List<BillsRequest.Split> getCurrentHistoryBills() {
        return isBillsToPayTab() ? paidHistoryBills : receivedHistoryBills;
    }

    private void updateHistorySection(List<BillsRequest.Split> currentHistoryBills) {
        if (currentHistoryBills.isEmpty()) {
            layoutHistoryHeader.setVisibility(View.GONE);
            tvHistoryHeader.setVisibility(View.GONE);
            btnToggleHistory.setVisibility(View.GONE);
            rvHistory.setVisibility(View.GONE);
            return;
        }

        layoutHistoryHeader.setVisibility(View.VISIBLE);
        tvHistoryHeader.setVisibility(View.VISIBLE);
        btnToggleHistory.setVisibility(View.VISIBLE);
        String historyLabel = isBillsToPayTab() ? "paid" : "received";
        btnToggleHistory.setText(isHistoryExpanded
                ? "Hide " + historyLabel + " (" + currentHistoryBills.size() + ")"
                : "Show " + historyLabel + " (" + currentHistoryBills.size() + ")");
        rvHistory.setVisibility(isHistoryExpanded ? View.VISIBLE : View.GONE);
    }

    private void updateEmptyState(int activeCount, int historyCount) {
        boolean showOfflineState = activeCount == 0 && historyCount == 0 && activeSectionFailed && historySectionFailed;
        if (showOfflineState) {
            tvEmptyTitle.setText("You're offline");
            tvEmptyBody.setText("Reconnect to refresh bills and payment history.");
            emptyStateCard.setVisibility(View.VISIBLE);
            return;
        }

        if (activeCount > 0) {
            emptyStateCard.setVisibility(View.GONE);
            return;
        }

        if (isBillsToPayTab()) {
            if (historyCount > 0) {
                tvEmptyTitle.setText("Nothing unpaid right now");
                tvEmptyBody.setText("You're all caught up. You can still open paid history below.");
            } else {
                tvEmptyTitle.setText("No bills to pay");
                tvEmptyBody.setText("When a shared bill is added, it will show up here.");
            }
        } else {
            if (historyCount > 0) {
                tvEmptyTitle.setText("Nobody owes you right now");
                tvEmptyBody.setText("You're caught up. Open received history below if you need it.");
            } else {
                tvEmptyTitle.setText("Nothing owed to you yet");
                tvEmptyBody.setText("Bills you create for roommates will appear here.");
            }
        }
        emptyStateCard.setVisibility(View.VISIBLE);
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

    private void updateSummaryFromCurrentData() {
        double total = 0.0;
        if (isBillsToPayTab()) {
            for (BillsRequest bill : activeBills) {
                if (bill == null || bill.getSplits() == null) {
                    continue;
                }
                for (BillsRequest.Split split : bill.getSplits()) {
                    if (split != null && currentUserId.equals(split.getUserId())) {
                        total += split.getAmountOwed();
                    }
                }
            }
        } else {
            for (OwedToUser owedToUser : owedToMe) {
                if (owedToUser != null) {
                    total += owedToUser.getAmountOwed();
                }
            }
        }
        tvSummary.setText(String.format(Locale.getDefault(), "\u20AC%.2f", total));
    }

    private void onSectionLoadFinished(int loadToken, boolean failed) {
        if (loadToken != billsLoadToken) {
            return;
        }

        currentTabLoadHadFailure = currentTabLoadHadFailure || failed;
        pendingSectionLoads = Math.max(0, pendingSectionLoads - 1);
        if (pendingSectionLoads > 0) {
            return;
        }

        if (!currentTabLoadHadFailure) {
            billsLoadErrorVisible = false;
            NetworkErrorDialog.dismiss(this);
            return;
        }

        if (billsLoadErrorVisible || isFinishing() || isDestroyed()) {
            return;
        }

        billsLoadErrorVisible = true;
        NetworkErrorDialog.show(
                this,
                "Something went wrong",
                "Check your internet connection and try again.",
                () -> {
                    billsLoadErrorVisible = false;
                    refreshData();
                }
        );
    }

    private void markBillPaid(BillsRequest bill) {
        ApiClient.getBillsApi().markBillPaid(bill.getId(), currentUserId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(TenantBillsActivity.this, "Paid!", Toast.LENGTH_SHORT).show();
                    refreshData();
                } else {
                    Toast.makeText(TenantBillsActivity.this, "We couldn't mark that bill as paid just yet.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                NetworkErrorDialog.show(
                        TenantBillsActivity.this,
                        "Couldn't update that bill",
                        "Check your connection and try again.",
                        () -> markBillPaid(bill)
                );
            }
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
