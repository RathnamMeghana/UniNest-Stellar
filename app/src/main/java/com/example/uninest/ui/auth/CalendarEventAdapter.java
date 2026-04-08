package com.example.uninest.ui.auth;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.SessionManager;
import com.example.uninest.data.api.ApiClient;
import com.example.uninest.model.Calendar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CalendarEventAdapter extends RecyclerView.Adapter<CalendarEventAdapter.VH> {

    private List<Calendar> items = new ArrayList<>();
    private Context context;
    private SessionManager sessionManager;

    public void setData(List<Calendar> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        this.sessionManager = new SessionManager(context);
        View v = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Calendar event = items.get(position);
        Log.d("AdapterStatus", "Event: " + event.getTitle() + " Status: " + event.getStatus());

        // 1. DATA NORMALIZATION (Ensuring status is read correctly)
        String rawStatus = event.getStatus();
        String currentStatus = (rawStatus != null) ? rawStatus.trim().toUpperCase() : "NOT_STARTED";
        boolean isResolved = "RESOLVED".equals(currentStatus);

        // 2. LOGIC CHECKS
        boolean isTenant = "2".equals(sessionManager.getUserRole());
        boolean isMaintenance = "MAINTENANCE".equalsIgnoreCase(event.getType());
        boolean isActionRequired = isTenant && isMaintenance && !isResolved;

        // 3. UI RENDERING
        String statusSuffix = isResolved ? " (Done)" : "";
        holder.tvTitle.setText(event.getTitle() + statusSuffix);
        holder.tvDesc.setText((event.getDescription() != null) ? event.getDescription() : "No details");

        if (isActionRequired) {
            // Apply "Action Needed" Purple Theme
            holder.itemView.setBackgroundColor(context.getResources().getColor(android.R.color.holo_purple, null));
            holder.tvTitle.setTextColor(Color.WHITE);
            holder.tvDesc.setTextColor(Color.WHITE);

            // Trigger Dialog on Click
            holder.itemView.setOnClickListener(v -> {
                // Get the CURRENT position in case the list moved
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    showConfirmationDialog(items.get(currentPos), currentPos);
                }
            });
        } else {
            // THE FIX: Explicitly clear the state for already Done or non-maintenance items
            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);

            // Force background back to White/Transparent
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);

            if (isResolved) {
                holder.tvTitle.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
                holder.tvDesc.setTextColor(Color.GRAY);
            } else {
                holder.tvTitle.setTextColor(Color.BLACK);
                holder.tvDesc.setTextColor(Color.GRAY);
            }
        }
    }

    private void showConfirmationDialog(Calendar event, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Confirm Maintenance Completion")
                .setMessage("Has this maintenance visit actually happened and been completed to your satisfaction?")
                .setPositiveButton("Yes, it's done", (dialog, which) -> {
                    performConfirmVisit(event, position);
                })
                .setNegativeButton("Not yet", null)
                .show();
    }

    private void performConfirmVisit(Calendar event, int position) {
        ApiClient.getTicketApi().confirmVisit(event.getId()).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "Confirmation sent! Agent has been notified.", Toast.LENGTH_SHORT).show();

                    // 1. Update the actual data object
                    event.setStatus("RESOLVED");

                    // 2. Update the specific item in the list array
                    items.set(position, event);

                    // 3. Force the UI to re-bind this specific item immediately
                    notifyItemChanged(position);

                } else {
                    Toast.makeText(context, "Error: " + response.code() + " - Unauthorized", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Toast.makeText(context, "Network Error: Please check your connection", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDesc;
        public VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(android.R.id.text1);
            tvDesc = itemView.findViewById(android.R.id.text2);
        }
    }
}