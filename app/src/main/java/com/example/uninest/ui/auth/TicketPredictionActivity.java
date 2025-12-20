package com.example.uninest.ui.auth;

import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.uninest.R;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class TicketPredictionActivity extends AppCompatActivity {

    private static final String TAG = "TicketPrediction";

    // UI
    private TextView resultTextView;
    private EditText descriptionEditText;
    private Spinner roomSpinner, typeSpinner;

    // ML
    private Interpreter tflite;
    private PyObject predictorModule;

    // ===== CATEGORY / ROOM ENCODING =====
    private static final Map<String, Integer> CATEGORY_MAP = new HashMap<String, Integer>() {{
        put("Plumbing", 0);
        put("Electrical", 1);
        put("Heating", 2);
        put("Appliance", 3);
        put("General", 4);
    }};

    private static final Map<String, Integer> ROOM_MAP = new HashMap<String, Integer>() {{
        put("Kitchen", 0);
        put("Bathroom", 1);
        put("Bedroom", 2);
        put("Living Room", 3);
        put("Other", 4);
    }};

    // ===== NORMALIZATION CONSTANTS =====
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

    // ================= UI =================

    private void initUI() {
        resultTextView = findViewById(R.id.resultText);
        descriptionEditText = findViewById(R.id.descriptionInput);
        roomSpinner = findViewById(R.id.roomSpinner);
        typeSpinner = findViewById(R.id.typeSpinner);
        Button predictButton = findViewById(R.id.predictButton);

        setupSpinners();
        predictButton.setOnClickListener(v -> runInference());
    }

    private void setupSpinners() {
        String[] rooms = {"Kitchen", "Bathroom", "Bedroom", "Living Room", "Other"};
        String[] categories = {"Plumbing", "Electrical", "Heating", "Appliance", "General"};

        roomSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, rooms));
        typeSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, categories));
    }

    // ================= PYTHON =================

    private void initPython() {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        Python py = Python.getInstance();
        predictorModule = py.getModule("predictor");

        try (InputStream is = getAssets().open("tokenizer.json")) {
            Scanner s = new Scanner(is).useDelimiter("\\A");
            String jsonStr = s.hasNext() ? s.next() : "";
            predictorModule.callAttr("load_tokenizer", jsonStr);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load tokenizer", e);
        }
    }

    // ================= TFLITE =================

    private void initTFLite() {
        try {
            Interpreter.Options options = new Interpreter.Options();
            options.setAllowFp16PrecisionForFp32(true);

            tflite = new Interpreter(loadModelFile(), options);

            // Log input tensor specs (sanity check)
            for (int i = 0; i < tflite.getInputTensorCount(); i++) {
                Log.d(TAG,
                        "Input " + i + " → " +
                                tflite.getInputTensor(i).name() + " | " +
                                tflite.getInputTensor(i).dataType());
            }

            Log.d(TAG, "TFLite initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "TFLite initialization failed", e);
        }
    }

    private MappedByteBuffer loadModelFile() throws Exception {
        AssetFileDescriptor afd = getAssets().openFd("model.tflite");
        FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
        FileChannel channel = fis.getChannel();
        return channel.map(FileChannel.MapMode.READ_ONLY,
                afd.getStartOffset(), afd.getDeclaredLength());
    }

    // ================= INFERENCE =================

    private void runInference() {
        final String description = descriptionEditText.getText().toString().trim();
        final String room = roomSpinner.getSelectedItem().toString();
        final String category = typeSpinner.getSelectedItem().toString();

        if (description.isEmpty()) {
            resultTextView.setText("Please enter a description");
            return;
        }

        new Thread(() -> {
            try {
                // ---- TEXT ----
                PyObject pyTokens = predictorModule.callAttr(
                        "preprocess_text", description);
                int[] tokens = pyTokens.toJava(int[].class);

                int[][] textInput = new int[1][300];
                for (int i = 0; i < 300; i++) {
                    textInput[0][i] = tokens[i];
                }


                // ---- CATEGORY / ROOM (INT32) ----
                int[][] categoryInput = {{
                        CATEGORY_MAP.getOrDefault(category, 0)
                }};
                int[][] roomInput = {{
                        ROOM_MAP.getOrDefault(room, 0)
                }};

                // ---- KEYWORD FEATURES ----
                float rawUrgent = countKeywords(description,
                        new String[]{"leak", "flood", "burst", "fire", "gas"});
                float rawMed = countKeywords(description,
                        new String[]{"noise", "flicker", "mildew"});
                float rawLow = countKeywords(description,
                        new String[]{"cosmetic", "minor", "scratch"});

                float[][] urgentInput = {{
                        (rawUrgent - MU_URGENT) / SIGMA_URGENT
                }};
                float[][] medInput = {{
                        (rawMed - MU_MED) / SIGMA_MED
                }};
                float[][] lowInput = {{
                        (rawLow - MU_LOW) / SIGMA_LOW
                }};

                // ---- INPUTS (ORDER MATTERS) ----
                Object[] inputs = new Object[6];
                inputs[0] = categoryInput;
                inputs[1] = urgentInput;
                inputs[2] = roomInput;
                inputs[3] = textInput;
                inputs[4] = medInput;
                inputs[5] = lowInput;

                float[][] output = new float[1][3];
                Map<Integer, Object> outputs = new HashMap<>();
                outputs.put(0, output);

                tflite.runForMultipleInputsOutputs(inputs, outputs);

                float high = output[0][0];
                float low = output[0][1];
                float med = output[0][2];

                String priority =
                        high > 0.55f ? "High" :
                                med > 0.60f ? "Medium" : "Low";

                runOnUiThread(() ->
                        resultTextView.setText("Priority: " + priority));

            } catch (Exception e) {
                Log.e(TAG, "Inference error", e);
                runOnUiThread(() ->
                        resultTextView.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    private int countKeywords(String text, String[] keys) {
        int c = 0;
        String t = text.toLowerCase();
        for (String k : keys) if (t.contains(k)) c++;
        return c;
    }
}
