package com.example.uninest.ui.auth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.model.OwedToUser;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TenantOwedToMeAdapter extends RecyclerView.Adapter<TenantOwedToMeAdapter.VH> {
    private List<OwedToUser> list;
    private final Map<String, String> names;

    public TenantOwedToMeAdapter(List<OwedToUser> list, Map<String, String> names) {
        this.list = list;
        this.names = names;
    }

    public void setData(List<OwedToUser> l) {
        this.list = l;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_bill_card, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int p) {
        OwedToUser o = list.get(p);
        h.title.setText(o.getBillTitle());
        h.itemView.findViewById(R.id.cardContainer).setBackgroundResource(R.drawable.bg_tenant_bill_card_awaiting);

        h.badge.setText("AWAITING");
        h.badge.setBackgroundResource(R.drawable.bg_tenant_bill_status_awaiting);
        h.badge.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.bill_awaiting_badge_text));

        h.createdBy.setVisibility(View.VISIBLE);
        h.createdBy.setText("By Me");

        h.label.setText("Expected back");
        h.amount.setText(String.format(Locale.getDefault(), "\u20AC%.2f", o.getAmountOwed()));
        h.amount.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.bill_awaiting_text));

        String debtor = names.get(o.getDebtorUserId());
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        h.subtitle.setVisibility(View.VISIBLE);
        h.subtitle.setText(String.format(
                Locale.getDefault(),
                "Waiting on %s  |  Due %s",
                (debtor != null ? debtor : "Roommate"),
                (o.getDueDate() != null ? sdf.format(o.getDueDate()) : "--")
        ));
        h.subtitle.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.calendar_text_secondary));

        h.date.setVisibility(View.GONE);
        h.btn.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, badge, label, amount, subtitle, date, createdBy;
        MaterialButton btn;

        VH(View v) {
            super(v);
            title = v.findViewById(R.id.tvBillTitle);
            badge = v.findViewById(R.id.tvBillStatusBadge);
            label = v.findViewById(R.id.tvAmountLabel);
            amount = v.findViewById(R.id.tvMainAmount);
            subtitle = v.findViewById(R.id.tvBillSubtitle);
            date = v.findViewById(R.id.tvBillDateInfo);
            createdBy = v.findViewById(R.id.tvBillCreatedBy);
            btn = v.findViewById(R.id.btnBillAction);
        }
    }
}
