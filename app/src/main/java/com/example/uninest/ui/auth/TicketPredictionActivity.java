package com.example.uninest.ui.auth;

import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.uninest.R;
import com.example.uninest.model.Ticket;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

// --- Retrofit API Interface ---
interface TicketApi {
    @POST("api/tickets/create")
    Call<String> createTicket(@Body Ticket ticket);
}

public class TicketPredictionActivity extends AppCompatActivity {

    private static final String TAG = "TicketPrediction";



    private static final String BASE_URL = "http://127.0.0.1:8080/";

    // UI Components
    private TextView resultTextView;
    private EditText descriptionEditText, buildingEditText, apartmentEditText;
    private Spinner roomSpinner, typeSpinner;

    // ML Components
    private Interpreter tflite;
    private PyObject predictorModule;

    // ML Mappings
    private static final Map<String, Integer> CATEGORY_MAP = new HashMap<String, Integer>() {{
        put("Plumbing", 0); put("Electrical", 1); put("Heating", 2); put("Appliance", 3); put("General", 4);
    }};
    private static final Map<String, Integer> ROOM_MAP = new HashMap<String, Integer>() {{
        put("Kitchen", 0); put("Bathroom", 1); put("Bedroom", 2); put("Living Room", 3); put("Other", 4);
    }};

    // Normalization Constants
    private static final float MU_URGENT = 0.16049382f;
    private static final float SIGMA_URGENT = 0.36706358f;
    private static final float MU_MED = 0.0617284f;
    private static final float SIGMA_MED = 0.2406616f;
    private static final float MU_LOW = 0.16049382f;
    private static final float SIGMA_LOW = 0.36706358f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_prediction);

        initUI();
        initPython();
        initTFLite();
    }

    // ================= UI SETUP =================

    private void initUI() {
        resultTextView = findViewById(R.id.resultText);
        descriptionEditText = findViewById(R.id.descriptionInput);
        buildingEditText = findViewById(R.id.buildingInput);
        apartmentEditText = findViewById(R.id.apartmentInput);
        roomSpinner = findViewById(R.id.roomSpinner);
        typeSpinner = findViewById(R.id.typeSpinner);
        Button predictButton = findViewById(R.id.predictButton);

        setupSpinners();
        predictButton.setOnClickListener(v -> runWorkflow());
    }

    private void setupSpinners() {
        String[] rooms = {"Kitchen", "Bathroom", "Bedroom", "Living Room", "Other"};
        String[] categories = {"Plumbing", "Electrical", "Heating", "Appliance", "General"};

        roomSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, rooms));
        typeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories));
    }

    // ================= WORKFLOW: ML -> BACKEND =================

    private void runWorkflow() {
        final String description = descriptionEditText.getText().toString().trim();
        final String building = buildingEditText.getText().toString().trim();
        final String apartment = apartmentEditText.getText().toString().trim();
        final String room = roomSpinner.getSelectedItem().toString();
        final String category = typeSpinner.getSelectedItem().toString();

        if (description.isEmpty() || building.isEmpty() || apartment.isEmpty()) {
            Toast.makeText(this, "Please fill in all details", Toast.LENGTH_SHORT).show();
            return;
        }

        resultTextView.setText("Analyzing...");

        new Thread(() -> {
            try {
                // 1. Text Preprocessing (Python)
                PyObject pyTokens = predictorModule.callAttr("preprocess_text", description);
                int[] tokens = pyTokens.toJava(int[].class);
                int[][] textInput = new int[1][300];
                for (int i = 0; i < 300; i++) textInput[0][i] = tokens[i];

                // 2. Feature Extraction
                float rawUrgent = countKeywords(description, new String[]{"leak", "flood", "burst", "fire", "gas"});
                float rawMed = countKeywords(description, new String[]{"noise", "flicker", "mildev"});
                float rawLow = countKeywords(description, new String[]{"cosmetic", "minor", "scratch"});

                // 3. Inference (TFLite)
                Object[] inputs = {
                        new int[][]{{CATEGORY_MAP.getOrDefault(category, 0)}},
                        new float[][]{{(rawUrgent - MU_URGENT) / SIGMA_URGENT}},
                        new int[][]{{ROOM_MAP.getOrDefault(room, 0)}},
                        textInput,
                        new float[][]{{(rawMed - MU_MED) / SIGMA_MED}},
                        new float[][]{{(rawLow - MU_LOW) / SIGMA_LOW}}
                };

                float[][] output = new float[1][3];
                Map<Integer, Object> outputs = new HashMap<>();
                outputs.put(0, output);
                tflite.runForMultipleInputsOutputs(inputs, outputs);

                // 4. Map Output to Priority String
                String priority = (output[0][0] > 0.55f) ? "High" : (output[0][2] > 0.60f) ? "Medium" : "Low";

                // 5. Send to Spring Boot Backend
                runOnUiThread(() -> resultTextView.setText("Priority: " + priority + " (Saving...)"));
                sendToBackend(description, building, apartment, room, category, priority);

            } catch (Exception e) {
                Log.e(TAG, "Workflow error", e);
                runOnUiThread(() -> resultTextView.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    private void sendToBackend(String desc, String bld, String apt, String rm, String cat, String prio) {
        Ticket ticket = new Ticket();
        ticket.setDescription(desc);
        ticket.setBuilding(bld);
        ticket.setApartmentId(apt);
        ticket.setRoom(rm);
        ticket.setCategory(cat);
        ticket.setPriority(prio);
        ticket.setStatus("Open");
        ticket.setUserId("android_user_1");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        TicketApi api = retrofit.create(TicketApi.class);
        api.createTicket(ticket).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    resultTextView.setText("Success! Priority: " + prio);
                    Toast.makeText(TicketPredictionActivity.this, "Ticket Created!", Toast.LENGTH_SHORT).show();
                } else {
                    resultTextView.setText("Backend error code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                resultTextView.setText("Network Failure: " + t.getMessage());
            }
        });
    }

    // ================= PYTHON & TFLITE INITIALIZATION =================

    private void initPython() {
        if (!Python.isStarted()) Python.start(new AndroidPlatform(this));
        Python py = Python.getInstance();
        predictorModule = py.getModule("predictor");

        try (InputStream is = getAssets().open("tokenizer.json")) {
            Scanner s = new Scanner(is).useDelimiter("\\A");
            predictorModule.callAttr("load_tokenizer", s.hasNext() ? s.next() : "");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load tokenizer", e);
        }
    }

    private void initTFLite() {
        try {
            tflite = new Interpreter(loadModelFile(), new Interpreter.Options().setAllowFp16PrecisionForFp32(true));
        } catch (Exception e) {
            Log.e(TAG, "TFLite initialization failed", e);
        }
    }

    private MappedByteBuffer loadModelFile() throws Exception {
        AssetFileDescriptor afd = getAssets().openFd("model.tflite");
        FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
        return fis.getChannel().map(FileChannel.MapMode.READ_ONLY, afd.getStartOffset(), afd.getDeclaredLength());
    }

    private int countKeywords(String text, String[] keys) {
        int c = 0;
        String t = text.toLowerCase();
        for (String k : keys) if (t.contains(k)) c++;
        return c;
    }
}