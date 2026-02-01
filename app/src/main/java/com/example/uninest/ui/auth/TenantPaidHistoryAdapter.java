package com.example.uninest.ui.auth;

import android.graphics.Color;
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
import java.util.Map;

public class TenantPaidHistoryAdapter extends RecyclerView.Adapter<TenantPaidHistoryAdapter.VH> {
    private List<BillsRequest.Split> list;
    private Map<String, String> roommateNameMap;
    private String myId;

    public TenantPaidHistoryAdapter(List<BillsRequest.Split> list, Map<String, String> map, String myId) {
        this.list = list; this.roommateNameMap = map; this.myId = myId;
    }
    public void setData(List<BillsRequest.Split> l) { this.list = l; notifyDataSetChanged(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_bill_card, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int p) {
        BillsRequest.Split s = list.get(p);
        h.title.setText(s.getBillTitle());


        h.itemView.findViewById(R.id.cardContainer).setBackgroundResource(R.drawable.bg_card_green);

        h.badge.setText("SETTLED");
        h.badge.setBackgroundResource(R.drawable.bg_status_completed);
        h.badge.setTextColor(Color.parseColor("#2E7D32"));

        h.createdBy.setVisibility(View.VISIBLE);

        String creatorId = s.getCreatorId();
        String creatorName = roommateNameMap.get(creatorId);

        if (creatorId != null && creatorId.equals(myId)) {
            creatorName = "Me";
        }

        h.createdBy.setText("Created by: " + (creatorName != null ? creatorName : "Unknown"));


        // Name Logic: "You paid" vs "Roommate paid you"
        if (s.getUserId().equals(myId)) {
            h.label.setText("You paid");
            h.subtitle.setVisibility(View.GONE);
        } else {
            h.label.setText("Received");
            String payer = roommateNameMap.get(s.getUserId());
            h.subtitle.setText("Paid by: " + (payer != null ? payer : "Roommate"));
            h.subtitle.setVisibility(View.VISIBLE);
        }

        h.amount.setText(String.format(Locale.getDefault(), "€%.2f", s.getAmountOwed()));
        h.amount.setTextColor(Color.parseColor("#2E7D32"));
        h.label.setTextColor(Color.parseColor("#2E7D32"));

        if (s.getPaidAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            h.date.setText("Finished on: " + sdf.format(s.getPaidAt()));
        } else {
            h.date.setText("Finished on: --");
        }


        h.btn.setVisibility(View.GONE);
        h.itemView.setAlpha(0.85f);
    }

    @Override public int getItemCount() { return list.size(); }
    static class VH extends RecyclerView.ViewHolder {
        TextView title, badge, label, amount, subtitle, date, createdBy; View btn;
        VH(View v) { super(v);
            title = v.findViewById(R.id.tvBillTitle); badge = v.findViewById(R.id.tvBillStatusBadge);
            label = v.findViewById(R.id.tvAmountLabel); amount = v.findViewById(R.id.tvMainAmount);
            subtitle = v.findViewById(R.id.tvBillSubtitle); date = v.findViewById(R.id.tvBillDateInfo); createdBy = v.findViewById(R.id.tvBillCreatedBy);
            btn = v.findViewById(R.id.btnBillAction);
        }
    }
}