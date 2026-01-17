package com.example.uninest.ui.auth;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.model.Ticket;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TicketCardAdapter extends RecyclerView.Adapter<TicketCardAdapter.VH> {

    public interface OnCardClick {
        void onClick(Ticket item);
    }

    private final Context context;
    private final OnCardClick onCardClick;

    private final List<Ticket> all = new ArrayList<>();
    private final List<Ticket> filtered = new ArrayList<>();

    public TicketCardAdapter(Context context, List<Ticket> data, OnCardClick onCardClick) {
        this.context = context;
        this.onCardClick = onCardClick;
        setData(data);
    }

    public void setData(List<Ticket> data) {
        all.clear();
        all.addAll(data);
        filter("");
    }

    public void filter(String q) {
        filtered.clear();
        String query = (q == null) ? "" : q.toLowerCase().trim();

        for (Ticket t : all) {
            String building = t.getBuilding() != null ? t.getBuilding() : "";
            String room = t.getRoom() != null ? t.getRoom() : "";
            String priority = t.getPriority() != null ? t.getPriority() : "";
            String status = t.getStatus() != null ? t.getStatus() : "";
            String category = t.getCategory() != null ? t.getCategory() : "";

            String hay = (building + " " + room + " " + category + " " + priority + " " + status).toLowerCase();

            if (hay.contains(query)) {
                filtered.add(t);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Ticket item = filtered.get(position);

        // 1. Building & Subtitle
        h.tvBuilding.setText(item.getBuilding() != null ? item.getBuilding() : "Unknown");
        String apt = (item.getApartmentName() != null) ? item.getApartmentName() : "Unit";
        String cat = item.getCategory() != null ? item.getCategory() : "General";
        h.tvSubTitle.setText(apt + " - " + cat);

        // 2. Priority
        String priority = item.getPriority() != null ? item.getPriority() : "Low";
        h.tvPriorityChip.setText(priority);

        // 3. Status & Date Logic
        String status = item.getStatus() != null ? item.getStatus() : "Open";
        String finalStatusText;

        // CASE 1: New Ticket
        if ("Raised".equalsIgnoreCase(status)) {
            finalStatusText = "Raised : " + parseDate(item.getCreatedAt());
        }
        // CASE 2: Scheduled Date exists
        else if (item.getArrivalDate() != null && !item.getArrivalDate().isEmpty()) {
            finalStatusText = status + " • Scheduled: " + item.getArrivalDate();
        }
        // CASE 3: Solved/Open/Closed
        else {
            // Use UpdatedAt if available, otherwise fall back to CreatedAt
            Object dateObj = (item.getUpdatedAt() != null) ? item.getUpdatedAt() : item.getCreatedAt();
            finalStatusText = status + " : " + parseDate(dateObj);
        }

        h.tvStatusDate.setText(finalStatusText);

        // 4. Styling
        applyPriorityChip(h.tvPriorityChip, priority);
        applyCardGlowByPriority(h.cardRoot, priority);
        applyStatusDateColor(h.tvStatusDate, status);

        h.cardRoot.setOnClickListener(v -> onCardClick.onClick(item));
    }

    // This handles both Map (Firestore) and String (JSON) formats
    private String parseDate(Object obj) {
        if (obj == null) return "N/A";
        try {
            if (obj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) obj;
                if (map.containsKey("seconds")) {
                    Object secObj = map.get("seconds");
                    long seconds = 0;
                    if (secObj instanceof Double) seconds = ((Double) secObj).longValue();
                    else if (secObj instanceof Long) seconds = (Long) secObj;

                    Date d = new Date(seconds * 1000);
                    return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(d);
                }
            }
            else if (obj instanceof String) {
                String s = (String) obj;
                // Quick formatting: just take the YYYY-MM-DD part
                if (s.length() >= 10) return s.substring(0, 10);
                return s;
            }
        } catch (Exception e) {
            return "Date Error";
        }
        return "N/A";
    }

    @Override
    public int getItemCount() {
        return filtered.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        View cardRoot;
        TextView tvBuilding, tvSubTitle, tvStatusDate, tvPriorityChip;

        VH(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            tvBuilding = itemView.findViewById(R.id.tvBuilding);
            tvSubTitle = itemView.findViewById(R.id.tvSubTitle);
            tvStatusDate = itemView.findViewById(R.id.tvStatusDate);
            tvPriorityChip = itemView.findViewById(R.id.tvPriorityChip);
        }
    }

    private void applyCardGlowByPriority(View root, String priority) {
        if ("High".equalsIgnoreCase(priority)) {
            root.setBackgroundResource(R.drawable.bg_ticket_card_high);
        } else if ("Medium".equalsIgnoreCase(priority)) {
            root.setBackgroundResource(R.drawable.bg_ticket_card_medium);
        } else {
            root.setBackgroundResource(R.drawable.bg_ticket_card_low);
        }
        root.setElevation(dp(6));
    }

    private void applyStatusDateColor(TextView tv, String state) {
        if (state == null) return;
        String s = state.toLowerCase();
        if (s.contains("raised") || s.contains("open")) tv.setTextColor(context.getColor(R.color.state_raised));
        else if (s.contains("progress")) tv.setTextColor(context.getColor(R.color.state_in_progress));
        else if (s.contains("solved") || s.contains("closed")) tv.setTextColor(context.getColor(R.color.state_solved));
    }

    private void applyPriorityChip(TextView chip, String priority) {
        int bg, text;
        if ("High".equalsIgnoreCase(priority)) {
            bg = context.getColor(R.color.chip_high_bg);
            text = context.getColor(R.color.chip_high_text);
        } else if ("Medium".equalsIgnoreCase(priority)) {
            bg = context.getColor(R.color.chip_medium_bg);
            text = context.getColor(R.color.chip_medium_text);
        } else {
            bg = context.getColor(R.color.chip_low_bg);
            text = context.getColor(R.color.chip_low_text);
        }
        GradientDrawable d = (GradientDrawable) chip.getBackground().mutate();
        d.setColor(bg);
        chip.setTextColor(text);
    }

    private int dp(int v) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(v * density);
    }
}
