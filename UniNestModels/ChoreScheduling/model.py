import tensorflow as tf
from tensorflow.keras import layers, models
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
import pickle
import os
import shutil
# ---------------- CONFIG ----------------
EPOCHS = 30
BATCH_SIZE = 16
LEARNING_RATE = 0.001
DATA_PATH = "choreData.csv"




# ---------------- CLEAN OLD FILES ----------------
if os.path.exists("encoders.pkl"):
    os.remove("encoders.pkl")

if os.path.exists("chore_model"):
    shutil.rmtree("chore_model")

# ---------------- LOAD DATA ----------------
data = pd.read_csv(DATA_PATH)

data.columns = data.columns.str.strip()
data = data.apply(lambda x: x.str.strip() if x.dtype == "object" else x)

# ---------------- ENCODE CATEGORICAL DATA ----------------
roommate_encoder = LabelEncoder()
task_encoder = LabelEncoder()
room_encoder = LabelEncoder()

data["assigned_to_enc"] = roommate_encoder.fit_transform(data["assigned_to"])
data["task_name_enc"] = task_encoder.fit_transform(data["task_name"])
data["room_enc"] = room_encoder.fit_transform(data["Room"])

# ---------------- SANITY CHECKS ----------------
print("Unique assigned_to:", sorted(data["assigned_to"].unique()))
print("Encoded labels:", np.unique(data["assigned_to_enc"]))
print("Roommate classes:", roommate_encoder.classes_)

NUM_ROOMMATES = data["assigned_to_enc"].nunique()

assert data["assigned_to_enc"].min() == 0
assert data["assigned_to_enc"].max() == NUM_ROOMMATES - 1

# ---------------- FEATURES ----------------
X_numeric = data[
    [
        "difficulty_score",
        "est_duration_min",
        "frequency_per_week",
        "roommate_preference",
        "availability_mins",
    ]
].values.astype("float32")

X_task = data["task_name_enc"].values.astype("int32")
X_room = data["room_enc"].values.astype("int32")

# ---------------- TARGET ----------------
y = data["assigned_to_enc"].values.astype("int32")

# ---------------- TRAIN / TEST SPLIT ----------------
(
    X_num_train,
    X_num_test,
    X_task_train,
    X_task_test,
    X_room_train,
    X_room_test,
    y_train,
    y_test,
) = train_test_split(
    X_numeric, X_task, X_room, y,
    test_size=0.2,
    random_state=42
)


# ---------------- MODEL ----------------
num_input = layers.Input(shape=(X_num_train.shape[1],), name="numeric_input")

task_input = layers.Input(shape=(1,), name="task_input")
task_emb = layers.Embedding(
    input_dim=len(task_encoder.classes_), output_dim=8
)(task_input)
task_emb = layers.Flatten()(task_emb)

room_input = layers.Input(shape=(1,), name="room_input")
room_emb = layers.Embedding(
    input_dim=len(room_encoder.classes_), output_dim=4
)(room_input)
room_emb = layers.Flatten()(room_emb)

combined = layers.Concatenate()([num_input, task_emb, room_emb])

x = layers.Dense(64, activation="relu")(combined)
x = layers.Dropout(0.3)(x)
x = layers.Dense(32, activation="relu")(x)

output = layers.Dense(
    NUM_ROOMMATES,
    activation="softmax",
    name="roommate_output",
)(x)

model = models.Model(
    inputs=[num_input, task_input, room_input],
    outputs=output,
)

model.compile(
    optimizer=tf.keras.optimizers.Adam(learning_rate=LEARNING_RATE),
    loss="sparse_categorical_crossentropy",
    metrics=["accuracy"],
)

model.summary()

# ---------------- TRAIN ----------------
model.fit(
    {
        "numeric_input": X_num_train,
        "task_input": X_task_train,
        "room_input": X_room_train,
    },
    y_train,
    epochs=EPOCHS,
    batch_size=BATCH_SIZE,
    validation_split=0.1,
)

# ---------------- EVALUATE ----------------
loss, acc = model.evaluate(
    {
        "numeric_input": X_num_test,
        "task_input": X_task_test,
        "room_input": X_room_test,
    },
    y_test,
)

print(f"Test Accuracy: {acc:.2f}")

# ---------------- SAVE MODEL ----------------
model.save("chore_model.keras")


encoders = {
    "assigned_to": roommate_encoder,
    "task_name": task_encoder,
    "room": room_encoder
}

with open("encoders.pkl", "wb") as f:
    pickle.dump(encoders, f)


print("Model saved to 'chore_model'")

