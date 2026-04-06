package com.example.uninest.utils;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uninest.R;
import com.google.android.material.button.MaterialButton;

public final class NameEditDialog {

    public interface OnConfirmListener {
        void onConfirm(String updatedName);
    }

    private NameEditDialog() {
    }

    public static void show(
            AppCompatActivity activity,
            String eyebrow,
            String title,
            String message,
            String currentName,
            String hint,
            OnConfirmListener onConfirm
    ) {
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_name_update, null);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .create();

        TextView tvEyebrow = view.findViewById(R.id.tvNameDialogEyebrow);
        TextView tvTitle = view.findViewById(R.id.tvNameDialogTitle);
        TextView tvMessage = view.findViewById(R.id.tvNameDialogMessage);
        TextView tvCurrentValue = view.findViewById(R.id.tvNameDialogCurrentValue);
        EditText etInput = view.findViewById(R.id.etNameDialogInput);
        MaterialButton btnSave = view.findViewById(R.id.btnNameDialogSave);
        MaterialButton btnCancel = view.findViewById(R.id.btnNameDialogCancel);

        bindText(tvEyebrow, eyebrow);
        bindText(tvTitle, title);
        bindText(tvMessage, message);
        tvCurrentValue.setText(TextUtils.isEmpty(currentName) ? "Untitled" : currentName);

        etInput.setHint(hint);
        etInput.setText(currentName);
        etInput.setSelection(etInput.getText().length());
        etInput.requestFocus();

        btnSave.setOnClickListener(v -> {
            String updatedName = etInput.getText().toString().trim();
            if (updatedName.isEmpty()) {
                etInput.setError("Name is required");
                return;
            }
            if (updatedName.equals(currentName != null ? currentName.trim() : "")) {
                dialog.dismiss();
                return;
            }

            dialog.dismiss();
            if (onConfirm != null) {
                onConfirm.onConfirm(updatedName);
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
