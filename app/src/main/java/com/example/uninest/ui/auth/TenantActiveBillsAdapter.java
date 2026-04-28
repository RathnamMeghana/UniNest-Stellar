package com.example.uninest.ui.auth;

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
        holder.createdBy.setVisibility(View.VISIBLE);
        holder.createdBy.setText("By " + (creatorName != null ? creatorName : "Unknown"));
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
        holder.date.setVisibility(View.GONE);

        Date dueDate = parseDueDate(bill.getDueDate());
        boolean isOverdue = isOverdue(dueDate);
        boolean isDueToday = isDueToday(dueDate);
        String dueText = dueDate != null
                ? new SimpleDateFormat("MMM d", Locale.US).format(dueDate)
                : "Soon";

        if (isOverdue) {
            holder.badge.setText("OVERDUE");
            holder.badge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.bill_overdue_badge_text));
            holder.badge.setBackgroundResource(R.drawable.bg_tenant_bill_status_overdue);
            holder.cardContainer.setBackgroundResource(R.drawable.bg_tenant_bill_card_overdue);
            holder.amount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.bill_overdue_text));
            holder.subtitle.setText(String.format(
                    Locale.getDefault(),
                    "Was due %s  |  Total EUR %.2f",
                    dueText,
                    bill.getTotalAmount()
            ));
            holder.subtitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.bill_overdue_text));
        } else if (isDueToday) {
            holder.badge.setText("DUE TODAY");
            holder.badge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.bill_overdue_badge_text));
            holder.badge.setBackgroundResource(R.drawable.bg_tenant_bill_status_overdue);
            holder.cardContainer.setBackgroundResource(R.drawable.bg_tenant_bill_card_overdue);
            holder.amount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.bill_overdue_text));
            holder.subtitle.setText(String.format(
                    Locale.getDefault(),
                    "Due today  |  Total EUR %.2f",
                    bill.getTotalAmount()
            ));
            holder.subtitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.bill_overdue_text));
        } else {
            holder.badge.setText("UNPAID");
            holder.badge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.bill_due_badge_text));
            holder.badge.setBackgroundResource(R.drawable.bg_tenant_bill_status_due);
            holder.cardContainer.setBackgroundResource(R.drawable.bg_tenant_bill_card_due);
            holder.amount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.bill_due_text));
            holder.subtitle.setText(String.format(
                    Locale.getDefault(),
                    "Due %s  |  Total EUR %.2f",
                    dueText,
                    bill.getTotalAmount()
            ));
            holder.subtitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.calendar_text_secondary));
        }

        holder.btn.setVisibility(View.VISIBLE);
        holder.btn.setText("Pay now");
        holder.btn.setOnClickListener(v -> listener.onPay(bill));
    }

    @Override
    public int getItemCount() {
        return list.size();
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
