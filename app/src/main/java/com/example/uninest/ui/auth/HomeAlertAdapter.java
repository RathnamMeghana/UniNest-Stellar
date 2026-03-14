package com.example.uninest.ui.auth;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_alert, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        HomeAlert alert = alerts.get(position);
        holder.tvTitle.setText(alert.getTitle() != null ? alert.getTitle() : "");
        holder.tvSubtitle.setText(formatSubtitle(alert));

        AlertPalette palette = paletteFor(holder.itemView.getContext(), alert.getType());
        holder.tvType.setText(palette.label);
        holder.icon.setImageResource(palette.iconRes);
        holder.icon.setImageTintList(ColorStateList.valueOf(palette.accentColor));
        holder.btnDelete.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.app_text_secondary)));
        holder.accentBar.setBackgroundColor(palette.accentColor);

        tintShape(holder.tvType, withAlpha(palette.accentColor, 0.14f), withAlpha(palette.accentColor, 0.28f));
        tintShape(holder.iconContainer, withAlpha(palette.accentColor, 0.12f), withAlpha(palette.accentColor, 0.22f));
        holder.tvType.setTextColor(palette.accentColor);

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

    private void tintShape(View view, @ColorInt int fillColor, @ColorInt int strokeColor) {
        GradientDrawable drawable = (GradientDrawable) view.getBackground().mutate();
        drawable.setColor(fillColor);
        drawable.setStroke(dp(view, 1), strokeColor);
    }

    private int dp(View view, int value) {
        float density = view.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    @ColorInt
    private int withAlpha(@ColorInt int color, float alpha) {
        int alphaChannel = Math.round(255 * alpha);
        return (color & 0x00FFFFFF) | (alphaChannel << 24);
    }

    private AlertPalette paletteFor(android.content.Context context, String type) {
        String normalized = type != null ? type.trim().toUpperCase(Locale.getDefault()) : "MESSAGE";

        switch (normalized) {
            case "CHORE":
                return new AlertPalette(
                        "Chore",
                        ContextCompat.getColor(context, R.color.app_accent_green),
                        R.drawable.ic_trash_bin
                );
            case "MAINTENANCE":
                return new AlertPalette(
                        "Maintenance",
                        ContextCompat.getColor(context, R.color.app_warning),
                        R.drawable.ic_tools
                );
            case "RENT":
                return new AlertPalette(
                        "Bills",
                        ContextCompat.getColor(context, R.color.app_danger),
                        R.drawable.ic_money_wings
                );
            case "CALENDAR":
                return new AlertPalette(
                        "Calendar",
                        ContextCompat.getColor(context, R.color.app_accent_cyan),
                        R.drawable.ic_calendar
                );
            case "MESSAGE":
            default:
                return new AlertPalette(
                        "Message",
                        ContextCompat.getColor(context, R.color.app_accent_purple),
                        R.drawable.ic_notifications
                );
        }
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
        long diffDays = diffMillis / (1000L * 60 * 60 * 24);

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
        TextView tvTitle, tvSubtitle, tvType;
        ImageView icon;
        View accentBar;
        ImageButton btnDelete;
        FrameLayout iconContainer;

        AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAlertTitle);
            tvSubtitle = itemView.findViewById(R.id.tvAlertSubtitle);
            tvType = itemView.findViewById(R.id.tvAlertType);
            icon = itemView.findViewById(R.id.imgAlertIcon);
            accentBar = itemView.findViewById(R.id.viewAlertAccent);
            btnDelete = itemView.findViewById(R.id.btnDeleteAlert);
            iconContainer = itemView.findViewById(R.id.layoutIconBadge);
        }
    }

    private static class AlertPalette {
        final String label;
        final int accentColor;
        final int iconRes;

        AlertPalette(String label, int accentColor, int iconRes) {
            this.label = label;
            this.accentColor = accentColor;
            this.iconRes = iconRes;
        }
    }
}
