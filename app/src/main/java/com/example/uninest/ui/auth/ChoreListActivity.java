package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.data.api.ApiClient;

import com.example.uninest.data.api.ChoreAPI;
import com.example.uninest.model.Chore;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChoreListActivity extends AppCompatActivity {

    private LinearLayout choreContainer;
    private ChoreAPI choreApi;
    private String houseCode = "APT-E2DE614"; // Your specific house code

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chore_list);

        choreContainer = findViewById(R.id.layoutChoreList);
        TextView tvTitle = findViewById(R.id.tvChoreTitle);
        tvTitle.setText("Chores for " + houseCode);


        choreApi = ApiClient.getChoreApi();

        loadChores();
    }

    private void loadChores() {
        choreContainer.removeAllViews();

        choreApi.getAllChoreByApartment(houseCode).enqueue(new Callback<List<Chore>>() {
            @Override
            public void onResponse(Call<List<Chore>> call, Response<List<Chore>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Chore> chores = response.body();

                    if (chores.isEmpty()) {
                        Toast.makeText(ChoreListActivity.this, "No chores found", Toast.LENGTH_SHORT).show();
                    }

                    for (Chore chore : chores) {
                        displayChoreCard(chore);
                    }
                } else {
                    com.example.uninest.utils.NetworkErrorDialog.show(
                            ChoreListActivity.this,
                            ChoreListActivity.this::loadChores
                    );
                }
            }

            @Override
            public void onFailure(Call<List<Chore>> call, Throwable t) {
                Log.e("ChoreAPI", "Error: " + t.getMessage());
                com.example.uninest.utils.NetworkErrorDialog.show(
                        ChoreListActivity.this,
                        ChoreListActivity.this::loadChores
                );
            }
        });
    }

    private void displayChoreCard(Chore chore) {
        // You can create a custom ChoreCardView or inflate a simple layout
        View cardView = getLayoutInflater().inflate(R.layout.item_chore_card, choreContainer, false);

        TextView tvTaskName = cardView.findViewById(R.id.tvTaskName);
        TextView tvAssignedTo = cardView.findViewById(R.id.tvAssignedTo);
        TextView tvDetails = cardView.findViewById(R.id.tvDetails);

        tvTaskName.setText(chore.getTaskName());
        tvAssignedTo.setText("Assigned to: " + (chore.getAssignedTo() != null ? chore.getAssignedTo() : "Unassigned"));
        tvDetails.setText(String.format("Room: %s | Difficulty: %d", chore.getRoom(), chore.getDifficultyScore()));

        choreContainer.addView(cardView);
    }
}
