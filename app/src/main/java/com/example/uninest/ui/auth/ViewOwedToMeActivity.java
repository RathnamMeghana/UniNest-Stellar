//package com.example.uninest.ui.auth;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.TextView;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.uninest.R;
//import com.example.uninest.data.api.ApiClient;
//import com.example.uninest.data.api.BillsApi;
//import com.example.uninest.data.api.ApartmentApi;
//import com.example.uninest.model.OwedToUser;
//import com.example.uninest.model.User;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//import java.util.Set;
//
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//
//public class ViewOwedToMeActivity extends AppCompatActivity {
//
//    private RecyclerView rvOwedToMe;
//    private OwedToMeAdapter owedAdapter;
//    private BillsApi billsApi;
//    private ApartmentApi apartmentApi;
//
//    private TextView tvTotalOwedToMe;
//
//    // 🔹 hardcoded for testing
//    private String userId = "BpkiEXWj9kXTjZ04GfFsGtZ2icq1";
//    private String houseCode = "APT-02BF8C3";
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_view_owed_to_me);
//
//        billsApi = ApiClient.getBillsApi();
//        apartmentApi = ApiClient.getApartmentApi();
//
//        tvTotalOwedToMe = findViewById(R.id.tvTotalOwedToMe);
//
//        rvOwedToMe = findViewById(R.id.recyclerViewOwedToMe);
//        rvOwedToMe.setLayoutManager(new LinearLayoutManager(this));
//
//        fetchOwedToMe();
//
//        // ------------------- Navigation Button -------------------
//        findViewById(R.id.btnGoToBills).setOnClickListener(v -> {
//            startActivity(new Intent(ViewOwedToMeActivity.this, ViewBillsActivity.class));
//            finish(); // optional: closes current activity
//        });
//    }
//
//    private void fetchOwedToMe() {
//        billsApi.getOwedToMe(userId).enqueue(new Callback<List<OwedToUser>>() {
//            @Override
//            public void onResponse(Call<List<OwedToUser>> call, Response<List<OwedToUser>> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    List<OwedToUser> owedList = response.body();
//
//                    // ---------------- Total owed ----------------
//                    double total = 0;
//                    for (OwedToUser o : owedList) total += o.getAmountOwed();
//                    tvTotalOwedToMe.setText(String.format(Locale.getDefault(),
//                            "Total owed to you: €%.2f", total));
//
//                    // ---------------- Fetch users in apartment ----------------
//                    apartmentApi.getUsersForApartment(houseCode).enqueue(new Callback<List<User>>() {
//                        @Override
//                        public void onResponse(Call<List<User>> call, Response<List<User>> resp) {
//                            Map<String, String> userIdToEmail = new HashMap<>();
//                            if (resp.isSuccessful() && resp.body() != null) {
//                                for (User u : resp.body()) {
//                                    userIdToEmail.put(u.getId(), u.getEmail());
//                                }
//                            }
//
//                            owedAdapter = new OwedToMeAdapter(owedList, userIdToEmail);
//                            rvOwedToMe.setAdapter(owedAdapter);
//                        }
//
//                        @Override
//                        public void onFailure(Call<List<User>> call, Throwable t) {
//                            Log.e("ViewOwedToMe", "Failed to fetch users: " + t.getMessage());
//                            owedAdapter = new OwedToMeAdapter(owedList, new HashMap<>());
//                            rvOwedToMe.setAdapter(owedAdapter);
//                        }
//                    });
//
//                } else {
//                    Log.e("ViewOwedToMe", "Server error: " + response.code());
//                }
//            }
//
//            @Override
//            public void onFailure(Call<List<OwedToUser>> call, Throwable t) {
//                Log.e("ViewOwedToMe", "Network failure: " + t.getMessage());
//            }
//        });
//    }
//}
