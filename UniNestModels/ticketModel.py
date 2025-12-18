import pickle
import tensorflow as tf
from tensorflow.keras import layers, models
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from tensorflow.keras.preprocessing.text import Tokenizer
from tensorflow.keras.preprocessing.sequence import pad_sequences
from tensorflow.keras.regularizers import l2
from sklearn.utils.class_weight import compute_class_weight
from sklearn.metrics import confusion_matrix, classification_report
import matplotlib.pyplot as plt
import seaborn as sns
import re
import nltk
from nltk.corpus import stopwords
from nltk.stem import WordNetLemmatizer
from tqdm import tqdm


LEARNING_RATE = 0.001
VOCAB_SIZE = 15000
EMBEDDING_DIM = 96
ROOM_EMBEDDING_DIM = 12
EPOCHS = 150
MAX_LEN = 300
BATCH_SIZE = 32
L2_REG = 1e-3


# Load in data              

data = pd.read_csv("apartment_tickets_augmented.csv")
texts = data["description"].astype(str).values

STOP_WORDS = set(stopwords.words('english'))
LEMMA = WordNetLemmatizer()

LOW_KEYWORDS = [
    'cosmetic', 'minor', 'scratch', 'loose', 'paint',
    'dripping', 'lightbulb', 'not cleaning','cleaned','bulb'
]

MEDIUM_KEYWORDS = [
    'mildew', 'not turning on', 'strange noise', 'Flickering'
]

URGENT_KEYWORDS = [
    'leak', 'flood', 'flooding','no water', 'burst pipe', 'sewage', 'leaking',
    'no heat', 'sparking', 'door not locking', 'no power',
    'mouse', 'rodent', 'cockroach', 'pest', 'rat', 'bedbug','gas', 'mold', 'backing up', 'burning smell'
]


# Clean text

def clean_and_lemmatize(text):
    text = text.lower()
    text = re.sub(r'[^a-z\s]', '', text)
    words = text.split()
    processed_words = [
        LEMMA.lemmatize(word)
        for word in words
        if word not in STOP_WORDS
    ]
    return " ".join(processed_words)

print("Cleaning text...")
cleaned_texts = [clean_and_lemmatize(t) for t in tqdm(texts)]
print("Text cleaning complete.")


# Text tokenization

tokenizer = Tokenizer(num_words=VOCAB_SIZE, oov_token="<OOV>")
tokenizer.fit_on_texts(cleaned_texts)
sequences = tokenizer.texts_to_sequences(cleaned_texts)
X_text = pad_sequences(sequences, maxlen=MAX_LEN, padding='post')

with open("tokenizer.pkl", "wb") as f:
    pickle.dump(tokenizer, f)


# Urgency features (Raw scores calculated)

def calculate_urgency_score(text_description, keywords):
    score = 0
    for keyword in keywords:
        score += text_description.count(keyword)
    return score

X_urgency_raw = np.array([
    calculate_urgency_score(t, URGENT_KEYWORDS) for t in cleaned_texts
], dtype=np.float32)

X_low_urgency_raw = np.array([
    calculate_urgency_score(t, LOW_KEYWORDS) for t in cleaned_texts
], dtype=np.float32)

X_medium_raw = np.array([
    calculate_urgency_score(t, MEDIUM_KEYWORDS) for t in cleaned_texts
], dtype=np.float32)

print(f"Urgency features added. Max urgency: {np.max(X_urgency_raw)}, Max low urgency: {np.max(X_low_urgency_raw)}")


# Category Encoding
category_lookup = layers.StringLookup(output_mode="int")
category_lookup.adapt(data["category"].astype(str))
X_category = data["category"].astype(str).values

# Room Encoding
room_lookup = layers.StringLookup(output_mode="int")
room_lookup.adapt(data["room"].astype(str))
X_room = data["room"].astype(str).values
print(f"Room feature added. Total unique rooms: {room_lookup.vocabulary_size()}")


# Target Encoding
prior_encoder = LabelEncoder()
y_raw = prior_encoder.fit_transform(data["priority"])
num_priorities = len(prior_encoder.classes_)
y = tf.keras.utils.to_categorical(y_raw, num_classes=num_priorities)


# --- Z-SCORE NORMALIZATION PREP ---

# Combine all raw urgency features into a single array
X_urgency_features_raw = np.column_stack([X_urgency_raw, X_low_urgency_raw, X_medium_raw])

# Create temporary index array for splitting
indices = np.arange(len(data))

# Perform the first split just on indices/raw features to get the training subset indices
train_indices, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ = train_test_split(
    indices, X_category, X_room, X_urgency_raw, X_low_urgency_raw,
    X_medium_raw, y, y_raw,
    test_size=0.4,
    random_state=42,
    stratify=y_raw
)

