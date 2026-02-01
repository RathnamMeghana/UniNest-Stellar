package com.example.uninest.ui.auth;

import android.graphics.Color;
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
import android.widget.Button;
import java.util.Map;

public class TenantOwedToMeAdapter extends RecyclerView.Adapter<TenantOwedToMeAdapter.VH> {
    private List<OwedToUser> list;
    private Map<String, String> names;
    public TenantOwedToMeAdapter(List<OwedToUser> list, Map<String, String> names) { this.list = list; this.names = names; }
    public void setData(List<OwedToUser> l) { this.list = l; notifyDataSetChanged(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_bill_card, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int p) {
        OwedToUser o = list.get(p);
        h.title.setText(o.getBillTitle());
        h.badge.setText("PENDING");
        h.badge.setBackgroundResource(R.drawable.bg_status_progress);
        h.badge.setTextColor(Color.parseColor("#EF6C00"));
        h.createdBy.setVisibility(View.VISIBLE);
        h.createdBy.setText("Created by: Me");
        h.label.setText("You are owed");
        h.amount.setText(String.format(Locale.getDefault(), "€%.2f", o.getAmountOwed()));
        String debtor = names.get(o.getDebtorUserId());
        h.subtitle.setText("Owed by: " + (debtor != null ? debtor : "Roommate"));
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        h.date.setText("Due: " + (o.getDueDate() != null ? sdf.format(o.getDueDate()) : "--"));
        h.btn.setText("Remind");
    }
    @Override public int getItemCount() { return list.size(); }
    static class VH extends RecyclerView.ViewHolder {
        TextView title, badge, label, amount, subtitle, date, createdBy; Button btn;

        VH(View v) { super(v);
            title = v.findViewById(R.id.tvBillTitle); badge = v.findViewById(R.id.tvBillStatusBadge);
            label = v.findViewById(R.id.tvAmountLabel); amount = v.findViewById(R.id.tvMainAmount);
            subtitle = v.findViewById(R.id.tvBillSubtitle); date = v.findViewById(R.id.tvBillDateInfo); createdBy = v.findViewById(R.id.tvBillCreatedBy);
            btn = v.findViewById(R.id.btnBillAction);
        }
    }
}