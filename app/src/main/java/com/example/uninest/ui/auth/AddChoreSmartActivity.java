package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.Chore;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddChoreSmartActivity extends AppCompatActivity {

    private EditText etTaskName, etRoom, etDifficulty, etDurationSmart, etFrequencySmart;
    private String houseCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_chore_smart);

        houseCode = getIntent().getStringExtra("HOUSE_CODE");

        // Safety check: if no house code was passed, we can't proceed
        if (houseCode == null || houseCode.isEmpty()) {
            Toast.makeText(this, "Error: House Code missing!", Toast.LENGTH_SHORT).show();
            finish(); // Close activity
            return;
        }


        etTaskName = findViewById(R.id.etTaskNameSmart);
        etRoom = findViewById(R.id.etRoomSmart);
        etDifficulty = findViewById(R.id.etDifficultySmart);
        etDurationSmart = findViewById(R.id.etDurationSmart);
        etFrequencySmart = findViewById(R.id.etFrequencySmart);
        Button btnSave = findViewById(R.id.btnSaveChoreSmart);

        btnSave.setOnClickListener(v -> saveChoreSmart());
    }

    private void saveChoreSmart() {
        String task = etTaskName.getText().toString();
        String room = etRoom.getText().toString();
        int diff = Integer.parseInt(etDifficulty.getText().toString());
        int duration = Integer.parseInt(etDurationSmart.getText().toString());
        int frequency = Integer.parseInt(etFrequencySmart.getText().toString());

        Chore chore = new Chore();
        chore.setTaskName(task);
        chore.setRoom(room);
        chore.setDifficultyScore(diff);
        chore.setEstDurationMin(duration);
        chore.setFrequencyPerWeek(frequency);


        // Calling the Smart Assign endpoint (No email needed in parameters)
        ApiClient.getChoreApi().addWithSmartAssign(houseCode, chore).enqueue(new Callback<Chore>() {
            @Override
            public void onResponse(Call<Chore> call, Response<Chore> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Chore assigned = response.body();
                    Toast.makeText(AddChoreSmartActivity.this,
                            "AI Assigned to: " + assigned.getAssignedTo(), Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(AddChoreSmartActivity.this, "Smart assignment failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Chore> call, Throwable t) {
                Toast.makeText(AddChoreSmartActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}