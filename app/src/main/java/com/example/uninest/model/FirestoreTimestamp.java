package com.example.uninest.model;

public class FirestoreTimestamp {
    private long seconds;
    private int nanos;

    public long getSeconds() { return seconds; }
    public void setSeconds(long seconds) { this.seconds = seconds; }

    public int getNanos() { return nanos; }
    public void setNanos(int nanos) { this.nanos = nanos; }

    // Helper to convert to Java Date
    public java.util.Date toDate() {
        return new java.util.Date(seconds * 1000 + nanos / 1000000);
    }

    @Override
    public String toString() {
        return toDate().toString();
    }
}
