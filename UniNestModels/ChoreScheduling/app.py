import tensorflow as tf
import numpy as np
import pickle
from flask import Flask, request, jsonify

# --------------------------
# APP SETUP
# --------------------------
app = Flask(__name__)

MODEL_PATH = "chore_model.keras"
ENCODER_PATH = "encoders.pkl"


# --------------------------
# LOAD MODEL
# --------------------------
model = tf.keras.models.load_model(MODEL_PATH)

# --------------------------
# LOAD ENCODERS
# --------------------------
with open(ENCODER_PATH, "rb") as f:
    encoders = pickle.load(f)



def get_encoder(possible_keys):
    for key in possible_keys:
        if key in encoders:
            return encoders[key]
    raise RuntimeError(
        f"Missing encoder. Expected one of {possible_keys}. "
        f"Found: {list(encoders.keys())}"
    )

roommate_encoder = get_encoder(["roommate", "assigned_to"])
task_encoder = get_encoder(["task", "task_name"])
room_encoder = get_encoder(["room", "Room"])

print("Encoders loaded:", list(encoders.keys()))

# --------------------------
# FIELD NORMALIZATION
# --------------------------
def get_field(data, *names, default=None):
    for name in names:
        if name in data:
            return data[name]
    return default

# --------------------------
# SAFE ENCODING
# --------------------------
def safe_encode(value, encoder):
    if value is None or value not in encoder.classes_:
        return int(0)  # fallback class
    return int(encoder.transform([value])[0])

# --------------------------
# PREDICT ENDPOINT
# --------------------------
@app.route("/predict", methods=["POST"])
def predict():
    data = request.get_json()
    if not data:
        return jsonify({"error": "No JSON received"}), 400

    try:
        # ---- Numeric features (defaults included) ----
        numeric_input = np.array([[
            float(get_field(data, "difficulty_score", "difficultyScore", default=3)),
            float(get_field(data, "est_duration_min", "estDurationMin", default=30)),
            float(get_field(data, "frequency_per_week", "frequencyPerWeek", default=1)),
            float(get_field(data, "roommate_preference", "roommatePreference", "preferenceScore", default=0.5)),
            float(get_field(data, "availability_mins", "availabilityMins", default=120)),
        ]], dtype="float32")



        # ---- Categorical features ----
        task_name = get_field(data, "task_name", "taskName")
        room_name = get_field(data, "room")

        task_enc = safe_encode(task_name, task_encoder)
        room_enc = safe_encode(room_name, room_encoder)

        # ---- Predict ----
        preds = model.predict(
            {
                "numeric_input": numeric_input,
                "task_input": np.array([task_enc]),
                "room_input": np.array([room_enc]),
            },
            verbose=0
        )

        idx = int(np.argmax(preds))
        confidence = float(np.max(preds))

        assigned_to = str(roommate_encoder.inverse_transform([idx])[0])

        return jsonify({
            "assigned_to": assigned_to,
            "confidence": round(confidence, 3)
        })

    except Exception as e:
        return jsonify({"error": str(e)}), 500

# --------------------------
# HEALTH CHECK
# --------------------------
@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})

# --------------------------
# RUN SERVER
# --------------------------
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5002, debug=True)
