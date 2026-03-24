package com.example.uninest.utils;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.uninest.R;
import com.google.android.material.button.MaterialButton;

import java.lang.ref.WeakReference;

public final class NetworkErrorDialog {

    private static WeakReference<View> activeOverlayRef = new WeakReference<>(null);
    private static String activeMessageKey;

    private NetworkErrorDialog() {
    }

    public static void show(Context context, Runnable retryAction) {
        show(context, "Something went wrong", "Check your connection and try again.", retryAction);
    }

    public static void show(Context context, String title, String body, Runnable retryAction) {
        if (context == null) {
            return;
        }

        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        } else {
            return;
        }

        Activity activity = (Activity) context;
        String message = title + "\n" + body;
        String messageKey = activity.getClass().getName() + "|" + message;
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) {
            return;
        }

        View activeOverlay = activeOverlayRef.get();
        if (activeOverlay != null && activeOverlay.getParent() != null && messageKey.equals(activeMessageKey)) {
            return;
        }

        if (activeOverlay != null) {
            removeOverlay(activeOverlay);
        }

        View overlayView = LayoutInflater.from(context).inflate(R.layout.dialog_network_error, rootView, false);
        activeOverlayRef = new WeakReference<>(overlayView);
        activeMessageKey = messageKey;

        TextView tvTitle = overlayView.findViewById(R.id.tvDialogNetworkErrorTitle);
        TextView tvBody = overlayView.findViewById(R.id.tvDialogNetworkErrorBody);
        MaterialButton btnRetry = overlayView.findViewById(R.id.btnDialogRetryNetwork);

        tvTitle.setText(title);
        tvBody.setText(body);
        btnRetry.setOnClickListener(v -> {
            dismiss(context);
            if (retryAction != null) {
                retryAction.run();
            }
        });

        rootView.addView(overlayView);
    }

    public static void dismiss(Context context) {
        View overlayView = activeOverlayRef.get();
        if (overlayView != null) {
            Context overlayContext = overlayView.getContext();
            if (context == null || overlayContext == context) {
                removeOverlay(overlayView);
            }
        }
    }

    private static void removeOverlay(View overlayView) {
        if (overlayView == null) {
            return;
        }

        ViewGroup parent = overlayView.getParent() instanceof ViewGroup
                ? (ViewGroup) overlayView.getParent()
                : null;
        if (parent != null) {
            parent.removeView(overlayView);
        }

        View currentOverlay = activeOverlayRef.get();
        if (currentOverlay == overlayView) {
            activeOverlayRef = new WeakReference<>(null);
            activeMessageKey = null;
        }
    }
}
