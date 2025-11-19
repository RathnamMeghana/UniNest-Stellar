package com.example.uninest.ui.auth;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.cardview.widget.CardView;

import com.example.uninest.R;

/**
 * Reusable card component for a building row.
 */
public class BuildingCardView extends FrameLayout {

    private TextView tvBuildingName;
    private TextView tvApartmentInfo;
    private ImageView imgBuilding;

    public BuildingCardView(Context context) {
        super(context);
        init(context);
    }

    public BuildingCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BuildingCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_building_card, this, true);

        tvBuildingName = findViewById(R.id.tvBuildingName);
        tvApartmentInfo = findViewById(R.id.tvApartmentInfo);
        imgBuilding = findViewById(R.id.imgBuilding);

    }

    // ---- setters from Activities ---- //

    public void setBuildingName(String name) {
        tvBuildingName.setText(name);
    }

    public void setApartmentCount(int count) {
        tvApartmentInfo.setText(count + " Apartments");
    }

    public void setApartmentInfoText(String text) {
        tvApartmentInfo.setText(text);
    }

    public void setBuildingImage(@DrawableRes int resId) {
        imgBuilding.setImageResource(resId);
    }

    public ImageView getImageView() {
        return imgBuilding; // for Glide/Coil later
    }
}
