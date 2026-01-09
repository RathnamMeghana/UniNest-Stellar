package com.example.uninest.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.BuildingApi;
import com.example.uninest.data.api.TicketApi;
import com.example.uninest.model.Building;
import com.example.uninest.model.Ticket;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LettingAgentTicketsActivity extends AppCompatActivity {

    private TicketCardAdapter adapter;
    private TicketApi ticketApi;


    private List<Ticket> allTickets = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letting_agent_tickets);

        ticketApi = ApiClient.getTicketApi();

        EditText etSearch = findViewById(R.id.etSearch);
        RecyclerView rv = findViewById(R.id.rvTickets);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TicketCardAdapter(this, new ArrayList<>(), item -> {
            Toast.makeText(this, "Clicked: " + item.getDescription(), Toast.LENGTH_SHORT).show();
        });

        rv.setAdapter(adapter);

        // LOAD DATA CORRECTLY BY LANDLORD ID
        loadTicketsByLandlordId();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Navigation
        findViewById(R.id.btnFilter).setOnClickListener(v -> {}); // Filter logic later
        findViewById(R.id.navTickets).setOnClickListener(v -> {});
        findViewById(R.id.navApartments).setOnClickListener(v ->
                startActivity(new Intent(this, LettingAgentApartmentsActivity.class))
        );
        findViewById(R.id.navProfile).setOnClickListener(v -> {});
    }

    private void loadTicketsByLandlordId() {
        String myAgentId = FirebaseAuth.getInstance().getUid();

        Log.d("DEBUG_TICKETS", "Agent ID: " + myAgentId);

        if (myAgentId == null) return;

        ticketApi.getTicketsByLandlord(myAgentId).enqueue(new Callback<List<Ticket>>() {
            @Override
            public void onResponse(Call<List<Ticket>> call, Response<List<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allTickets = response.body();

                    Log.d("DEBUG_TICKETS", "Tickets Found: " + allTickets.size());

                    adapter.setData(allTickets);

                    if (allTickets.isEmpty()) {
                        Toast.makeText(LettingAgentTicketsActivity.this, "No tickets found", Toast.LENGTH_SHORT).show();
                    }
                }
                    else {
                        Log.e("DEBUG_TICKETS", "API Fail: " + response.code());
                    }
                }

            @Override
            public void onFailure(Call<List<Ticket>> call, Throwable t) {
                Log.e("DEBUG_TICKETS", "Network Error", t);
                Toast.makeText(LettingAgentTicketsActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}