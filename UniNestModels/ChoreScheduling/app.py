import numpy as np
import pickle
from flask import Flask, request, jsonify

# --------------------------
# APP SETUP
# --------------------------
app = Flask(__name__)

# Note: Using the .pkl file we saved from the Random Forest script
MODEL_DATA_PATH = "stable_chore_rf_model.pkl"

# --------------------------
# LOAD MODEL & ENCODERS
# --------------------------
with open(MODEL_DATA_PATH, "rb") as f:
    payload = pickle.load(f)

# Extract everything from the saved dictionary
rf_model = payload["model"]
encoders = payload["encoders"]
scaler = encoders["scaler"]
roommate_encoder = encoders["assigned_to"]
task_encoder = encoders["task_name"]
room_encoder = encoders["room"]

print("Random Forest Model and Encoders loaded successfully.")

# --------------------------
# HELPERS
# --------------------------
def get_field(data, *names, default=None):
    for name in names:
        if name in data:
            return data[name]
    return default

def safe_encode(value, encoder):
    # If it's a new chore name we haven't seen, default to the first class
    if value not in encoder.classes_:
        return 0
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
        # 1. Extract raw inputs
        diff = float(get_field(data, "difficulty_score", "difficultyScore", default=3))
        dur = float(get_field(data, "est_duration_min", "estDurationMin", default=30))
        freq = float(get_field(data, "frequency_per_week", "frequencyPerWeek", default=1))
        pref = float(get_field(data, "roommate_preference", "preferenceScore", default=0.5))
        avail = float(get_field(data, "availability_mins", "availabilityMins", default=120))
        
        task_name = get_field(data, "task_name", "taskName")
        room_name = get_field(data, "room")

        # 2. CALCULATE ENGINEERED FEATURES (Must match training exactly)
        total_effort = dur * freq
        workload_ratio = total_effort / (avail + 1)
        difficulty_density = diff / (dur + 1)

        # 3. SCALE NUMERIC INPUTS
        # Must be in the exact order as training: 
        # [diff, dur, freq, pref, avail, total_effort, workload_ratio, difficulty_density]
        numeric_features = np.array([[
            diff, dur, freq, pref, avail, 
            total_effort, workload_ratio, difficulty_density
        ]])
        numeric_scaled = scaler.transform(numeric_features)

        # 4. ENCODE CATEGORICAL INPUTS
        task_enc = safe_encode(task_name, task_encoder)
        room_enc = safe_encode(room_name, room_encoder)

        # 5. COMBINE FOR RANDOM FOREST
        # X_final = [scaled_numerics..., task_id, room_id]
        X_final = np.hstack([numeric_scaled, [[task_enc, room_enc]]])

        # 6. PREDICT
        prediction = rf_model.predict(X_final)
        probabilities = rf_model.predict_proba(X_final)
        
        idx = int(prediction[0])
        confidence = float(np.max(probabilities))

        assigned_to = str(roommate_encoder.inverse_transform([idx])[0])

        return jsonify({
            "assigned_to": assigned_to,
            "confidence": round(confidence, 3),
            "features_used": {
                "workload_ratio": round(workload_ratio, 4),
                "total_effort": total_effort
            }
        })

    except Exception as e:
        import traceback
        print(traceback.format_exc())
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5002, debug=True)