# Use the train indices to calculate MEAN (MU) and STD DEV (SIGMA)
X_urgency_train_raw_subset = X_urgency_features_raw[train_indices]

# Calculate mean (mu) and std dev (sigma) ONLY from the training subset
MU = np.mean(X_urgency_train_raw_subset, axis=0)
SIGMA = np.std(X_urgency_train_raw_subset, axis=0)
# Avoid division by zero
SIGMA[SIGMA == 0] = 1e-8

print(f"\nUrgency Normalization Parameters (Calculated on Train Set Only):")
print(f"  Mean (Urgent, Low, Medium): {MU}")
print(f"  Std Dev (Urgent, Low, Medium): {SIGMA}")

# Apply Z-score normalization to the entire dataset using the training parameters
X_urgency_norm = (X_urgency_features_raw[:, 0] - MU[0]) / SIGMA[0]
X_low_urgency_norm = (X_urgency_features_raw[:, 1] - MU[1]) / SIGMA[1]
X_medium_norm = (X_urgency_features_raw[:, 2] - MU[2]) / SIGMA[2]


#           Split into train val and test    

X_text_train, X_text_temp, X_category_train, X_category_temp, X_room_train, X_room_temp, \
    X_urgency_train, X_urgency_temp, X_low_urgency_train, X_low_urgency_temp, \
    X_medium_train, X_medium_temp, y_train, y_temp, y_raw_train, y_raw_temp = train_test_split(
    X_text, X_category, X_room, X_urgency_norm, X_low_urgency_norm,
    X_medium_norm, y, y_raw, # Use the normalized features
    test_size=0.4,
    random_state=42,
    stratify=y_raw
)

X_text_val, X_text_test, X_category_val, X_category_test, X_room_val, X_room_test, \
    X_urgency_val, X_urgency_test, X_low_urgency_val, X_low_urgency_test, \
    X_medium_val, X_medium_test, y_val, y_test = train_test_split(
    X_text_temp, X_category_temp, X_room_temp, X_urgency_temp,
    X_low_urgency_temp, X_medium_temp, y_temp,
    test_size=0.4,
    random_state=42,
    stratify=y_raw_temp
)


# Focal loss (Included for reference but not used in final compilation)

def focal_loss(gamma=1.5, alpha=0.25):
    def loss(y_true, y_pred):
        y_pred = tf.clip_by_value(y_pred, 1e-7, 1-1e-7)
        cross_entropy = -y_true * tf.math.log(y_pred)
        weight = alpha * tf.math.pow((1 - y_pred), gamma)
        loss = weight * cross_entropy
        return tf.reduce_mean(loss, axis=1)
    return loss


# BUILD MULTI INPUT MODEL

# Text input
text_input = layers.Input(shape=(MAX_LEN,), name="text_input")
x_text = layers.Embedding(VOCAB_SIZE, EMBEDDING_DIM)(text_input)


x_text = layers.Bidirectional(layers.GRU(8, kernel_regularizer=l2(L2_REG)))(x_text)

# ---------------------------------------------------------------------

x_text = layers.Dropout(0.3)(x_text)

# Category input
category_input = layers.Input(shape=(1,), dtype=tf.string, name="category_input")
x_category = category_lookup(category_input)
x_category = layers.Embedding(category_lookup.vocabulary_size(), 12)(x_category)
x_category = layers.Flatten()(x_category)

# Room input
room_input = layers.Input(shape=(1,), dtype=tf.string, name="room_input")
x_room = room_lookup(room_input)
x_room = layers.Embedding(room_lookup.vocabulary_size(), ROOM_EMBEDDING_DIM)(x_room)
x_room = layers.Flatten()(x_room)

# Urgency inputs (Now Z-score Normalized)
urgency_input = layers.Input(shape=(1,), name="urgency_input")
low_urgency_input = layers.Input(shape=(1,), name="low_urgency_input")
medium_input = layers.Input(shape=(1,), name="medium_input")

# Combine all features
combined = layers.Concatenate()([
    x_text, x_category, x_room,
    urgency_input, low_urgency_input, medium_input
])


# Dense head
dense = layers.Dense(32, activation="relu")(combined)

dense = layers.Dropout(0.4)(dense)

dense = layers.Dense(16, activation="relu")(dense)
dense = layers.Dropout(0.3)(dense)

output = layers.Dense(num_priorities, activation="softmax", kernel_regularizer=l2(L2_REG))(dense)

# Model
model = models.Model(
    inputs=[text_input, category_input, room_input, urgency_input, low_urgency_input, medium_input],
    outputs=output
)

optimizer = tf.keras.optimizers.Adam(learning_rate=LEARNING_RATE)


