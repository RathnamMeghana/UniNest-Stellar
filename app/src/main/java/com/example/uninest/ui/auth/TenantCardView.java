package com.example.uninest.ui.auth;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.uninest.R;

/**
 * Reusable card component for a tenant row.
 */
public class TenantCardView extends FrameLayout {

    private TextView tvTenantName;
    private TextView tvRoomLabel;
    private ImageView imgTenantAvatar;

    public TenantCardView(Context context) {
        super(context);
        init(context);
    }

    public TenantCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TenantCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_tenant_card, this, true);

        tvTenantName = findViewById(R.id.tvTenantName);
        tvRoomLabel = findViewById(R.id.tvRoomLabel);
        imgTenantAvatar = findViewById(R.id.imgTenantAvatar);
    }

    public void setTenantName(String name) {
        tvTenantName.setText(name);
    }

    public void setRoomLabel(String label) {
        tvRoomLabel.setText(label);
    }

    public ImageView getAvatarImageView() {
        return imgTenantAvatar; // for loading real profile pics later
    }
}
