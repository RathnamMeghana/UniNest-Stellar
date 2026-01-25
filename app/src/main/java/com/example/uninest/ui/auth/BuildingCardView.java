package com.example.uninest.ui.auth;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.uninest.R;

/**
 * Reusable card component for a building row.
 * Now supports Base64 image decoding for Firestore-stored images.
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

    // ---- Setters from Activities / Adapters ---- //

    public void setBuildingName(String name) {
        tvBuildingName.setText(name);
    }

    public void setApartmentCount(int count) {
        tvApartmentInfo.setText(count + " Apartments");
    }

    public void setApartmentInfoText(String text) {
        tvApartmentInfo.setText(text);
    }

    /**
     * Use this for local Android resource icons.
     */
    public void setBuildingImage(@DrawableRes int resId) {
        imgBuilding.setImageResource(resId);
    }

    /**
     * Use this to display the image stored as a Base64 string in Firestore.
     */
    public void setBuildingImageFromBase64(String base64String) {
        if (base64String == null || base64String.isEmpty()) {
            // Use a default image if no data exists
            imgBuilding.setImageResource(R.drawable.ic_launcher_background);
            return;
        }

        try {
            // Clean the string if it contains headers (like "data:image/jpeg;base64,")
            if (base64String.contains(",")) {
                base64String = base64String.split(",")[1];
            }

            // Convert Base64 string to byte array
            byte[] imageBytes = Base64.decode(base64String, Base64.DEFAULT);

            // Load into ImageView using Glide
            Glide.with(getContext())
                    .asBitmap()
                    .load(imageBytes)
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache for performance
                    .placeholder(R.drawable.ic_launcher_background) // Show while loading
                    .error(R.drawable.ic_launcher_background)       // Show if decoding fails
                    .into(imgBuilding);

        } catch (Exception e) {
            Log.e("BuildingCardView", "Error decoding Base64 image", e);
            imgBuilding.setImageResource(R.drawable.ic_launcher_background);
        }
    }

    public ImageView getImageView() {
        return imgBuilding;
    }
}