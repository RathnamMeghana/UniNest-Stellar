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

import java.util.List;
import java.util.Locale;

public class BillAdapter extends RecyclerView.Adapter<BillAdapter.BillViewHolder> {

    private List<BillsRequest> billList;
    private OnBillClickListener listener;
    private String currentUserId; // Current user ID

    // Listener interface for handling button clicks
    public interface OnBillClickListener {
        void onBillClick(BillsRequest bill, int position);
    }

    public BillAdapter(List<BillsRequest> billList, String currentUserId, OnBillClickListener listener) {
        this.billList = billList;
        this.listener = listener;
        this.currentUserId = currentUserId;
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

        // Set bill title
        holder.tvTitle.setText(bill.getTitle() != null ? bill.getTitle() : "Untitled Bill");

        // Amount owed by current user
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

        // Total amount of the bill
        holder.tvTotalAmount.setText(String.format(Locale.getDefault(), "Total: €%.2f", bill.getTotalAmount()));

        // Gray out if nothing owed
        holder.itemView.setAlpha(amountOwed > 0 ? 1f : 0.65f);

        // Button enabled only if user owes money
        holder.btnMarkPaid.setEnabled(amountOwed > 0);
        holder.btnMarkPaid.setAlpha(amountOwed > 0 ? 1f : 0.5f);

        // Button click listener
        holder.btnMarkPaid.setOnClickListener(v -> {
            if (listener != null) listener.onBillClick(bill, position);
        });
    }

    @Override
    public int getItemCount() {
        return billList == null ? 0 : billList.size();
    }

    static class BillViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTotalAmount, tvAmountOwed;
        Button btnMarkPaid;

        public BillViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvBillTitle);
            tvTotalAmount = itemView.findViewById(R.id.tvBillTotalAmount);
            tvAmountOwed = itemView.findViewById(R.id.tvBillAmountOwed);
            btnMarkPaid = itemView.findViewById(R.id.btnMarkPaid);
        }
    }
}
