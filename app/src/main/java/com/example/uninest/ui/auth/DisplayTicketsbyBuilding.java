package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.model.Ticket;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import com.example.uninest.data.api.TicketApi;
import com.example.uninest.model.Ticket;

public class DisplayTicketsbyBuilding extends AppCompatActivity {

    private static final String BASE_URL = "http://127.0.0.1:8080/";
    private TextView textTickets;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_tickets_by_building);

        textTickets = findViewById(R.id.textTickets);

        //String building = getIntent().getStringExtra("EXTRA_BUILDING");

        String building = "blue Building";
        //if (building == null) {
        //    Toast.makeText(this, "Building not provided", Toast.LENGTH_LONG).show();
        //    finish();
        //    return;
        //}

        loadTickets(building);
    }

    private void loadTickets(String building) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        TicketApi api = retrofit.create(TicketApi.class);

        api.getTicketsByBuilding(building).enqueue(new Callback<List<Ticket>>() {
            @Override
            public void onResponse(Call<List<Ticket>> call, Response<List<Ticket>> response) {
                if (!response.isSuccessful()) {
                    textTickets.setText("Error: " + response.code());
                    return;
                }

                List<Ticket> tickets = response.body();
                if (tickets == null || tickets.isEmpty()) {
                    textTickets.setText("No tickets found for this building.");
                    return;
                }


                StringBuilder sb = new StringBuilder();

                for (Ticket t : tickets) {
                    sb.append("Description: ").append(t.getDescription()).append("\n")
                            .append("Priority: ").append(t.getPriority()).append("\n")
                            .append("Category: ").append(t.getCategory()).append("\n")
                            .append("Room: ").append(t.getRoom()).append("\n")
                            .append("Status: ").append(t.getStatus()).append("\n");
                }

                textTickets.setText(sb.toString());
            }

            @Override
            public void onFailure(Call<List<Ticket>> call, Throwable t) {
                textTickets.setText("Network error occurred.");
            }
        });
    }
}
