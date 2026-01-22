package com.example.uninest.ui.auth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.model.BillsRequest;

import java.text.DateFormat;
import java.util.List;

public class BillAdapter extends RecyclerView.Adapter<BillAdapter.BillViewHolder> {
    private List<BillsRequest> billList;

    public BillAdapter(List<BillsRequest> billList) {
        this.billList = billList;
    }

    public void updateData(List<BillsRequest> newList) {
        this.billList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bill, parent, false);
        return new BillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BillViewHolder holder, int position) {
        BillsRequest bill = billList.get(position);
        holder.tvTitle.setText(bill.getTitle());
        holder.tvAmount.setText(String.format("Total: €%.2f", bill.getTotalAmount()));

        if (bill.getDueDate() != null) {
            holder.tvDate.setText("Due: " + DateFormat.getDateInstance().format(bill.getDueDate()));
        }
    }

    @Override
    public int getItemCount() { return billList.size(); }

    static class BillViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvAmount, tvDate;
        public BillViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvBillTitle);
            tvAmount = itemView.findViewById(R.id.tvBillAmount);
            tvDate = itemView.findViewById(R.id.tvBillDueDate);
        }
    }
}