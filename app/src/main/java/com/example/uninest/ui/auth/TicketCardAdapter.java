package com.example.uninest.ui.auth;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

        //  Basic Info
        h.tvBuilding.setText(item.getBuilding() != null ? item.getBuilding() : "Unknown");
        String apt = (item.getApartmentName() != null) ? item.getApartmentName() : "Unit";
        String cat = item.getCategory() != null ? item.getCategory() : "General";
        h.tvSubTitle.setText(apt + " - " + cat);

        String name = (item.getUserName() != null) ? item.getUserName() : "Tenant";
        h.tvRaisedBy.setText("Raised by: " + name);
        h.tvReportedOn.setText("Reported on: " + parseDate(item.getCreatedAt()));

        //  Priority & AI Star
        String priority = item.getPriority() != null ? item.getPriority() : "Low";
        h.tvPriorityChip.setText(priority);

        if (item.getPrioritySource() != null && item.getPrioritySource().equalsIgnoreCase("AI")) {
            h.ivAiStar.setVisibility(View.VISIBLE);
            h.ivAiStar.setColorFilter(Color.parseColor("#9C27B0"));
        } else {
            h.ivAiStar.setVisibility(View.GONE);
        }

        // Status & Date String Construction
        String status = item.getStatus() != null ? item.getStatus() : "Open";
        String dateSuffix;

        if ("Raised".equalsIgnoreCase(status)) {
            dateSuffix = parseDate(item.getCreatedAt());
        } else if (item.getArrivalDate() != null && !item.getArrivalDate().isEmpty()) {
            dateSuffix = "• Scheduled: " + item.getArrivalDate();
        } else {
            Object dateObj = (item.getUpdatedAt() != null) ? item.getUpdatedAt() : item.getCreatedAt();
            dateSuffix = parseDate(dateObj);
        }

        //  Deleted logic
        if (Boolean.TRUE.equals(item.isDeletedByTenant())) {
            // Override text and style for deleted tickets
            h.tvStatusDate.setText("REMOVED BY TENANT • " + status);
            h.tvStatusDate.setTextColor(Color.GRAY);
            h.itemView.setAlpha(0.6f); // Faded
        } else {
            // Normal ticket display
            h.tvStatusDate.setText(status + " : " + dateSuffix);
            h.itemView.setAlpha(1.0f); // opaque
            applyStatusDateColor(h.tvStatusDate, status);
        }

        //  General Styling
        applyPriorityChip(h.tvPriorityChip, priority);
        applyCardGlowByPriority(h.cardRoot, priority);

        h.cardRoot.setOnClickListener(v -> onCardClick.onClick(item));
    }

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
                    return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(seconds * 1000));
                }
            } else if (obj instanceof String) {
                String s = (String) obj;
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
        TextView tvBuilding, tvSubTitle, tvStatusDate, tvPriorityChip, tvRaisedBy, tvReportedOn;
        ImageView ivAiStar;

        VH(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            tvBuilding = itemView.findViewById(R.id.tvBuilding);
            tvSubTitle = itemView.findViewById(R.id.tvSubTitle);
            tvStatusDate = itemView.findViewById(R.id.tvStatusDate);
            tvPriorityChip = itemView.findViewById(R.id.tvPriorityChip);
            tvRaisedBy = itemView.findViewById(R.id.tvRaisedBy);
            tvReportedOn = itemView.findViewById(R.id.tvReportedOn);
            ivAiStar = itemView.findViewById(R.id.ivAiStar);
        }
    }

    private void applyCardGlowByPriority(View root, String priority) {
        if ("High".equalsIgnoreCase(priority)) {
            root.setBackgroundResource(R.drawable.bg_card_border_raised);
        } else if ("Medium".equalsIgnoreCase(priority)) {
            root.setBackgroundResource(R.drawable.bg_card_border_progress);
        } else {
            root.setBackgroundResource(R.drawable.bg_card_border_solved);
        }
        root.setElevation(dp(2));
    }

    private void applyStatusDateColor(TextView tv, String state) {
        if (state == null) return;
        String s = state.toLowerCase();
        if (s.contains("raised") || s.contains("open")) {
            tv.setTextColor(context.getColor(R.color.state_raised));
        } else if (s.contains("progress")) {
            tv.setTextColor(context.getColor(R.color.state_in_progress));
        } else if (s.contains("solved") || s.contains("closed") || s.contains("resolved")) {
            tv.setTextColor(context.getColor(R.color.state_solved));
        }
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

    public void applyAdvancedFilter(String priority, String state, String sortType, boolean aiOnly) {
        filtered.clear();
        for (Ticket t : all) {
            boolean matchesPriority = priority.isEmpty() || (t.getPriority() != null && t.getPriority().equalsIgnoreCase(priority));
            boolean matchesState = state.isEmpty() || (t.getStatus() != null && t.getStatus().equalsIgnoreCase(state));
            boolean matchesAi = !aiOnly || (t.getPrioritySource() != null && t.getPrioritySource().equalsIgnoreCase("AI"));

            if (matchesPriority && matchesState && matchesAi) {
                filtered.add(t);
            }
        }

        if ("Building".equalsIgnoreCase(sortType)) {
            filtered.sort((a, b) -> (a.getBuilding() != null ? a.getBuilding() : "").compareToIgnoreCase(b.getBuilding() != null ? b.getBuilding() : ""));
        } else if ("Priority".equalsIgnoreCase(sortType)) {
            filtered.sort((a, b) -> Integer.compare(getPriorityRank(b.getPriority()), getPriorityRank(a.getPriority())));
        } else {
            filtered.sort((a, b) -> Long.compare(getTicketSeconds(b), getTicketSeconds(a)));
        }
        notifyDataSetChanged();
    }

    private int getPriorityRank(String p) {
        if (p == null) return 0;
        switch (p.toLowerCase()) {
            case "high": return 3;
            case "medium": return 2;
            case "low": return 1;
            default: return 0;
        }
    }

    private long getTicketSeconds(Ticket t) {
        Object obj = t.getCreatedAt();
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            if (map.containsKey("seconds")) {
                Object sec = map.get("seconds");
                if (sec instanceof Number) return ((Number) sec).longValue();
            }
        }
        return 0;
    }
}