package com.example.uninest.ui.auth;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.example.uninest.R;

public class BuildingCardView extends FrameLayout {

    private TextView tvBuildingName, tvApartmentInfo;
    private ImageView imgBuilding;

    public BuildingCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BuildingCardView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_building_card, this, true);
        tvBuildingName = findViewById(R.id.tvBuildingName);
        tvApartmentInfo = findViewById(R.id.tvApartmentInfo);
        imgBuilding = findViewById(R.id.imgBuilding);
    }

    public void setBuildingName(String name) { tvBuildingName.setText(name); }
    public void setApartmentCount(int count) { tvApartmentInfo.setText(count + " Apartments"); }

    // ADDED THIS METHOD BACK TO FIX YOUR ERROR
    public void setBuildingImage(int resId) {
        Glide.with(getContext()).clear(imgBuilding);
        imgBuilding.setImageResource(resId);
    }

    public void setBuildingImageFromBase64(String base64String) {
        // Clear state to fix the "Wrong Image" recycling bug
        Glide.with(getContext()).clear(imgBuilding);
        imgBuilding.setImageResource(R.drawable.ic_launcher_background);

        if (base64String == null || base64String.isEmpty()) return;

        try {
            byte[] imageBytes = Base64.decode(base64String, Base64.DEFAULT);
            Glide.with(getContext())
                    .asBitmap()
                    .load(imageBytes)
                    .centerCrop()
                    .into(imgBuilding);
        } catch (Exception e) {
            imgBuilding.setImageResource(R.drawable.ic_launcher_background);
        }
    }
}