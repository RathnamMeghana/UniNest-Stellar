package com.example.uninest.ui.auth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.model.BillsRequest;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class BillAdapter extends RecyclerView.Adapter<BillAdapter.BillViewHolder> {

    private List<BillsRequest> billList;
    private OnBillClickListener listener;
    private String currentUserId;

    public interface OnBillClickListener {
        void onBillClick(BillsRequest bill, int position);
    }

    public BillAdapter(List<BillsRequest> billList, String currentUserId, OnBillClickListener listener) {
        this.billList = billList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void updateData(List<BillsRequest> newList) {
        this.billList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bill, parent, false);
        return new BillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BillViewHolder holder, int position) {
        BillsRequest bill = billList.get(position);

        // Bill title
        holder.tvTitle.setText(bill.getTitle());

        // Total amount
        holder.tvTotalAmount.setText(String.format(Locale.getDefault(), "Total: €%.2f", bill.getTotalAmount()));

        // Amount owed by this user
        double amountOwed = 0;
        if (bill.getSplits() != null) {
            for (BillsRequest.Split split : bill.getSplits()) {
                if (currentUserId.equals(split.getUserId()) && !split.isPaid()) {
                    amountOwed = split.getAmountOwed();
                    break;
                }
            }
        }
        holder.tvAmountOwed.setText(String.format(Locale.getDefault(), "You owe: €%.2f", amountOwed));
        holder.tvAmountOwed.setTextColor(holder.tvAmountOwed.getResources().getColor(R.color.black));

        // Due date
        if (bill.getDueDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            holder.tvDate.setText("Due: " + sdf.format(bill.getDueDate()));
        } else {
            holder.tvDate.setText("Due: --");
        }

        // Enable or disable button based on amount owed
        holder.btnMarkPaid.setVisibility(amountOwed > 0 ? View.VISIBLE : View.GONE);

        // Button click
        holder.btnMarkPaid.setOnClickListener(v -> {
            if (listener != null) listener.onBillClick(bill, position);
        });

        // Gray out if nothing owed
        holder.itemView.setAlpha(amountOwed > 0 ? 1f : 0.65f);
    }

    @Override
    public int getItemCount() {
        return billList == null ? 0 : billList.size();
    }

    static class BillViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTotalAmount, tvAmountOwed, tvDate;
        Button btnMarkPaid;

        public BillViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvBillTitle);
            tvTotalAmount = itemView.findViewById(R.id.tvBillTotalAmount);
            tvAmountOwed = itemView.findViewById(R.id.tvBillAmountOwed);
            tvDate = itemView.findViewById(R.id.tvBillDueDate);
            btnMarkPaid = itemView.findViewById(R.id.btnMarkPaid);
        }
    }

}

