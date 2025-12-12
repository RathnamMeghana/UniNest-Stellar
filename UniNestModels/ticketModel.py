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


LEARNING_RATE = 0.0002
VOCAB_SIZE = 15000
EMBEDDING_DIM = 256
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
    'dripping', 'lightbulb', 'not cleaning','cleaned','bulb', 'not cleaning', 'cleaned',
]

MEDIUM_KEYWORDS = [
    'mold', 'mildew', 'not turning on', 'strange noise'
]

URGENT_KEYWORDS = [
    'leak', 'flood', 'flooding','no water', 'burst pipe', 'sewage', 'leaking',
    'no heat', 'sparking', 'door not locking', 'no power',
    'mouse', 'rodent', 'cockroach', 'pest', 'rat', 'bedbug','gas'
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


#  Urgency features

def calculate_urgency_score(text_description, keywords):
    score = 0
    for keyword in keywords:
        score += text_description.count(keyword)
    return score

X_urgency_raw = np.array([
    calculate_urgency_score(t, URGENT_KEYWORDS) for t in cleaned_texts
], dtype=np.float32)
X_urgency = X_urgency_raw / (np.max(X_urgency_raw) + 1e-8)

X_low_urgency_raw = np.array([
    calculate_urgency_score(t, LOW_KEYWORDS) for t in cleaned_texts
], dtype=np.float32)
X_low_urgency = X_low_urgency_raw / (np.max(X_low_urgency_raw) + 1e-8)

X_medium_raw = np.array([
    calculate_urgency_score(t, MEDIUM_KEYWORDS) for t in cleaned_texts
], dtype=np.float32)

X_medium = X_medium_raw / (np.max(X_medium_raw) + 1e-8)

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


# Class Weights

class_weights = compute_class_weight(
    class_weight='balanced',
    classes=np.unique(y_raw),
    y=y_raw
)
class_weights_dict = dict(enumerate(class_weights))
print("\nClass Weights:")
for i, w in class_weights_dict.items():
    print(f"{prior_encoder.classes_[i]}: {w:.4f}")

#      Split into train val and test

X_text_train, X_text_temp, X_category_train, X_category_temp, X_room_train, X_room_temp, \
    X_urgency_train, X_urgency_temp, X_low_urgency_train, X_low_urgency_temp, \
    X_medium_train, X_medium_temp, y_train, y_temp = train_test_split(
    X_text, X_category, X_room, X_urgency, X_low_urgency,
    X_medium, y, test_size=0.4, random_state=42
)

X_text_val, X_text_test, X_category_val, X_category_test, X_room_val, X_room_test, \
    X_urgency_val, X_urgency_test, X_low_urgency_val, X_low_urgency_test, \
    X_medium_val, X_medium_test, y_val, y_test = train_test_split(
    X_text_temp, X_category_temp, X_room_temp, X_urgency_temp,
    X_low_urgency_temp, X_medium_temp, y_temp, test_size=0.4, random_state=42
)



# Focal loss

def focal_loss(gamma=2.0, alpha=0.25):
    def loss(y_true, y_pred):
        y_pred = tf.clip_by_value(y_pred, 1e-7, 1-1e-7)
        cross_entropy = -y_true * tf.math.log(y_pred)
        weight = alpha * tf.math.pow((1 - y_pred), gamma)
        loss = weight * cross_entropy
        return tf.reduce_mean(loss, axis=1)
    return loss


# bUILD MULTI INPUT MODEL

# Text input
text_input = layers.Input(shape=(MAX_LEN,), name="text_input")
x_text = layers.Embedding(VOCAB_SIZE, EMBEDDING_DIM)(text_input)
x_text = layers.Bidirectional(layers.GRU(16))(x_text)
x_text = layers.Dropout(0.4)(x_text)

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

# Urgency inputs
urgency_input = layers.Input(shape=(1,), name="urgency_input")
low_urgency_input = layers.Input(shape=(1,), name="low_urgency_input")
medium_input = layers.Input(shape=(1,), name="medium_input")

# Combine all features
combined = layers.Concatenate()([
    x_text, x_category, x_room,
    urgency_input, low_urgency_input, medium_input
])


# Dense head
dense = layers.Dense(16, activation="relu", kernel_regularizer=l2(L2_REG))(combined)
dense = layers.Dropout(0.4)(dense)
dense = layers.Dense(8, activation="relu")(dense)
output = layers.Dense(num_priorities, activation="softmax", kernel_regularizer=l2(L2_REG))(dense)

# Model
model = models.Model(
    inputs=[text_input, category_input, room_input, urgency_input, low_urgency_input, medium_input],
    outputs=output
)

optimizer = tf.keras.optimizers.Adam(learning_rate=LEARNING_RATE)
model.compile(
    optimizer=optimizer,
    loss=focal_loss(),
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

print("\n--- Classification Report ---")
print(classification_report(y_true, y_pred, target_names=prior_encoder.classes_, labels=np.unique(y_true)))

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
model.save("ticket_priority_model.h5")
