package com.example.uninest.ui.auth;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.uninest.R;

public class ApartmentCardView extends FrameLayout {

    private TextView tvApartmentName;
    private TextView tvTenantInfo;

    private OnClickListener deleteListener;
    private OnClickListener notifyListener;

    public ApartmentCardView(Context context) {
        super(context);
        init(context);
    }

    public ApartmentCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ApartmentCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_apartment_card, this, true);

        tvApartmentName = findViewById(R.id.tvApartmentName);
        tvTenantInfo = findViewById(R.id.tvTenantInfo);

        findViewById(R.id.btnDeleteApartment).setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onClick(v);
        });

        findViewById(R.id.btnNotifyApartment).setOnClickListener(v -> {
            if (notifyListener != null) notifyListener.onClick(v);
        });
    }

    public void setOnDeleteClickListener(OnClickListener listener) {
        this.deleteListener = listener;
    }

    public void setOnNotifyClickListener(OnClickListener listener) {
        this.notifyListener = listener;
    }

    public void setApartmentName(String name) {
        tvApartmentName.setText(name);
    }

    public void setTenantInfo(String occupied, String capacity) {
        tvTenantInfo.setText(occupied + "/" + capacity + " Tenants");
    }

    public void setTenantInfoText(String text) {
        tvTenantInfo.setText(text);
    }
}