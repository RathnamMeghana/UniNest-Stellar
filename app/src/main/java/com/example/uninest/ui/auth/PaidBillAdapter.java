package com.example.uninest.ui.auth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.model.BillsRequest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PaidBillAdapter extends RecyclerView.Adapter<PaidBillAdapter.PaidViewHolder> {

    // Each item now keeps both the bill and the split
    public static class PaidItem {
        BillsRequest bill;
        BillsRequest.Split split;

        public PaidItem(BillsRequest bill, BillsRequest.Split split) {
            this.bill = bill;
            this.split = split;
        }
    }

    private List<PaidItem> paidItems;

    public PaidBillAdapter(List<PaidItem> paidItems) {
        this.paidItems = paidItems;
    }

    public void updateData(List<PaidItem> newList) {
        this.paidItems = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PaidViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bill, parent, false);
        return new PaidViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaidViewHolder holder, int position) {
        PaidItem item = paidItems.get(position);
        BillsRequest bill = item.bill;
        BillsRequest.Split split = item.split;

        // Show the bill title
        holder.tvTitle.setText(bill.getTitle() != null ? bill.getTitle() : "Untitled Bill");

        // Amount the user paid
        holder.tvAmountOwed.setText(String.format(Locale.getDefault(), "You paid: €%.2f", split.getAmountOwed()));

        // Total bill amount
        holder.tvTotalAmount.setText(String.format(Locale.getDefault(), "Total: €%.2f", bill.getTotalAmount()));

        // Paid date
        if (split.getPaidAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            holder.tvDate.setText("Paid on: " + sdf.format(split.getPaidAt()));
        } else {
            holder.tvDate.setText("Paid");
        }

        // Hide the button for paid items
        holder.btnMarkPaid.setVisibility(View.GONE);

        // Slightly gray out to indicate it's paid
        holder.itemView.setAlpha(0.65f);
    }

    @Override
    public int getItemCount() {
        return paidItems == null ? 0 : paidItems.size();
    }

    static class PaidViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAmountOwed, tvTotalAmount, tvDate;
        Button btnMarkPaid;

        public PaidViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvBillTitle);
            tvAmountOwed = itemView.findViewById(R.id.tvBillAmountOwed);
            tvTotalAmount = itemView.findViewById(R.id.tvBillTotalAmount);
            tvDate = itemView.findViewById(R.id.tvBillDueDate);
            btnMarkPaid = itemView.findViewById(R.id.btnMarkPaid);
        }
    }

    // Helper to convert BillsRequest list to PaidItem list
    public static List<PaidItem> buildPaidItems(List<BillsRequest> bills, String currentUserId) {
        List<PaidItem> result = new ArrayList<>();
        for (BillsRequest bill : bills) {
            if (bill.getSplits() != null) {
                for (BillsRequest.Split split : bill.getSplits()) {
                    if (split.isPaid() && currentUserId.equals(split.getUserId())) {
                        result.add(new PaidItem(bill, split));
                    }
                }
            }
        }
        return result;
    }
}
