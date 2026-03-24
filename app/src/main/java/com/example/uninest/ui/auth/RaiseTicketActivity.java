package com.example.uninest.ui.auth;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import androidx.core.content.FileProvider;

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
import com.example.uninest.utils.ContactUtils;
import com.google.firebase.auth.FirebaseAuth;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.File;
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

    private String houseCode;
    private String currentBuildingName;
    private String currentUserId;
    private String currentLandlordId;
    private String currentApartmentName;
    private ImageView imgPreview;
    private Uri selectedImageUri;
    private Uri cameraImageUri;
    private String base64Image;

    private static final String[] EMERGENCY_KEYWORDS = {
            "fire", "gas", "smoke", "flood", "explosion", "burst pipe", "danger", "electric shock"
    };

    private TextView resultTextView;
    private EditText descriptionEditText;
    private Spinner roomSpinner;
    private Spinner typeSpinner;
    private Button predictButton;

    private TicketApi ticketApi;
    private ApartmentApi apartmentApi;
    private BuildingApi buildingApi;
    private SessionManager sessionManager;

    private Interpreter tflite;
    private PyObject predictorModule;

    private static final Map<String, Integer> CATEGORY_MAP = new HashMap<String, Integer>() {{
        put("Plumbing", 0);
        put("Electrical", 1);
        put("Heating", 2);
        put("Appliance", 3);
        put("Pest Control", 4);
        put("Safety", 5);
        put("Other", 6);
    }};

    private static final Map<String, Integer> ROOM_MAP = new HashMap<String, Integer>() {{
        put("Kitchen", 0);
        put("Bathroom", 1);
        put("Bedroom", 2);
        put("Living Room", 3);
        put("Unit", 4);
        put("Other", 4);
    }};

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

        ticketApi = ApiClient.getTicketApi();
        apartmentApi = ApiClient.getApartmentApi();
        buildingApi = ApiClient.getBuildingApi();
        sessionManager = new SessionManager(this);
        currentUserId = FirebaseAuth.getInstance().getUid();

        String sessionCode = sessionManager.fetchHouseCode();
        if (sessionCode != null) {
            houseCode = sessionCode;
        } else {
            houseCode = getIntent().getStringExtra("EXTRA_HOUSE_CODE");
        }

        if (houseCode == null || houseCode.isEmpty()) {
            Toast.makeText(this, "Context Error: No House Code found. Relogin.", Toast.LENGTH_LONG).show();
        }

        fetchBuildingContext();
        initUI();
        initPython();
        initTFLite();
    }

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            selectedImageUri = result.getData().getData();
                            renderSelectedImage(selectedImageUri);
                            new Thread(() -> base64Image = convertImageToResizedBase64(selectedImageUri)).start();
                        }
                    });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(),
                    success -> {
                        if (success && cameraImageUri != null) {
                            selectedImageUri = cameraImageUri;
                            renderSelectedImage(selectedImageUri);
                            new Thread(() -> base64Image = convertImageToResizedBase64(selectedImageUri)).start();
                        } else {
                            cameraImageUri = null;
                        }
                    });

    private void fetchBuildingContext() {
        apartmentApi.getAllApartments().enqueue(new Callback<List<Apartment>>() {
            @Override
            public void onResponse(Call<List<Apartment>> call, Response<List<Apartment>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Apartment apartment : response.body()) {
                        if (apartment.getCode() != null && apartment.getCode().equals(houseCode)) {
                            currentLandlordId = apartment.getLandlordId();
                            currentApartmentName = apartment.getName();

                            String buildingId = apartment.getBuildingId();
                            if (buildingId != null) {
                                fetchBuildingName(buildingId);
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
                    Log.e(TAG, "Failed to get building name. Code: " + response.code());
                    currentBuildingName = "Unknown Building";
                }
            }

            @Override
            public void onFailure(Call<Building> call, Throwable t) {
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
        imgPreview = findViewById(R.id.imgTicketPreview);
        Button btnTakePhoto = findViewById(R.id.btnTakePhoto);
        Button btnAttach = findViewById(R.id.btnAttachImage);

        btnTakePhoto.setOnClickListener(v -> launchCameraCapture());

        btnAttach.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        setupSpinners();
        predictButton.setOnClickListener(v -> runHiddenPriorityWorkflow());
    }

    private void setupSpinners() {
        String[] rooms = {"Kitchen", "Bathroom", "Bedroom", "Living Room", "Unit", "Other"};
        String[] categories = {"Plumbing", "Electrical", "Heating", "Appliance", "Pest Control", "Safety", "Other"};

        setSpinnerAdapter(roomSpinner, rooms);
        setSpinnerAdapter(typeSpinner, categories);
    }

    private void setSpinnerAdapter(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, R.layout.item_calendar_spinner_selected, items);
        adapter.setDropDownViewResource(R.layout.item_calendar_spinner_dropdown);
        spinner.setAdapter(adapter);
    }

    private void runHiddenPriorityWorkflow() {
        final String rawDescription = descriptionEditText.getText().toString().trim();
        final String room = roomSpinner.getSelectedItem().toString();
        final String category = typeSpinner.getSelectedItem().toString();
        final String description = rawDescription.isEmpty()
                ? buildFallbackDescription(room, category)
                : rawDescription;

        if (currentLandlordId == null) {
            Toast.makeText(this, "Loading Apartment Info... Please wait a second and try again.", Toast.LENGTH_LONG).show();
            fetchBuildingContext();
            return;
        }

        boolean isEmergency = false;
        String lowerDesc = rawDescription.toLowerCase();
        for (String key : EMERGENCY_KEYWORDS) {
            if (lowerDesc.contains(key)) {
                isEmergency = true;
                break;
            }
        }

        if (isEmergency) {
            showEmergencyWarning(description, room, category);
        } else {
            startProcessingTicket(description, room, category, false);
        }
    }

    private String buildFallbackDescription(String room, String category) {
        return category + " issue reported in " + room + ".";
    }

    private void startProcessingTicket(final String description, final String room, final String category) {
        startProcessingTicket(description, room, category, true);
    }

    private void startProcessingTicket(final String description, final String room,
                                       final String category, final boolean emergencyPromptShown) {
        predictButton.setEnabled(false);
        predictButton.setText("Processing...");

        new Thread(() -> {
            try {
                PyObject pyTokens = predictorModule.callAttr("preprocess_text", description);
                int[] tokens = pyTokens.toJava(int[].class);
                int[][] textInput = new int[1][300];
                for (int i = 0; i < 300; i++) {
                    textInput[0][i] = tokens[i];
                }

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
                if (rawUrgent > 0) {
                    priority = "High";
                }

                String finalBuilding = currentBuildingName != null ? currentBuildingName : "Unknown Building";
                String finalPriority = priority;

                runOnUiThread(() -> {
                    if ("High".equalsIgnoreCase(finalPriority) && !emergencyPromptShown) {
                        showHighPriorityWarning(description, room, category, finalBuilding, finalPriority);
                    } else {
                        sendToBackend(description, finalBuilding, houseCode, room, category, finalPriority);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Workflow error", e);
                runOnUiThread(() -> {
                    Toast.makeText(RaiseTicketActivity.this, "System error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetSubmitButton();
                });
            }
        }).start();
    }

    private void resetSubmitButton() {
        predictButton.setEnabled(true);
        predictButton.setText("Submit Ticket");
    }

    private void showHighPriorityWarning(String description, String room, String category, String building, String priority) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Urgent issue detected")
                .setMessage("This ticket has been marked as high priority.\n\n"
                        + "If anyone is unsafe, call 112 / 999 first. You can also contact the emergency number at "
                        + ContactUtils.EMERGENCY_CONTACT_DISPLAY + ".\n\n"
                        + "Would you like to send the ticket now?")
                .setPositiveButton("Send ticket", (dialog, which) ->
                        sendToBackend(description, building, houseCode, room, category, priority))
                .setNeutralButton("Call emergency contact", (dialog, which) -> {
                    ContactUtils.dialEmergency(this);
                    resetSubmitButton();
                })
                .setNegativeButton("Cancel", (dialog, which) -> resetSubmitButton())
                .setCancelable(false)
                .show();
    }

    private void sendToBackend(String desc, String buildingName, String homeCode, String room, String category, String priority) {
        Ticket ticket = new Ticket();
        ticket.setDescription(desc);
        ticket.setBuilding(buildingName);
        ticket.setApartmentId(homeCode);
        ticket.setRoom(room);
        ticket.setCategory(category);
        ticket.setPriority(priority);
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
                    resetSubmitButton();
                    if (response.isSuccessful()) {
                        Toast.makeText(RaiseTicketActivity.this, "Ticket submitted", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(RaiseTicketActivity.this, "Submission failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                runOnUiThread(() -> {
                    resetSubmitButton();
                    com.example.uninest.utils.NetworkErrorDialog.show(
                            RaiseTicketActivity.this,
                            () -> sendToBackend(desc, buildingName, homeCode, room, category, priority)
                    );
                });
            }
        });
    }

    private void initPython() {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        Python py = Python.getInstance();
        predictorModule = py.getModule("predictor");

        try (InputStream is = getAssets().open("tokenizer.json")) {
            Scanner scanner = new Scanner(is).useDelimiter("\\A");
            predictorModule.callAttr("load_tokenizer", scanner.hasNext() ? scanner.next() : "");
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
        int count = 0;
        String normalized = text.toLowerCase();
        for (String key : keys) {
            if (normalized.contains(key)) {
                count++;
            }
        }
        return count;
    }

    private String convertImageToResizedBase64(Uri uri) {
        try {
            Bitmap original = decodeScaledBitmapFromUri(uri, 1600);
            return convertBitmapToResizedBase64(original);
        } catch (Exception e) {
            Log.e(TAG, "Image conversion failed", e);
            return null;
        }
    }

    private String convertBitmapToResizedBase64(Bitmap original) {
        if (original == null) {
            return null;
        }

        try {
            int maxSize = 1280;
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

            Bitmap scaled = Bitmap.createScaledBitmap(original, width, height, true);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, baos);
            byte[] bytes = baos.toByteArray();

            return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Bitmap conversion failed", e);
            return null;
        }
    }

    private void renderSelectedImage(Object imageSource) {
        imgPreview.setVisibility(ImageView.VISIBLE);
        com.bumptech.glide.Glide.with(this)
                .load(imageSource)
                .fitCenter()
                .into(imgPreview);
    }

    private void launchCameraCapture() {
        try {
            cameraImageUri = createCameraImageUri();
            takePictureLauncher.launch(cameraImageUri);
        } catch (Exception e) {
            Log.e(TAG, "Unable to launch camera capture", e);
            Toast.makeText(this, "Couldn't open the camera right now.", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri createCameraImageUri() throws java.io.IOException {
        File cameraDir = new File(getCacheDir(), "ticket-photos");
        if (!cameraDir.exists() && !cameraDir.mkdirs()) {
            throw new IllegalStateException("Unable to create camera cache directory");
        }

        File imageFile = File.createTempFile("ticket_", ".jpg", cameraDir);
        return FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                imageFile
        );
    }

    private Bitmap decodeScaledBitmapFromUri(Uri uri, int maxDimension) throws Exception {
        BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
        boundsOptions.inJustDecodeBounds = true;
        try (InputStream boundsStream = getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(boundsStream, null, boundsOptions);
        }

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = calculateInSampleSize(boundsOptions, maxDimension, maxDimension);
        try (InputStream decodeStream = getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(decodeStream, null, decodeOptions);
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        while ((height / inSampleSize) > reqHeight || (width / inSampleSize) > reqWidth) {
            inSampleSize *= 2;
        }

        return Math.max(1, inSampleSize);
    }

    private void showEmergencyWarning(final String description, final String room, final String category) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Emergency detected")
                .setMessage("Your description suggests a life-safety issue.\n\n"
                        + "If anyone is unsafe, call 112 / 999 first. You can also call the UniNest emergency number at "
                        + ContactUtils.EMERGENCY_CONTACT_DISPLAY + ".\n\n"
                        + "Would you still like to raise this maintenance ticket?")
                .setPositiveButton("Send ticket", (dialog, which) ->
                        startProcessingTicket(description, room, category))
                .setNeutralButton("Call emergency contact", (dialog, which) -> {
                    ContactUtils.dialEmergency(this);
                    resetSubmitButton();
                })
                .setNegativeButton("Cancel", (dialog, which) -> resetSubmitButton())
                .setCancelable(false)
                .show();
    }
}
