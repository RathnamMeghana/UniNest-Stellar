import json
import numpy as np
from keras_preprocessing.text import tokenizer_from_json
from keras_preprocessing.sequence import pad_sequences

tokenizer = None

def load_tokenizer(json_str):
    global tokenizer
    try:
        data = json.loads(json_str)
        tokenizer = tokenizer_from_json(json.dumps(data))
        return "Tokenizer loaded successfully!"
    except Exception as e:
        return f"Python Error: {str(e)}"

def preprocess_text(text):
    global tokenizer
    if tokenizer is None:
        return [0] * 300  # fallback

    sequences = tokenizer.texts_to_sequences([text])
    padded = pad_sequences(sequences, maxlen=300, padding='post', truncating='post')
    return padded[0].tolist()
