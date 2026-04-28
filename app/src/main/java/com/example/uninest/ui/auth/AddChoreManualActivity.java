package com.example.uninest.ui.auth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.ChoreAPI;
import com.example.uninest.model.Chore;
import com.example.uninest.utils.NetworkErrorDialog;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddChoreManualActivity extends AppCompatActivity {

    private EditText etTaskName, etRoom, etUserEmail, etDifficulty,etDuration, etFrequency;
    private String houseCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_chore_manual);

        houseCode = getIntent().getStringExtra("HOUSE_CODE");

        // Safety check: if no house code was passed, we can't proceed
        if (houseCode == null || houseCode.isEmpty()) {
            Toast.makeText(this, "Error: House Code missing!", Toast.LENGTH_SHORT).show();
            finish(); // Close activity
            return;
        }


        etTaskName = findViewById(R.id.etTaskName);
        etRoom = findViewById(R.id.etRoom);
        etUserEmail = findViewById(R.id.etUserEmail);
        etDifficulty = findViewById(R.id.etDifficulty);
        etDuration = findViewById(R.id.etDuration);
        etFrequency = findViewById(R.id.etFrequency);

        Button btnSave = findViewById(R.id.btnSaveChore);

        btnSave.setOnClickListener(v -> saveChore());
    }

    private void saveChore() {
        String task = etTaskName.getText().toString();
        String room = etRoom.getText().toString();
        String email = etUserEmail.getText().toString();
        int diff = Integer.parseInt(etDifficulty.getText().toString());
        int duration = Integer.parseInt(etDuration.getText().toString());
        int frequency = Integer.parseInt(etFrequency.getText().toString());

        Chore chore = new Chore();
        chore.setTaskName(task);
        chore.setRoom(room);
        chore.setDifficultyScore(diff);
        chore.setEstDurationMin(duration);     // NEW
        chore.setFrequencyPerWeek(frequency);

        ApiClient.getChoreApi().addWithAssignment(houseCode, email, chore).enqueue(new Callback<Chore>() {
            @Override
            public void onResponse(Call<Chore> call, Response<Chore> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddChoreManualActivity.this, "Chore Assigned Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddChoreManualActivity.this, "Error: User not found or invalid data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Chore> call, Throwable t) {
                NetworkErrorDialog.show(
                        AddChoreManualActivity.this,
                        AddChoreManualActivity.this::saveChore
                );
            }
        });
    }
}
