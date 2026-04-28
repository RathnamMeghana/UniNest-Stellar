package com.example.uninest.utils;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.google.android.material.button.MaterialButton;

public final class DestructiveConfirmationDialog {

    private DestructiveConfirmationDialog() {
    }

    public static void show(
            AppCompatActivity activity,
            String eyebrow,
            String title,
            String message,
            String impactMessage,
            String confirmText,
            Runnable onConfirm
    ) {
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_destructive_confirmation, null);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .create();

        TextView tvEyebrow = view.findViewById(R.id.tvConfirmEyebrow);
        TextView tvTitle = view.findViewById(R.id.tvConfirmTitle);
        TextView tvMessage = view.findViewById(R.id.tvConfirmMessage);
        TextView tvImpact = view.findViewById(R.id.tvConfirmImpact);
        View layoutImpact = view.findViewById(R.id.layoutConfirmImpact);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirmAction);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancelAction);

        bindText(tvEyebrow, eyebrow);
        bindText(tvTitle, title);
        bindText(tvMessage, message);

        if (TextUtils.isEmpty(impactMessage)) {
            layoutImpact.setVisibility(View.GONE);
        } else {
            tvImpact.setText(impactMessage);
            layoutImpact.setVisibility(View.VISIBLE);
        }

        if (!TextUtils.isEmpty(confirmText)) {
            btnConfirm.setText(confirmText);
        }

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            if (onConfirm != null) {
                onConfirm.run();
            }
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.92f),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private static void bindText(TextView textView, String value) {
        if (TextUtils.isEmpty(value)) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(value);
            textView.setVisibility(View.VISIBLE);
        }
    }
}
