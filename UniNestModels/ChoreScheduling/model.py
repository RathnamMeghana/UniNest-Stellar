import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder, StandardScaler
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, classification_report
import pickle

# ---------------- CONFIG ----------------
DATA_PATH = "choreData.csv"

# ---------------- LOAD & SHUFFLE ----------------
data = pd.read_csv(DATA_PATH)
data.columns = data.columns.str.strip()

# SHUFFLE immediately to ensure a fair split
data = data.sample(frac=1, random_state=42).reset_index(drop=True)

# ---------------- FEATURE ENGINEERING ----------------
# Re-applying the logic that helps the model understand the "cost" of a chore
data['total_effort'] = data['est_duration_min'] * data['frequency_per_week']
data['workload_ratio'] = data['total_effort'] / (data['availability_mins'] + 1)
data['difficulty_density'] = data['difficulty_score'] / (data['est_duration_min'] + 1)

# ---------------- ENCODING ----------------
roommate_encoder = LabelEncoder()
task_encoder = LabelEncoder()
room_encoder = LabelEncoder()

data["assigned_to_enc"] = roommate_encoder.fit_transform(data["assigned_to"])
data["task_name_enc"] = task_encoder.fit_transform(data["task_name"])
data["room_enc"] = room_encoder.fit_transform(data["Room"])

# ---------------- PREPARE FEATURES ----------------
# Numeric Features
feature_cols = [
    "difficulty_score", "est_duration_min", "frequency_per_week", 
    "roommate_preference", "availability_mins", "total_effort",
    "workload_ratio", "difficulty_density"
]

X_numeric = data[feature_cols].values.astype("float32")

# Scale numeric data
scaler = StandardScaler()
X_numeric_scaled = scaler.fit_transform(X_numeric)

# Combine Numeric + Categorical for the Random Forest
# RF doesn't need embeddings, just the encoded integers
X_categorical = data[["task_name_enc", "room_enc"]].values
X_final = np.hstack([X_numeric_scaled, X_categorical])
y = data["assigned_to_enc"].values

# ---------------- SPLIT ----------------
X_train, X_test, y_train, y_test = train_test_split(
    X_final, y, test_size=0.2, random_state=42
)

# ---------------- TRAIN RANDOM FOREST ----------------
# We use 'balanced' class weights to handle any roommate chore-load gaps
rf_model = RandomForestClassifier(
    n_estimators=200, 
    max_depth=15, 
    min_samples_split=2,
    random_state=42,
    class_weight='balanced'
)

rf_model.fit(X_train, y_train)

# ---------------- EVALUATE ----------------
y_pred = rf_model.predict(X_test)
acc = accuracy_score(y_test, y_pred)

print(f" Random Forest Results")
print(f"Final Test Accuracy: {acc:.2f}")

unique_labels = np.unique(np.concatenate((y_test, y_pred)))
target_names = roommate_encoder.inverse_transform(unique_labels).astype(str)

print("\nDetailed Performance per Roommate:")
print(classification_report(y_test, y_pred, labels=unique_labels, target_names=target_names))

# ---------------- FEATURE IMPORTANCE ----------------
# See what the model actually cares about
all_col_names = feature_cols + ["task_name", "room"]
importances = rf_model.feature_importances_
importance_df = pd.DataFrame({"Feature": all_col_names, "Importance": importances})
print("\nFeature Importance (What the AI is looking at):")
print(importance_df.sort_values(by="Importance", ascending=False))

# ---------------- SAVE ----------------
model_data = {
    "model": rf_model,
    "encoders": {
        "assigned_to": roommate_encoder,
        "task_name": task_encoder,
        "room": room_encoder,
        "scaler": scaler
    },
    "feature_names": feature_cols
}

with open("stable_chore_rf_model.pkl", "wb") as f:
    pickle.dump(model_data, f)

print("\nModel and Encoders saved to: stable_chore_rf_model.pkl")