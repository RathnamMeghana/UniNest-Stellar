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
import com.google.android.material.card.MaterialCardView;

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
        AlertPalette palette = paletteFor(holder.itemView.getContext(), alert.getType());

        holder.tvType.setText(palette.label);
        holder.tvTitle.setText(alert.getTitle() != null && !alert.getTitle().trim().isEmpty()
                ? alert.getTitle().trim()
                : "New update");
        String metaText = formatMeta(alert);
        holder.tvMeta.setText(metaText);
        holder.tvMeta.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                isUrgentMeta(metaText) ? R.color.app_danger : R.color.app_text_secondary
        ));
        String detailText = detailFor(alert);
        boolean showSubtitle = shouldShowSubtitle(alert, detailText);
        holder.tvSubtitle.setText(detailText);
        holder.tvTitle.setMaxLines(showSubtitle ? 1 : 2);
        holder.tvSubtitle.setVisibility(showSubtitle ? View.VISIBLE : View.GONE);

        holder.icon.setImageResource(palette.iconRes);
        holder.icon.setImageTintList(ColorStateList.valueOf(palette.accentColor));
        holder.btnDelete.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.getContext(), R.color.app_text_secondary)
        ));
        holder.cardAlert.setCardBackgroundColor(palette.surfaceColor);
        holder.cardAlert.setStrokeColor(palette.borderColor);

        tintShape(holder.tvType, withAlpha(palette.accentColor, 0.13f), withAlpha(palette.accentColor, 0.26f));
        tintShape(holder.iconContainer, withAlpha(palette.accentColor, 0.12f), withAlpha(palette.accentColor, 0.18f));
        holder.tvType.setTextColor(palette.accentColor);
        holder.itemView.setContentDescription(buildAlertContentDescription(holder, palette));
        holder.btnDelete.setContentDescription("Delete " + palette.label.toLowerCase(Locale.getDefault()) + " alert");
        holder.btnDelete.setVisibility(alert.isDismissible() ? View.VISIBLE : View.GONE);
        holder.btnDelete.setOnClickListener(null);

        holder.itemView.setOnClickListener(v -> {
            if (alertClickListener != null) {
                alertClickListener.onAlertClick(alert);
            }
        });

        if (alert.isDismissible()) {
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteClickListener != null) {
                    deleteClickListener.onDelete(alert);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    private String detailFor(HomeAlert alert) {
        String original = alert.getSubtitle() != null ? alert.getSubtitle().trim() : "";
        String title = alert.getTitle() != null ? alert.getTitle().trim() : "";

        if (!original.isEmpty() && !original.equalsIgnoreCase(title)) {
            return original;
        }

        String target = alert.getTargetScreen() != null
                ? alert.getTargetScreen().trim().toUpperCase(Locale.getDefault())
                : "";

        switch (target) {
            case "BILLS":
                return "Opens the bills section for quick action.";
            case "CHORES":
                return "Opens the chores section with the related task.";
            case "CALENDAR":
                return "Opens the planner on the related day.";
            case "TICKETS":
                return "Opens the ticket details so you can review it.";
            default:
                return "Tap to view the related update.";
        }
    }

    private String buildAlertContentDescription(AlertViewHolder holder, AlertPalette palette) {
        StringBuilder description = new StringBuilder();
        description.append(palette.label).append(" alert. ");
        description.append(holder.tvTitle.getText()).append(". ");
        description.append(holder.tvMeta.getText()).append(". ");
        if (holder.tvSubtitle.getVisibility() == View.VISIBLE
                && holder.tvSubtitle.getText() != null
                && holder.tvSubtitle.getText().length() > 0) {
            description.append(holder.tvSubtitle.getText());
        }
        return description.toString().trim();
    }

    private boolean shouldShowSubtitle(HomeAlert alert, String detailText) {
        if (detailText == null || detailText.trim().isEmpty()) {
            return false;
        }

        String type = alert != null && alert.getType() != null
                ? alert.getType().trim().toUpperCase(Locale.getDefault())
                : "";
        return "MESSAGE".equals(type);
    }

    private String formatMeta(HomeAlert alert) {
        String metaOverride = alert.getMetaOverride() != null ? alert.getMetaOverride().trim() : "";
        if (!metaOverride.isEmpty()) {
            return metaOverride;
        }

        long time = alert.getEventTime() > 0 ? alert.getEventTime() : alert.getCreatedAt();
        if (time <= 0) {
            return "Recent";
        }

        Calendar now = Calendar.getInstance();
        Calendar event = Calendar.getInstance();
        event.setTimeInMillis(time);

        if (isSameDay(now, event)) {
            return "Today at " + new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date(time));
        }

        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        if (isSameDay(tomorrow, event)) {
            return "Tomorrow";
        }

        if (isSameWeek(now, event)) {
            return new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date(time));
        }

        return new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date(time));
    }

    private boolean isUrgentMeta(String metaText) {
        if (metaText == null) {
            return false;
        }

        String normalized = metaText.trim().toLowerCase(Locale.getDefault());
        return normalized.contains("overdue") || normalized.contains("due today");
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
                        ContextCompat.getColor(context, R.color.calendar_chore_text),
                        ContextCompat.getColor(context, R.color.calendar_chore_bg),
                        ContextCompat.getColor(context, R.color.calendar_chore_border),
                        R.drawable.ic_alert_chore
                );
            case "MAINTENANCE":
                return new AlertPalette(
                        "Maintenance",
                        ContextCompat.getColor(context, R.color.alert_maintenance_accent),
                        ContextCompat.getColor(context, R.color.alert_maintenance_bg),
                        ContextCompat.getColor(context, R.color.alert_maintenance_border),
                        R.drawable.ic_alert_maintenance
                );
            case "RENT":
                return new AlertPalette(
                        "Bills",
                        ContextCompat.getColor(context, R.color.calendar_bill_text),
                        ContextCompat.getColor(context, R.color.calendar_bill_bg),
                        ContextCompat.getColor(context, R.color.calendar_bill_border),
                        R.drawable.ic_alert_bills
                );
            case "CALENDAR":
                return new AlertPalette(
                        "Event",
                        ContextCompat.getColor(context, R.color.calendar_event_text),
                        ContextCompat.getColor(context, R.color.calendar_event_bg),
                        ContextCompat.getColor(context, R.color.calendar_event_border),
                        R.drawable.ic_alert_calendar
                );
            case "MESSAGE":
            default:
                return new AlertPalette(
                        "Message",
                        ContextCompat.getColor(context, R.color.alert_message_accent),
                        ContextCompat.getColor(context, R.color.alert_message_bg),
                        ContextCompat.getColor(context, R.color.alert_message_border),
                        R.drawable.ic_alert_message
                );
        }
    }

    private boolean isSameWeek(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.WEEK_OF_YEAR) == second.get(Calendar.WEEK_OF_YEAR);
    }

    private boolean isSameDay(Calendar first, Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardAlert;
        TextView tvTitle;
        TextView tvSubtitle;
        TextView tvType;
        TextView tvMeta;
        ImageView icon;
        ImageButton btnDelete;
        FrameLayout iconContainer;

        AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            cardAlert = itemView.findViewById(R.id.cardAlert);
            tvTitle = itemView.findViewById(R.id.tvAlertTitle);
            tvSubtitle = itemView.findViewById(R.id.tvAlertSubtitle);
            tvType = itemView.findViewById(R.id.tvAlertType);
            tvMeta = itemView.findViewById(R.id.tvAlertMeta);
            icon = itemView.findViewById(R.id.imgAlertIcon);
            btnDelete = itemView.findViewById(R.id.btnDeleteAlert);
            iconContainer = itemView.findViewById(R.id.layoutIconBadge);
        }
    }

    private static class AlertPalette {
        final String label;
        final int accentColor;
        final int surfaceColor;
        final int borderColor;
        final int iconRes;

        AlertPalette(String label, int accentColor, int surfaceColor, int borderColor, int iconRes) {
            this.label = label;
            this.accentColor = accentColor;
            this.surfaceColor = surfaceColor;
            this.borderColor = borderColor;
            this.iconRes = iconRes;
        }
    }
}