class_weights_dict = {
    0: 1.7,
    1: 1.1,
    2: 1.05
}
loss = tf.keras.losses.CategoricalCrossentropy()
model.compile(
    optimizer=optimizer,
    loss= loss,
    metrics=["accuracy"]
)

model.summary()


# train          
early_stopping = tf.keras.callbacks.EarlyStopping(
    monitor='val_loss',
    patience=15,
    restore_best_weights=True
)

train_inputs = {
    "text_input": X_text_train,
    "category_input": X_category_train,
    "room_input": X_room_train,
    "urgency_input": X_urgency_train,
    "low_urgency_input": X_low_urgency_train,
    "medium_input": X_medium_train
}

val_inputs = {
    "text_input": X_text_val,
    "category_input": X_category_val,
    "room_input": X_room_val,
    "urgency_input": X_urgency_val,
    "low_urgency_input": X_low_urgency_val,
    "medium_input": X_medium_val
}


history = model.fit(
    train_inputs,
    y_train,
    validation_data=(val_inputs, y_val),
    epochs=EPOCHS,
    batch_size=BATCH_SIZE,
    callbacks=[early_stopping],
    class_weight=class_weights_dict
)


# Evaluation        

test_inputs = {
    "text_input": X_text_test,
    "category_input": X_category_test,
    "room_input": X_room_test,
    "urgency_input": X_urgency_test,
    "low_urgency_input": X_low_urgency_test,
    "medium_input": X_medium_test
}

val_loss, val_acc = model.evaluate(val_inputs, y_val, verbose=0)
test_loss, test_acc = model.evaluate(test_inputs, y_test, verbose=0)

print(f"\nValidation Accuracy: {val_acc:.4f}")
print(f"Test Accuracy: {test_acc:.4f}")

y_pred_probs = model.predict(test_inputs)
y_pred = np.argmax(y_pred_probs, axis=1)
y_true = np.argmax(y_test, axis=1)

# Split indices the same way as your text split
indices = np.arange(len(data))

# First split: train vs temp
train_idx, temp_idx = train_test_split(
    indices,
    test_size=0.4,
    random_state=42,
    stratify=y_raw
)

# Second split: val vs test
val_idx, test_idx = train_test_split(
    temp_idx,
    test_size=0.4,
    random_state=42,
    stratify=y_raw[temp_idx]
)
y_pred_probs = model.predict(test_inputs)

# Apply thresholds and rules to the whole test set
thresholded_predictions = []

high_idx = list(prior_encoder.classes_).index("High")
medium_idx = list(prior_encoder.classes_).index("Medium")

high_keywords = ["gas", "flood", "rat", "mouse"]




for i in range(len(y_pred_probs)):
    probs = y_pred_probs[i]
    urgency_score = X_urgency_test[i]
    ticket_text = cleaned_texts[test_idx[i]]

    if probs[high_idx] > 0.55:
        priority = "High"
    elif probs[medium_idx] > 0.60:
        priority = "Medium"
    else:
        priority = "Low"

    if urgency_score >= 2.75 and probs[high_idx] > 0.30:
        priority = "High"


    thresholded_predictions.append(priority)

y_pred_thresholded = np.array([prior_encoder.transform([p])[0] for p in thresholded_predictions])
y_true_numeric = np.argmax(y_test, axis=1)



print("\n--- Classification Report ---")
print(classification_report(
    y_true_numeric,
    y_pred_thresholded,
    target_names=prior_encoder.classes_,
    labels=np.unique(y_true_numeric)
))

cm = confusion_matrix(y_true, y_pred)
print("\nConfusion Matrix:\n", cm)

plt.figure(figsize=(9, 7))
sns.heatmap(cm, annot=True, fmt='d',
            xticklabels=prior_encoder.classes_,
            yticklabels=prior_encoder.classes_,
            cmap="Blues")
plt.title("Confusion Matrix")
plt.ylabel("True")
plt.xlabel("Predicted")
plt.show()

# Save model
model.save("ticket_priority_model_final.h5")


# Create TFLite converter
converter = tf.lite.TFLiteConverter.from_keras_model(model)

#  optimization for smaller/faster model
converter.optimizations = [tf.lite.Optimize.DEFAULT]

# enable select TF ops if your model uses any TF ops not in TFLite core
converter.target_spec.supported_ops = [
    tf.lite.OpsSet.TFLITE_BUILTINS,  # default TFLite ops
    tf.lite.OpsSet.SELECT_TF_OPS     # allows Flex ops (e.g., GRU, Bidirectional)
]

# Convert the model
tflite_model = converter.convert()

# Save the TFLite model
with open("ticket_priority_model_final.tflite", "wb") as f:
    f.write(tflite_model)

