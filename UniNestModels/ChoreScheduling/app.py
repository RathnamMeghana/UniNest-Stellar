from flask import Flask, request, jsonify

app = Flask(__name__)

# --------------------------
# PREDICT ENDPOINT
# --------------------------
@app.route("/predict", methods=["POST"])
def predict():
    data = request.get_json()
    if not data:
        return jsonify({"error": "No JSON received"}), 400

    input_data = data.get("input_data")
    if not input_data:
        return jsonify({"error": "Missing input_data"}), 400

    # Dummy prediction logic (replace with your TensorFlow model)
    predictions = [sum(sample) for sample in input_data]  # simple sum of features as example
    return jsonify({"predictions": predictions})


# --------------------------
# CHORES ASSIGN ENDPOINT
# --------------------------
@app.route("/chores/assign", methods=["POST"])
def assign_chores():
    data = request.get_json()
    if not data:
        return jsonify({"error": "No JSON received"}), 400

    roommates = data.get("roommates", [])
    chores = data.get("chores", [])

    if not roommates or not chores:
        return jsonify({"error": "Missing roommates or chores"}), 400

    # Sort roommates by availability and preference descending
    roommates_sorted = sorted(
        roommates,
        key=lambda x: (x["availability_mins"], x["preference_score"]),
        reverse=True
    )

    # Round-robin chore assignment
    assignments = []
    roommate_index = 0
    for chore in chores:
        assigned_roommate = roommates_sorted[roommate_index % len(roommates_sorted)]
        assignments.append({
            "chore_id": chore["id"],
            "chore_name": chore["task_name"],
            "assigned_to_id": assigned_roommate["id"],
            "assigned_to_name": assigned_roommate["name"]
        })
        roommate_index += 1

    return jsonify({"assignments": assignments})


# --------------------------
# RUN SERVER
# --------------------------
if __name__ == "__main__":
    app.run(debug=True, port=5002)
