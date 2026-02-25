package com.example.uninest.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.example.uninest.R;

public class ImageUtils {
    public static void loadProfileImage(ImageView imageView, String imageStr) {
        // 1. Basic Check
        if (imageStr == null || imageStr.trim().isEmpty() || imageStr.length() < 10) {
            imageView.setImageResource(R.drawable.ic_profile_tenant);
            return;
        }

        try {
            if (imageStr.startsWith("http")) {
                // Handle URL
                Glide.with(imageView.getContext())
                        .load(imageStr)
                        .placeholder(R.drawable.ic_profile_tenant)
                        .into(imageView);
            } else {
                // 2. AGGRESSIVE CLEANING (Crucial for your logs)
                // Remove data header if it exists
                String cleanImage = imageStr.replace("data:image/jpeg;base64,", "")
                        .replace("data:image/png;base64,", "");

                // Remove ALL whitespace, including the \n seen in your logs
                cleanImage = cleanImage.replaceAll("\\s+", "");

                // 3. Decode
                byte[] decodedString = Base64.decode(cleanImage, Base64.DEFAULT);

                // 4. Load using Glide for better memory management
                Glide.with(imageView.getContext())
                        .asBitmap()
                        .load(decodedString)
                        .placeholder(R.drawable.ic_profile_tenant)
                        .circleCrop()
                        .error(R.drawable.ic_profile_tenant) // Fallback if decoding results in invalid image
                        .into(imageView);

                Log.d("IMAGE_UTILS", "Image loaded successfully. Byte length: " + decodedString.length);
            }
        } catch (Exception e) {
            Log.e("IMAGE_UTILS", "Error decoding Base64: " + e.getMessage());
            imageView.setImageResource(R.drawable.ic_profile_tenant);
        }
    }
}