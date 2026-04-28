package com.example.uninest.ui.auth;

import android.content.Context;
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
    private String searchQuery = "";
    private String selectedPriority = "";
    private String selectedState = "";
    private String selectedSort = "";
    private boolean aiOnly = false;

    public TicketCardAdapter(Context context, List<Ticket> data, OnCardClick onCardClick) {
        this.context = context;
        this.onCardClick = onCardClick;
        setData(data);
    }

    public void setData(List<Ticket> data) {
        all.clear();
        if (data != null) {
            all.addAll(data);
        }
        reapplyFilters();
    }

    public void filter(String q) {
        searchQuery = (q == null) ? "" : q.trim();
        reapplyFilters();
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

        String building = item.getBuilding() != null ? item.getBuilding() : "";
        String apartment = item.getApartmentName() != null ? item.getApartmentName() : "";
        String room = item.getRoom() != null ? item.getRoom() : "";
        String category = item.getCategory() != null ? item.getCategory() : "";

        h.tvBuilding.setText(buildIssueTitle(room, category));
        h.tvSubTitle.setText(buildLocationLine(building, apartment));

        String name = item.getUserName() != null ? item.getUserName() : "Tenant";
        h.tvRaisedBy.setText("Raised by: " + name);

        String priority = item.getPriority() != null ? item.getPriority() : "Low";
        h.tvPriorityChip.setText(priority);

        if (item.getPrioritySource() != null && item.getPrioritySource().equalsIgnoreCase("AI")) {
            h.ivAiStar.setVisibility(View.VISIBLE);
            h.ivAiStar.setColorFilter(context.getColor(R.color.calendar_primary_dark));
        } else {
            h.ivAiStar.setVisibility(View.GONE);
        }

        String status = item.getStatus() != null ? item.getStatus() : "Open";
        String canonicalStatus = canonicalStatus(status);
        String dateSuffix;
        if ("raised".equals(canonicalStatus)) {
            dateSuffix = parseDate(item.getCreatedAt());
        } else if (item.getArrivalDate() != null && !item.getArrivalDate().isEmpty()) {
            dateSuffix = "Scheduled: " + item.getArrivalDate();
        } else {
            Object dateObj = item.getUpdatedAt() != null ? item.getUpdatedAt() : item.getCreatedAt();
            dateSuffix = parseDate(dateObj);
        }

        h.tvReportedOn.setText(buildDateLabel(item, canonicalStatus, dateSuffix));

        if (Boolean.TRUE.equals(item.isDeletedByTenant())) {
            h.tvStatusDate.setText("Removed");
            h.tvStatusDate.setBackgroundResource(R.drawable.bg_tenant_ticket_note);
            h.tvStatusDate.setTextColor(context.getColor(R.color.calendar_text_secondary));
            h.itemView.setAlpha(0.6f);
        } else {
            h.tvStatusDate.setText(prettyStatus(canonicalStatus));
            h.itemView.setAlpha(1.0f);
            applyStatusBadgeStyle(h.tvStatusDate, canonicalStatus);
        }

        applyPriorityChip(h.tvPriorityChip, priority);
        applyCardStyleByStatus(h.cardRoot, canonicalStatus);

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
                    return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(new Date(seconds * 1000));
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

    private String buildIssueTitle(String room, String category) {
        String cleanRoom = room == null ? "" : room.trim();
        String cleanCategory = category == null ? "" : category.trim();

        if (!cleanRoom.isEmpty() && !cleanCategory.isEmpty()) {
            if (cleanRoom.equalsIgnoreCase(cleanCategory)) {
                return cleanCategory;
            }
            return cleanRoom + ": " + cleanCategory;
        }

        if (!cleanCategory.isEmpty()) {
            return cleanCategory;
        }

        if (!cleanRoom.isEmpty()) {
            return cleanRoom;
        }

        return "Maintenance issue";
    }

    private String buildLocationLine(String building, String apartment) {
        String cleanBuilding = building == null ? "" : building.trim();
        String cleanApartment = apartment == null ? "" : apartment.trim();

        if (!cleanBuilding.isEmpty() && !cleanApartment.isEmpty()) {
            return cleanBuilding + " | " + cleanApartment;
        }

        if (!cleanBuilding.isEmpty()) {
            return cleanBuilding;
        }

        if (!cleanApartment.isEmpty()) {
            return cleanApartment;
        }

        return "Unknown location";
    }

    @Override
    public int getItemCount() {
        return filtered.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        View cardRoot;
        TextView tvBuilding;
        TextView tvSubTitle;
        TextView tvStatusDate;
        TextView tvPriorityChip;
        TextView tvRaisedBy;
        TextView tvReportedOn;
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

    private void applyCardStyleByStatus(View root, String status) {
        if ("progress".equals(status)) {
            root.setBackgroundResource(R.drawable.bg_tenant_ticket_card_progress);
        } else if ("solved".equals(status)) {
            root.setBackgroundResource(R.drawable.bg_tenant_ticket_card_solved);
        } else {
            root.setBackgroundResource(R.drawable.bg_tenant_ticket_card_raised);
        }
        root.setElevation(dp(1));
    }

    private void applyStatusBadgeStyle(TextView tv, String state) {
        if ("raised".equals(state)) {
            tv.setBackgroundResource(R.drawable.bg_tenant_ticket_status_raised);
            tv.setTextColor(context.getColor(R.color.ticket_raised_text));
        } else if ("progress".equals(state)) {
            tv.setBackgroundResource(R.drawable.bg_tenant_ticket_status_progress);
            tv.setTextColor(context.getColor(R.color.ticket_progress_text));
        } else if ("solved".equals(state)) {
            tv.setBackgroundResource(R.drawable.bg_tenant_ticket_status_solved);
            tv.setTextColor(context.getColor(R.color.ticket_solved_text));
        } else {
            tv.setBackgroundResource(R.drawable.bg_tenant_ticket_status_raised);
            tv.setTextColor(context.getColor(R.color.ticket_raised_text));
        }
    }

    private String prettyStatus(String status) {
        if ("progress".equals(status)) return "In progress";
        if ("solved".equals(status)) return "Solved";
        return "Raised";
    }

    private String buildDateLabel(Ticket item, String status, String dateSuffix) {
        if ("progress".equals(status) && item.getArrivalDate() != null && !item.getArrivalDate().isEmpty()) {
            return "Scheduled: " + item.getArrivalDate();
        }
        if ("solved".equals(status)) {
            return "Updated: " + dateSuffix;
        }
        return "Reported: " + parseDate(item.getCreatedAt());
    }

    private String canonicalStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            return "raised";
        }

        String normalized = rawStatus.trim()
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ");

        if ("in progress".equals(normalized) || "in process".equals(normalized)) {
            return "progress";
        }

        if ("resolved".equals(normalized) || "closed".equals(normalized) || "solved".equals(normalized)) {
            return "solved";
        }

        if ("open".equals(normalized) || "raised".equals(normalized)) {
            return "raised";
        }

        return "raised";
    }

    private void applyPriorityChip(TextView chip, String priority) {
        int bg;
        int text;
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
        selectedPriority = priority == null ? "" : priority.trim();
        selectedState = state == null ? "" : state.trim();
        selectedSort = sortType == null ? "" : sortType.trim();
        this.aiOnly = aiOnly;
        reapplyFilters();
    }

    private void reapplyFilters() {
        filtered.clear();
        String query = searchQuery.toLowerCase(Locale.ROOT).trim();
        String normalizedState = normalizeSelectedState(selectedState);

        for (Ticket t : all) {
            String building = t.getBuilding() != null ? t.getBuilding() : "";
            String apartment = t.getApartmentName() != null ? t.getApartmentName() : "";
            String room = t.getRoom() != null ? t.getRoom() : "";
            String priority = t.getPriority() != null ? t.getPriority() : "";
            String status = t.getStatus() != null ? t.getStatus() : "";
            String category = t.getCategory() != null ? t.getCategory() : "";
            String hay = (building + " " + apartment + " " + room + " " + category + " " + priority + " " + status)
                    .toLowerCase(Locale.ROOT);

            boolean matchesSearch = query.isEmpty() || hay.contains(query);
            boolean matchesPriority = selectedPriority.isEmpty()
                    || (t.getPriority() != null && t.getPriority().equalsIgnoreCase(selectedPriority));
            boolean matchesState = normalizedState.isEmpty()
                    || normalizedState.equals(canonicalStatus(t.getStatus()));
            boolean matchesAi = !this.aiOnly
                    || (t.getPrioritySource() != null
                    && t.getPrioritySource().equalsIgnoreCase("AI"));

            if (matchesSearch && matchesPriority && matchesState && matchesAi) {
                filtered.add(t);
            }
        }

        if ("Building".equalsIgnoreCase(selectedSort)) {
            filtered.sort((a, b) -> (a.getBuilding() != null ? a.getBuilding() : "")
                    .compareToIgnoreCase(b.getBuilding() != null ? b.getBuilding() : ""));
        } else if ("Priority".equalsIgnoreCase(selectedSort)) {
            filtered.sort((a, b) -> Integer.compare(getPriorityRank(b.getPriority()), getPriorityRank(a.getPriority())));
        } else if ("Date".equalsIgnoreCase(selectedSort)) {
            filtered.sort((a, b) -> Long.compare(getTicketSeconds(b), getTicketSeconds(a)));
        }
        notifyDataSetChanged();
    }

    private String normalizeSelectedState(String rawState) {
        if (rawState == null || rawState.trim().isEmpty()) {
            return "";
        }
        return canonicalStatus(rawState);
    }

    private int getPriorityRank(String p) {
        if (p == null) return 0;
        switch (p.toLowerCase(Locale.ROOT)) {
            case "high":
                return 3;
            case "medium":
                return 2;
            case "low":
                return 1;
            default:
                return 0;
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
