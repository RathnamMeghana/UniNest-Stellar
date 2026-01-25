package com.example.uninest.ui.auth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.model.OwedToUser;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OwedToMeAdapter extends RecyclerView.Adapter<OwedToMeAdapter.OwedViewHolder> {

    private final List<OwedToUser> owedList;
    private final Map<String, String> userIdToEmail;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public OwedToMeAdapter(List<OwedToUser> owedList, Map<String, String> userIdToEmail) {
        this.owedList = owedList;
        this.userIdToEmail = userIdToEmail;
    }

    @NonNull
    @Override
    public OwedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_owed_to_me, parent, false);
        return new OwedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OwedViewHolder holder, int position) {
        OwedToUser item = owedList.get(position);

        // Bill title
        holder.tvBillTitle.setText(item.getBillTitle());

        // Amount owed
        holder.tvAmount.setText(String.format(Locale.getDefault(),
                "You are owed: €%.2f", item.getAmountOwed()));

        // Debtor email (fallback to ID if email not found)
        String debtorEmail = userIdToEmail.getOrDefault(item.getDebtorUserId(),
                item.getDebtorUserId());
        holder.tvDebtor.setText("Owed by: " + debtorEmail);

        // Due date
        if (item.getDueDate() != null) {
            holder.tvDueDate.setText("Due: " + dateFormat.format(item.getDueDate()));
        } else {
            holder.tvDueDate.setText("Due: —");
        }
    }

    @Override
    public int getItemCount() {
        return owedList == null ? 0 : owedList.size();
    }

    static class OwedViewHolder extends RecyclerView.ViewHolder {

        TextView tvBillTitle;
        TextView tvAmount;
        TextView tvDebtor;
        TextView tvDueDate;

        public OwedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBillTitle = itemView.findViewById(R.id.tvBillTitle);
            tvAmount = itemView.findViewById(R.id.tvBillAmount);
            tvDebtor = itemView.findViewById(R.id.tvBillDebtor);
            tvDueDate = itemView.findViewById(R.id.tvBillDueDate);
        }
    }
}
