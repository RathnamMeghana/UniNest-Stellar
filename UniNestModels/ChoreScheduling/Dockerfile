#  lightweight Python image
FROM python:3.10-slim

#  Set the working directory inside the container
WORKDIR /app

#  Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

#  Copy the model file and the code
# Ensure stable_chore_rf_model.pkl is in this folder!
COPY stable_chore_rf_model.pkl .
COPY app.py .

#  Start Gunicorn
# --bind :$PORT tells it to use the port Google Cloud provides
# app:app means "Look in app.py for the variable named app"
CMD exec gunicorn --bind :$PORT --workers 1 --threads 8 --timeout 0 app:app