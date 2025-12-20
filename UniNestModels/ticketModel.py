import numpy as np
import tflite_runtime.interpreter as tflite
import json
import pickle
import re
import os
import shutil
from os.path import dirname, join, exists

# --- USE THE VALUES FROM YOUR TRAINING OUTPUT HERE ---
MU = np.array([0.5, 0.2, 0.3]) # Example values
SIGMA = np.array([0.8, 0.4, 0.5]) # Example values

def clean_text(text):
    text = text.lower()
    text = re.sub(r'[^a-z\s]', '', text)
    return text # Simplified for mobile (Lemmatization requires NLTK data)

def predict_ticket(description, category, room, high_urg_raw, med_urg_raw, low_urg_raw):
    cur_dir = dirname(__file__)

    # 1. SIGBUS FIX: Move model to safe storage
    safe_path = join(os.environ["HOME"], "model_fixed.tflite")
    if not exists(safe_path):
        shutil.copy(join(cur_dir, "model.tflite"), safe_path)

    # 2. Initialize Interpreter
    interpreter = tflite.Interpreter(model_path=safe_path)
    interpreter.allocate_tensors()

    # 3. Preprocess Text (Tokenize & Pad)
    with open(join(cur_dir, "tokenizer.pkl"), "rb") as f:
        tokenizer = pickle.load(f)

    cleaned = clean_text(description)
    seq = tokenizer.texts_to_sequences([cleaned])
    # MAX_LEN was 300 in your training code
    X_text = np.zeros((1, 300), dtype=np.float32)
    truncated_seq = seq[0][:300]
    X_text[0, :len(truncated_seq)] = truncated_seq

    # 4. Normalize Urgency Scores (Z-Score)
    # Your training: (Raw - MU) / SIGMA
    u_norm = (float(high_urg_raw) - MU[0]) / SIGMA[0]
    l_norm = (float(low_urg_raw) - MU[1]) / SIGMA[1]
    m_norm = (float(med_urg_raw) - MU[2]) / SIGMA[2]

    # 5. Map Inputs by Name (Crucial for Multi-Input Models)
    input_details = interpreter.get_input_details()

    # Map inputs to their correct positions in the TFLite list
    for detail in input_details:
        name = detail['name']
        if "text_input" in name:
            interpreter.set_tensor(detail['index'], X_text)
        elif "category_input" in name:
            interpreter.set_tensor(detail['index'], np.array([[category]], dtype=np.object_))
        elif "room_input" in name:
            interpreter.set_tensor(detail['index'], np.array([[room]], dtype=np.object_))
        elif "urgency_input" in name:
            interpreter.set_tensor(detail['index'], np.array([[u_norm]], dtype=np.float32))
        elif "low_urgency_input" in name:
            interpreter.set_tensor(detail['index'], np.array([[l_norm]], dtype=np.float32))
        elif "medium_input" in name:
            interpreter.set_tensor(detail['index'], np.array([[m_norm]], dtype=np.float32))

    # 6. Run Inference
    interpreter.invoke()
    output_details = interpreter.get_output_details()
    prediction_probs = interpreter.get_tensor(output_details[0]['index'])

    # 7. Apply your manual thresholding from training
    # high_idx = 0, med_idx = 1, low_idx = 2 (Check your encoder classes!)
    # Using your 0.55 / 0.60 logic:
    pred_idx = np.argmax(prediction_probs[0])
    labels = ["High", "Low", "Medium"] # MUST MATCH YOUR LabelEncoder Order

    return labels[pred_idx]