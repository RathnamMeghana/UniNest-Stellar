package com.example.uninest.ui.auth;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uninest.R;
import com.example.uninest.model.HomeAlert;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeAlertAdapter extends RecyclerView.Adapter<HomeAlertAdapter.AlertViewHolder> {

    public interface OnDeleteClickListener {
        void onDelete(HomeAlert alert);
    }

    public interface OnAlertClickListener {
        void onAlertClick(HomeAlert alert);
    }

    private final List<HomeAlert> alerts;
    private final OnDeleteClickListener deleteClickListener;
    private final OnAlertClickListener alertClickListener;

    public HomeAlertAdapter(
            List<HomeAlert> alerts,
            OnDeleteClickListener deleteClickListener,
            OnAlertClickListener alertClickListener
    ) {
        this.alerts = alerts;
        this.deleteClickListener = deleteClickListener;
        this.alertClickListener = alertClickListener;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_alert, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        HomeAlert alert = alerts.get(position);

        holder.tvTitle.setText(alert.getTitle() != null ? alert.getTitle() : "");
        holder.tvSubtitle.setText(formatSubtitle(alert));

        String type = alert.getType() != null ? alert.getType() : "MESSAGE";

        switch (type) {
            case "CHORE":
                holder.background.setBackgroundColor(Color.parseColor("#00C853"));
                holder.icon.setImageResource(R.drawable.ic_trash_bin);
                break;
            case "MAINTENANCE":
                holder.background.setBackgroundColor(Color.parseColor("#FF7043"));
                holder.icon.setImageResource(R.drawable.ic_tools);
                break;
            case "RENT":
                holder.background.setBackgroundColor(Color.parseColor("#E91E63"));
                holder.icon.setImageResource(R.drawable.ic_money_wings);
                break;
            case "CALENDAR":
                holder.background.setBackgroundColor(Color.parseColor("#5C6BC0"));
                holder.icon.setImageResource(R.drawable.ic_calendar);
                break;
            case "MESSAGE":
            default:
                holder.background.setBackgroundColor(Color.parseColor("#B792D9"));
                holder.icon.setImageResource(R.drawable.ic_notifications);
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            if (alertClickListener != null) {
                alertClickListener.onAlertClick(alert);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDelete(alert);
            }
        });
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    private String formatSubtitle(HomeAlert alert) {
        long time = alert.getEventTime() > 0 ? alert.getEventTime() : alert.getCreatedAt();
        String type = alert.getType() != null ? alert.getType() : "";
        String original = alert.getSubtitle() != null ? alert.getSubtitle().trim() : "";

        if (time <= 0) {
            return original.isEmpty() ? "Recent update" : original;
        }

        Calendar now = Calendar.getInstance();
        Calendar event = Calendar.getInstance();
        event.setTimeInMillis(time);

        if ("CHORE".equals(type) && isSameWeek(now, event)) {
            return "This week";
        }

        if ("RENT".equals(type) && isSameWeek(now, event)) {
            return "Due this week";
        }

        if (isSameDay(now, event)) {
            return "Today at " + new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(time));
        }

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        if (isSameDay(tomorrow, event)) {
            return "Tomorrow";
        }

        long diffMillis = time - now.getTimeInMillis();
        long diffDays = diffMillis / (1000 * 60 * 60 * 24);

        if (diffDays >= 2 && diffDays <= 7) {
            return new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date(time));
        }

        return original.isEmpty()
                ? new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(time))
                : original;
    }

    private boolean isSameWeek(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.WEEK_OF_YEAR) == c2.get(Calendar.WEEK_OF_YEAR);
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle;
        ImageView icon;
        RelativeLayout background;
        ImageButton btnDelete;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAlertTitle);
            tvSubtitle = itemView.findViewById(R.id.tvAlertSubtitle);
            icon = itemView.findViewById(R.id.imgAlertIcon);
            background = itemView.findViewById(R.id.layoutAlertBackground);
            btnDelete = itemView.findViewById(R.id.btnDeleteAlert);
        }
    }
}