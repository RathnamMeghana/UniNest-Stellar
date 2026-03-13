package com.example.uninest.ui.auth;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.data.api.ApartmentApi;
import com.example.uninest.data.api.BuildingApi;
import com.example.uninest.data.api.TicketApi;
import com.example.uninest.model.Apartment;
import com.example.uninest.model.Building;
import com.example.uninest.model.Ticket;
import com.google.firebase.auth.FirebaseAuth;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RaiseTicketActivity extends AppCompatActivity {

    private static final String TAG = "RaiseTicketActivity";

    // Context Data
    private String houseCode;
    private String currentBuildingName; // Will be set automatically
    private String currentUserId;
    private String currentLandlordId;
    private String currentApartmentName;
    private ImageView imgPreview;
    private Uri selectedImageUri;
    private String base64Image = null; // Default is null (Optional)

    private static final String[] EMERGENCY_KEYWORDS = {"fire", "gas", "smoke", "flood", "explosion",
            "burst pipe", "danger", "electric shock"};

    // UI Components
    private TextView resultTextView;
    private EditText descriptionEditText;
    private Spinner roomSpinner, typeSpinner;
    private Button predictButton;

    // API & Session
    private TicketApi ticketApi;
    private ApartmentApi apartmentApi;
    private BuildingApi buildingApi;
    private SessionManager sessionManager;

    // ML Components
    private Interpreter tflite;
    private PyObject predictorModule;

    // ML Mappings
    private static final Map<String, Integer> CATEGORY_MAP = new HashMap<String, Integer>() {{
        put("Plumbing", 0); put("Electrical", 1); put("Heating", 2); put("Appliance", 3); put("Pest Control",4); put("Safety", 5); put("Other", 6);
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
        setContentView(R.layout.activity_raise_ticket);

        // 1. Initialize APIs and Session
        ticketApi = ApiClient.getTicketApi();
        apartmentApi = ApiClient.getApartmentApi();
        buildingApi = ApiClient.getBuildingApi();
        sessionManager = new SessionManager(this);
        currentUserId = FirebaseAuth.getInstance().getUid();

        // 2. Get House Code
        String sessionCode = sessionManager.fetchHouseCode();
        if (sessionCode != null) {
            houseCode = sessionCode;
        } else {
            houseCode = getIntent().getStringExtra("EXTRA_HOUSE_CODE");
        }

        if (houseCode == null || houseCode.isEmpty()) {
            Toast.makeText(this, "Context Error: No House Code found. Relogin.", Toast.LENGTH_LONG).show();
            // Fallback for testing
            // houseCode = "123";
        }

        // 3. Find Building name
        fetchBuildingContext();

        // 4. Setup UI and ML
        initUI();
        initPython();
        initTFLite();
    }

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            selectedImageUri = result.getData().getData();
                            imgPreview.setVisibility(View.VISIBLE);
                            // Show preview using Glide
                            com.bumptech.glide.Glide.with(this).load(selectedImageUri).into(imgPreview);

                            // Convert in background thread so the UI doesn't freeze
                            new Thread(() -> {
                                base64Image = convertImageToResizedBase64(selectedImageUri);
                            }).start();
                        }
                    });

    private void fetchBuildingContext() {
        // Step 1: Find the Apartment by House Code to get the BuildingID
        apartmentApi.getAllApartments().enqueue(new Callback<List<Apartment>>() {
            @Override
            public void onResponse(Call<List<Apartment>> call, Response<List<Apartment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Apartment a : response.body()) {
                        if (a.getCode() != null && a.getCode().equals(houseCode)) {

                            currentLandlordId = a.getLandlordId();
                            currentApartmentName = a.getName();

                            // get the building id
                            String bId = a.getBuildingId();
                            if(bId != null) {
                                fetchBuildingName(bId);
                            } else {
                                currentBuildingName = "Unknown Building";
                            }
                            return;
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<List<Apartment>> call, Throwable t) {
                Log.e(TAG, "Failed to fetch apartments", t);
            }
        });
    }

    private void fetchBuildingName(String buildingId) {
        if (buildingId == null || buildingId.isEmpty()) {
            currentBuildingName = "Unknown Building";
            return;
        }

        buildingApi.getBuilding(buildingId).enqueue(new Callback<Building>() {
            @Override
            public void onResponse(Call<Building> call, Response<Building> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentBuildingName = response.body().getName();
                    Log.d(TAG, "Real Building Name Fetched: " + currentBuildingName);
                } else {
                    // Endpoint worked but ID wasn't found or returned error
                    Log.e(TAG, "Failed to get building name. Code: " + response.code());
                    currentBuildingName = "Unknown Building";
                }
            }

            @Override
            public void onFailure(Call<Building> call, Throwable t) {
                // Network failure
                Log.e(TAG, "Network error fetching building name", t);
                currentBuildingName = "Unknown Building (Offline)";
            }
        });
    }

    private void initUI() {
        resultTextView = findViewById(R.id.resultText);
        descriptionEditText = findViewById(R.id.descriptionInput);
        roomSpinner = findViewById(R.id.roomSpinner);
        typeSpinner = findViewById(R.id.typeSpinner);
        predictButton = findViewById(R.id.predictButton);
        imgPreview = findViewById(R.id.imgTicketPreview); // Add to XML
        Button btnAttach = findViewById(R.id.btnAttachImage); // Add to XML

        btnAttach.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        // Close Button
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        setupSpinners();
        predictButton.setOnClickListener(v -> runHiddenPriorityWorkflow());
    }

    private void setupSpinners() {
        String[] rooms = {"Kitchen", "Bathroom", "Bedroom", "Living Room", "Unit", "Other"};
        String[] categories = {"Plumbing", "Electrical", "Heating", "Appliance", "Pest Control","Safety", "Other"};

        roomSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, rooms));
        typeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories));
    }

    private void runHiddenPriorityWorkflow() {
        final String description = descriptionEditText.getText().toString().trim();
        final String room = roomSpinner.getSelectedItem().toString();
        final String category = typeSpinner.getSelectedItem().toString();

        if (description.isEmpty()) {
            Toast.makeText(this, "Please describe the issue", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentLandlordId == null) {
            Toast.makeText(this, "Loading Apartment Info... Please wait a second and try again.", Toast.LENGTH_LONG).show();
            // Retry fetching if it failed before
            fetchBuildingContext();
            return;
        }

    }


        private void startProcessingTicket(final String description, final String room,
        final String category){
        // Lock UI
        predictButton.setEnabled(false);
        predictButton.setText("Processing...");

            new Thread(() -> {
                try {
                    // --- ML Logic (Tokenization + Inference) ---
                    PyObject pyTokens = predictorModule.callAttr("preprocess_text", description);
                    int[] tokens = pyTokens.toJava(int[].class);
                    int[][] textInput = new int[1][300];
                    for (int i = 0; i < 300; i++) textInput[0][i] = tokens[i];

                    float rawUrgent = countKeywords(description, new String[]{"flood", "flooding", "fire", "gas", "mouse", "pest", "rat"});
                    float rawMed = countKeywords(description, new String[]{"mildew", "not turning on", "strange noise", "flickering"});
                    float rawLow = countKeywords(description, new String[]{"cosmetic", "minor", "scratch", "loose", "paint", "dripping", "lightbulb"});

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

                    String priority = (output[0][0] > 0.40f) ? "High" : (output[0][2] > 0.50f) ? "Medium" : "Low";
                    if (rawUrgent > 0) priority = "High";

                    // --- Send to Backend using Automatic Context ---
                    // Use the Building Name we found in onCreate
                    String finalBuilding = (currentBuildingName != null) ? currentBuildingName : "Unknown Building";

                    sendToBackend(description, finalBuilding, houseCode, room, category, priority);

                } catch (Exception e) {
                    Log.e(TAG, "Workflow error", e);
                    runOnUiThread(() -> {
                        Toast.makeText(RaiseTicketActivity.this, "System error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        predictButton.setEnabled(true);
                        predictButton.setText("Submit Ticket");
                    });
                }
            }).start();
        }


    private void sendToBackend(String desc, String bld, String houseCode, String rm, String cat, String prio) {
        Ticket ticket = new Ticket();
        ticket.setDescription(desc);
        ticket.setBuilding(bld);
        ticket.setApartmentId(houseCode);
        ticket.setRoom(rm);
        ticket.setCategory(cat);
        ticket.setPriority(prio);      // CALCULATED
        ticket.setPrioritySource("AI");
        ticket.setStatus("Raised");
        ticket.setUserId(currentUserId != null ? currentUserId : "android_user");
        ticket.setUserName(sessionManager.getUserFullName());
        ticket.setLandlordId(currentLandlordId);
        ticket.setApartmentName(currentApartmentName);
        ticket.setImageUrl(base64Image);

        ticketApi.createTicket(ticket).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                runOnUiThread(() -> {
                    predictButton.setEnabled(true);
                    predictButton.setText("Submit Ticket");
                    if (response.isSuccessful()) {
                        Toast.makeText(RaiseTicketActivity.this, "Ticket submitted! Priority: " + prio, Toast.LENGTH_LONG).show();
                        finish(); // Close activity
                    } else {
                        Toast.makeText(RaiseTicketActivity.this, "Submission failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                runOnUiThread(() -> {
                    predictButton.setEnabled(true);
                    predictButton.setText("Submit Ticket");
                    Toast.makeText(RaiseTicketActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }


    // --- ML Init Helpers ---
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

    private String convertImageToResizedBase64(Uri uri) {
        try {
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            android.graphics.Bitmap original = android.graphics.BitmapFactory.decodeStream(is);

            // Resize to max 600px to stay under Firestore document limits
            int maxSize = 600;
            int width = original.getWidth();
            int height = original.getHeight();

            float bitmapRatio = (float) width / (float) height;
            if (bitmapRatio > 1) {
                width = maxSize;
                height = (int) (width / bitmapRatio);
            } else {
                height = maxSize;
                width = (int) (height * bitmapRatio);
            }

            android.graphics.Bitmap scaled = android.graphics.Bitmap.createScaledBitmap(original, width, height, true);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, baos); // 50% quality is good for maintenance photos
            byte[] bytes = baos.toByteArray();

            return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Image conversion failed", e);
            return null;
        }
    }
}