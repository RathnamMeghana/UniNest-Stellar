package com.example.uninest.ui.auth;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.uninest.R;
import com.example.uninest.model.BillsRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TenantActiveBillsAdapter extends RecyclerView.Adapter<TenantActiveBillsAdapter.VH> {
    private List<BillsRequest> list;
    private String myId;
    private Map<String, String> roommateNameMap;
    private OnAction listener;
    public interface OnAction { void onPay(BillsRequest b); }

    public TenantActiveBillsAdapter(List<BillsRequest> list, String myId, Map<String, String> roommateNameMap, OnAction listener) {
        this.list = list;
        this.myId = myId;
        this.roommateNameMap = roommateNameMap; // Initialize the map
        this.listener = listener;
    }
    public void setData(List<BillsRequest> l) { this.list = l; notifyDataSetChanged(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_bill_card, p, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int p) {
        BillsRequest b = list.get(p);
        h.title.setText(b.getTitle());
        h.badge.setText("UNPAID");
        h.badge.setTextColor(Color.parseColor("#C62828"));

        // RESOLVE CREATOR NAME
        String creatorName = roommateNameMap.get(b.getCreatorId());
        if (b.getCreatorId() != null && b.getCreatorId().equals(myId)) {
            creatorName = "Me";
        }
        h.createdBy.setText("Created by: " + (creatorName != null ? creatorName : "Unknown"));
        h.createdBy.setVisibility(View.VISIBLE);

        double myOwe = 0;
        if (b.getSplits() != null) {
            for (BillsRequest.Split split : b.getSplits()) {
                if (myId.equals(split.getUserId())) {
                    myOwe = split.getAmountOwed();
                    break;
                }
            }
        }

        h.label.setText("You owe");
        h.amount.setText(String.format(Locale.getDefault(), "€%.2f", myOwe));
        h.subtitle.setText("Total bill: €" + String.format(Locale.getDefault(), "%.2f", b.getTotalAmount()));

        if (b.getDueDate() != null) {
            try {

                SimpleDateFormat isoInput = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                SimpleDateFormat desiredOutput = new SimpleDateFormat("dd MMM yyyy", Locale.US);
                Date date = isoInput.parse(b.getDueDate());
                h.date.setText("Due: " + desiredOutput.format(date));
            } catch (Exception e) {
                h.date.setText("Due: " + b.getDueDate());
            }
        }else {
            h.date.setText("Due: --");
        }

        h.btn.setVisibility(View.VISIBLE);
        h.btn.setText("Pay Now");
        h.btn.setOnClickListener(v -> listener.onPay(b));
    }


    @Override public int getItemCount() { return list.size(); }
    static class VH extends RecyclerView.ViewHolder {
        TextView title, badge, label, amount, subtitle,createdBy, date; Button btn;
        VH(View v) { super(v);
            title = v.findViewById(R.id.tvBillTitle); badge = v.findViewById(R.id.tvBillStatusBadge);
            label = v.findViewById(R.id.tvAmountLabel); amount = v.findViewById(R.id.tvMainAmount);
            subtitle = v.findViewById(R.id.tvBillSubtitle); createdBy = v.findViewById(R.id.tvBillCreatedBy);date = v.findViewById(R.id.tvBillDateInfo);
            btn = v.findViewById(R.id.btnBillAction);
        }
    }
}