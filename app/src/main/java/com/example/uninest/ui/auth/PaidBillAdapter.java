package com.example.uninest.ui.auth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.model.BillSplitRequest;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bill, parent, false);
        return new PaidViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaidViewHolder holder, int position) {
        BillSplitRequest split = paidList.get(position);

        // Bill title (denormalized from backend)
        holder.tvTitle.setText(split.getBillTitle());

        // Amount
        holder.tvAmount.setText(String.format("Paid: €%.2f", split.getAmountOwed()));

        // Paid date
        if (split.getPaidAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            holder.tvDate.setText("Paid on: " + sdf.format(split.getPaidAt()));
        } else {
            holder.tvDate.setText("Paid");
        }

        // Visual distinction for paid items
        holder.itemView.setAlpha(0.65f);
    }

    @Override
    public int getItemCount() {
        return paidList == null ? 0 : paidList.size();
    }

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
