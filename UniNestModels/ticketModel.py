import pickle
import tensorflow as tf
from tensorflow.keras import layers, models
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder, OneHotEncoder
from tensorflow.keras.preprocessing.text import Tokenizer
from tensorflow.keras.preprocessing.sequence import pad_sequences
import pickle


# --- Load Data ---
data = pd.read_csv("apartment_issues_3000.csv")

# --- Process Text Data ---
texts = data["IssueDescription"].values 

tokenizer = Tokenizer(num_words=10000, oov_token="<OOV>")
tokenizer.fit_on_texts(texts)
sequences = tokenizer.texts_to_sequences(texts)

# Save tokenizer
with open("tokenizer.pkl", "wb") as f:
    pickle.dump(tokenizer, f)

max_len = 100
X_text = pad_sequences(sequences, maxlen=max_len, padding='post')

# ---  Encode Categories (X_cat) ---
# Use ApartmentNumber and Status as categorical inputs
cat_features = data[["ApartmentNumber", "Status"]].astype(str)
cat_encoder = OneHotEncoder(sparse_output=False)
X_cat = cat_encoder.fit_transform(cat_features)

# Save encoder
with open("category_encoder.pkl", "wb") as f:
    pickle.dump(cat_encoder, f)

# ---  Encode Priority Labels (y) ---
prior_encoder = LabelEncoder()
y_raw = prior_encoder.fit_transform(data["Priority"])
y = np.eye(len(prior_encoder.classes_))[y_raw]

with open("priority_label_encoder.pkl", "wb") as f:
    pickle.dump(prior_encoder, f)

# ---  Train/Validation/Test Split ---
X_text_train, X_text_temp, X_cat_train, X_cat_temp, y_train, y_temp = train_test_split(
    X_text, X_cat, y, test_size=0.4, random_state=42
)
X_text_val, X_text_test, X_cat_val, X_cat_test, y_val, y_test = train_test_split(
    X_text_temp, X_cat_temp, y_temp, test_size=0.5, random_state=42
)

# --- Build Model ---
vocab_size = 10000
num_cat = X_cat.shape[1]
num_priorities = y.shape[1]

# Text Input Branch
text_input = layers.Input(shape=(max_len,), name="text_input")
x = layers.Embedding(vocab_size, 64)(text_input)
x = layers.GlobalAveragePooling1D()(x)  # simple pooling instead of GRU
x = layers.Dropout(0.3)(x)

# Categorical Input Branch
cat_input = layers.Input(shape=(num_cat,), name="cat_input")

# Combine Text + Categorical Features
combined = layers.Concatenate()([x, cat_input])
dense = layers.Dense(64, activation="relu", kernel_regularizer=tf.keras.regularizers.l2(0.01))(combined)
dense = layers.Dropout(0.3)(dense)

# Output Layer
output = layers.Dense(num_priorities, activation="softmax")(dense)

# Create and compile model
model = models.Model(inputs=[text_input, cat_input], outputs=output)
model.compile(optimizer="adam", loss="categorical_crossentropy", metrics=["accuracy"])

print("\n--- , MODEL SUMMARY ---")
model.summary()

# --- Train ---
history = model.fit(
    {"text_input": X_text_train, "cat_input": X_cat_train},
    y_train,
    validation_data=(
        {"text_input": X_text_val, "cat_input": X_cat_val},
        y_val
    ),
    epochs=35,
    batch_size=128,
)

# --- Evaluate ---
val_loss, val_acc = model.evaluate({"text_input": X_text_val, "cat_input": X_cat_val}, y_val)
print(f"Validation Accuracy: {val_acc:.4f}")

test_loss, test_acc = model.evaluate({"text_input": X_text_test, "cat_input": X_cat_test}, y_test)
print(f"Test Accuracy: {test_acc:.4f}")

# Save model
#model.save("ticket_priority_model_v2.h5")
#print("Model training complete and saved!")
