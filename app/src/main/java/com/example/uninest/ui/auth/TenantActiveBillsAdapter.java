package com.example.uninest.ui.auth;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.model.BillsRequest;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TenantActiveBillsAdapter extends RecyclerView.Adapter<TenantActiveBillsAdapter.VH> {
    private final String myId;
    private final Map<String, String> roommateNameMap;
    private final OnAction listener;
    private List<BillsRequest> list;

    public interface OnAction {
        void onPay(BillsRequest bill);
    }

    public TenantActiveBillsAdapter(List<BillsRequest> list, String myId, Map<String, String> roommateNameMap, OnAction listener) {
        this.list = list;
        this.myId = myId;
        this.roommateNameMap = roommateNameMap;
        this.listener = listener;
    }

    public void setData(List<BillsRequest> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bill_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        BillsRequest bill = list.get(position);
        holder.title.setText(bill.getTitle());

        String creatorName = roommateNameMap.get(bill.getCreatorId());
        if (bill.getCreatorId() != null && bill.getCreatorId().equals(myId)) {
            creatorName = "Me";
        }
        holder.createdBy.setText("Created by: " + (creatorName != null ? creatorName : "Unknown"));
        holder.createdBy.setVisibility(View.VISIBLE);

        double myOwe = 0;
        if (bill.getSplits() != null) {
            for (BillsRequest.Split split : bill.getSplits()) {
                if (myId.equals(split.getUserId())) {
                    myOwe = split.getAmountOwed();
                    break;
                }
            }
        }

        holder.label.setText("You owe");
        holder.amount.setText(String.format(Locale.getDefault(), "\u20AC%.2f", myOwe));
        holder.subtitle.setText(String.format(Locale.getDefault(), "Total bill: \u20AC%.2f", bill.getTotalAmount()));

        Date dueDate = parseDueDate(bill.getDueDate());
        boolean isOverdue = isOverdue(dueDate);
        boolean isDueToday = isDueToday(dueDate);

        if (isOverdue) {
            holder.badge.setText("OVERDUE");
            holder.badge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.app_danger));
            holder.badge.setBackgroundResource(R.drawable.bg_status_overdue);
            holder.cardContainer.setBackgroundResource(R.drawable.bg_card_border_raised);
            holder.amount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.app_danger));
            holder.date.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.app_danger));
            holder.date.setText("Was due: " + formatDueDate(dueDate, bill.getDueDate()));
            styleActionButton(holder.btn, R.color.app_danger, android.R.color.white);
        } else if (isDueToday) {
            holder.badge.setText("DUE TODAY");
            holder.badge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.app_danger));
            holder.badge.setBackgroundResource(R.drawable.bg_status_overdue);
            holder.cardContainer.setBackgroundResource(R.drawable.bg_card_border_raised);
            holder.amount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.app_danger));
            holder.date.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.app_danger));
            holder.date.setText("Due today");
            styleActionButton(holder.btn, R.color.app_danger, android.R.color.white);
        } else {
            holder.badge.setText("UNPAID");
            holder.badge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.app_accent_pink));
            holder.badge.setBackgroundResource(R.drawable.bg_status_pending);
            holder.cardContainer.setBackgroundResource(R.drawable.bg_card_orange);
            holder.amount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.app_accent_pink));
            holder.date.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.app_text_secondary));
            holder.date.setText("Due: " + formatDueDate(dueDate, bill.getDueDate()));
            styleActionButton(holder.btn, R.color.app_accent_purple, android.R.color.white);
        }

        holder.btn.setVisibility(View.VISIBLE);
        holder.btn.setText("Pay now");
        holder.btn.setOnClickListener(v -> listener.onPay(bill));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private void styleActionButton(MaterialButton button, int backgroundColorRes, int textColorRes) {
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(button.getContext(), backgroundColorRes)));
        button.setTextColor(ContextCompat.getColor(button.getContext(), textColorRes));
    }

    private String formatDueDate(Date dueDate, String fallback) {
        if (dueDate == null) {
            return fallback == null || fallback.isBlank() ? "--" : fallback;
        }
        return new SimpleDateFormat("dd MMM yyyy", Locale.US).format(dueDate);
    }

    private Date parseDueDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd"
        };

        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern, Locale.US).parse(raw);
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private boolean isDueToday(Date date) {
        if (date == null) {
            return false;
        }
        Calendar today = Calendar.getInstance();
        Calendar due = Calendar.getInstance();
        due.setTime(date);
        return today.get(Calendar.YEAR) == due.get(Calendar.YEAR)
                && today.get(Calendar.DAY_OF_YEAR) == due.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isOverdue(Date date) {
        if (date == null) {
            return false;
        }
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar due = Calendar.getInstance();
        due.setTime(date);
        due.set(Calendar.HOUR_OF_DAY, 0);
        due.set(Calendar.MINUTE, 0);
        due.set(Calendar.SECOND, 0);
        due.set(Calendar.MILLISECOND, 0);

        return due.before(today);
    }

    static class VH extends RecyclerView.ViewHolder {
        View cardContainer;
        TextView title, badge, label, amount, subtitle, createdBy, date;
        MaterialButton btn;

        VH(View view) {
            super(view);
            cardContainer = view.findViewById(R.id.cardContainer);
            title = view.findViewById(R.id.tvBillTitle);
            badge = view.findViewById(R.id.tvBillStatusBadge);
            label = view.findViewById(R.id.tvAmountLabel);
            amount = view.findViewById(R.id.tvMainAmount);
            subtitle = view.findViewById(R.id.tvBillSubtitle);
            createdBy = view.findViewById(R.id.tvBillCreatedBy);
            date = view.findViewById(R.id.tvBillDateInfo);
            btn = view.findViewById(R.id.btnBillAction);
        }
    }
}
