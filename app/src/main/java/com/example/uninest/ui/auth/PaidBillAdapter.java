package com.example.uninest.ui.auth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.model.BillsRequest;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PaidBillAdapter extends RecyclerView.Adapter<PaidBillAdapter.PaidViewHolder> {

    private List<BillsRequest.Split> paidList;

    public PaidBillAdapter(List<BillsRequest.Split> paidList) {
        this.paidList = paidList;
    }

    public void updateData(List<BillsRequest.Split> newList) {
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
        BillsRequest.Split split = paidList.get(position);

        // Bill title
        holder.tvTitle.setText(split.getBillTitle() != null ? split.getBillTitle() : "Untitled Bill");

        // Total amount
        holder.tvTotalAmount.setText(String.format(Locale.getDefault(), "Total: €%.2f", split.getAmountOwed()));

        // Paid info
        holder.tvAmountOwed.setText(String.format(Locale.getDefault(), "You paid: €%.2f", split.getAmountOwed()));
        holder.tvAmountOwed.setTextColor(holder.tvAmountOwed.getResources().getColor(R.color.black));

        // Paid date
        if (split.getPaidAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            holder.tvDate.setText("Paid on: " + sdf.format(split.getPaidAt()));
        } else {
            holder.tvDate.setText("Paid");
        }

        // Hide button
        holder.btnMarkPaid.setVisibility(View.GONE);

        // Gray out visually
        holder.itemView.setAlpha(0.65f);
    }

    @Override
    public int getItemCount() {
        return paidList == null ? 0 : paidList.size();
    }

    static class PaidViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTotalAmount, tvAmountOwed, tvDate;
        TextView btnMarkPaid; // hidden for paid items

        public PaidViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvBillTitle);
            tvTotalAmount = itemView.findViewById(R.id.tvBillTotalAmount);
            tvAmountOwed = itemView.findViewById(R.id.tvBillAmountOwed);
            tvDate = itemView.findViewById(R.id.tvBillDueDate);
            btnMarkPaid = itemView.findViewById(R.id.btnMarkPaid);
        }
    }
}

