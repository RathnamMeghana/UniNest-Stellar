package com.example.uninest.ui.auth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.model.BillSplitRequest;

import java.util.List;

public class PaidBillAdapter extends RecyclerView.Adapter<PaidBillAdapter.PaidViewHolder> {
    private List<BillSplitRequest> paidList;

    public PaidBillAdapter(List<BillSplitRequest> paidList) {
        this.paidList = paidList;
    }

    public void updateData(List<BillSplitRequest> newList) {
        this.paidList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PaidViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bill, parent, false);
        return new PaidViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaidViewHolder holder, int position) {
        BillSplitRequest split = paidList.get(position);

        // Setting data from BillSplitRequest
        holder.tvAmount.setText(String.format("Paid: €%.2f", split.getAmountOwed()));
        holder.tvTitle.setText("Bill ID: " + split.getBillId().substring(0, 8));
        holder.tvDate.setText("Status: Paid");

        // Styling for paid items
        holder.itemView.setAlpha(0.7f);
    }

    @Override
    public int getItemCount() { return paidList.size(); }

    static class PaidViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAmount, tvDate;
        public PaidViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvBillTitle);
            tvAmount = itemView.findViewById(R.id.tvBillAmount);
            tvDate = itemView.findViewById(R.id.tvBillDueDate);
        }
    }
}
