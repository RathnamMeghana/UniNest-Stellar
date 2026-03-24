package com.example.uninest.utils;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.BitmapShader;
import android.util.LruCache;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.example.uninest.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageUtils {
    private static final LruCache<String, Bitmap> IMAGE_BITMAP_CACHE = createImageBitmapCache();
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newFixedThreadPool(2);

    public static void loadProfileImageImmediate(ImageView imageView, String imageStr) {
        String normalizedImage = normalizeImageSource(imageStr);
        String requestKey = buildRequestKey("profile", normalizedImage);
        if (hasMatchingImageRequest(imageView, requestKey)) {
            return;
        }

        if (normalizedImage == null) {
            showProfilePlaceholder(imageView, requestKey);
            return;
        }

        imageView.setTag(R.id.tag_image_request_key, requestKey);
        try {
            if (normalizedImage.startsWith("http")) {
                imageView.clearColorFilter();
                loadProfileImage(imageView, normalizedImage);
                return;
            }

            Bitmap bitmap = getOrCreateBitmap(requestKey, normalizedImage, true);
            if (bitmap != null && !bitmap.isRecycled()) {
                imageView.clearColorFilter();
                imageView.setImageBitmap(bitmap);
            } else {
                showProfilePlaceholder(imageView, requestKey);
            }
        } catch (Exception e) {
            Log.e("IMAGE_UTILS", "Error loading immediate profile image: " + e.getMessage());
            showProfilePlaceholder(imageView, requestKey);
        }
    }

    public static void loadProfileImage(ImageView imageView, String imageStr) {
        String normalizedImage = normalizeImageSource(imageStr);
        String requestKey = buildRequestKey("profile", normalizedImage);
        if (hasMatchingImageRequest(imageView, requestKey)) {
            return;
        }

        if (normalizedImage == null) {
            showProfilePlaceholder(imageView, requestKey);
            return;
        }

        try {
            imageView.setTag(R.id.tag_image_request_key, requestKey);
            Drawable currentDrawable = imageView.getDrawable();
            Drawable fallbackDrawable = currentDrawable != null
                    ? currentDrawable
                    : buildProfilePlaceholderDrawable(imageView);

            if (normalizedImage.startsWith("http")) {
                imageView.clearColorFilter();
                Glide.with(imageView.getContext())
                        .load(normalizedImage)
                        .placeholder(fallbackDrawable)
                        .error(buildProfilePlaceholderDrawable(imageView))
                        .dontAnimate()
                        .circleCrop()
                        .signature(new ObjectKey(requestKey))
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .into(imageView);
            } else {
                loadCachedBitmap(
                        imageView,
                        normalizedImage,
                        requestKey,
                        fallbackDrawable,
                        R.drawable.ic_profile_tenant,
                        true
                );
            }
        } catch (Exception e) {
            Log.e("IMAGE_UTILS", "Error decoding Base64: " + e.getMessage());
            showProfilePlaceholder(imageView, requestKey);
        }
    }

    public static void loadTicketImage(ImageView imageView, String imageStr) {
        String normalizedImage = normalizeImageSource(imageStr);
        String requestKey = buildRequestKey("ticket", normalizedImage);
        if (hasMatchingImageRequest(imageView, requestKey)) {
            return;
        }

        if (normalizedImage == null) {
            imageView.setTag(R.id.tag_image_request_key, requestKey);
            imageView.setImageDrawable(null);
            return;
        }

        try {
            imageView.setTag(R.id.tag_image_request_key, requestKey);
            Drawable currentDrawable = imageView.getDrawable();

            if (normalizedImage.startsWith("http")) {
                Glide.with(imageView.getContext())
                        .load(normalizedImage)
                        .fitCenter()
                        .placeholder(currentDrawable)
                        .dontAnimate()
                        .signature(new ObjectKey(requestKey))
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(imageView);
            } else {
                loadCachedBitmap(
                        imageView,
                        normalizedImage,
                        requestKey,
                        currentDrawable,
                        android.R.drawable.ic_menu_report_image,
                        false
                );
            }
        } catch (Exception e) {
            Log.e("IMAGE_UTILS", "Error loading ticket image: " + e.getMessage());
            imageView.setImageResource(android.R.drawable.ic_menu_report_image);
        }
    }

    public static void loadBuildingImageImmediate(ImageView imageView, byte[] imageBytes) {
        String requestKey = buildByteArrayRequestKey("building", imageBytes);
        if (hasMatchingImageRequest(imageView, requestKey)) {
            return;
        }

        imageView.setTag(R.id.tag_image_request_key, requestKey);
        if (imageBytes == null || imageBytes.length == 0) {
            imageView.setImageResource(R.drawable.building_placeholder);
            return;
        }

        try {
            Bitmap bitmap = getOrCreateBitmap(requestKey, imageBytes, false);
            if (bitmap != null && !bitmap.isRecycled()) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.building_placeholder);
            }
        } catch (Exception e) {
            Log.e("IMAGE_UTILS", "Error loading immediate building image: " + e.getMessage());
            imageView.setImageResource(R.drawable.building_placeholder);
        }
    }

    public static void loadBuildingImage(ImageView imageView, byte[] imageBytes) {
        String requestKey = buildByteArrayRequestKey("building", imageBytes);
        if (hasMatchingImageRequest(imageView, requestKey)) {
            return;
        }

        if (imageBytes == null || imageBytes.length == 0) {
            imageView.setTag(R.id.tag_image_request_key, requestKey);
            imageView.setImageResource(R.drawable.building_placeholder);
            return;
        }

        imageView.setTag(R.id.tag_image_request_key, requestKey);
        Bitmap cachedBitmap = IMAGE_BITMAP_CACHE.get(requestKey);
        if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
            imageView.setImageBitmap(cachedBitmap);
            return;
        }

        Drawable fallbackDrawable = imageView.getDrawable() != null
                ? imageView.getDrawable()
                : ContextCompat.getDrawable(imageView.getContext(), R.drawable.building_placeholder);
        if (fallbackDrawable != null) {
            imageView.setImageDrawable(fallbackDrawable);
        }

        IMAGE_EXECUTOR.execute(() -> {
            try {
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                if (decodedBitmap == null) {
                    postErrorDrawable(imageView, requestKey, R.drawable.building_placeholder);
                    return;
                }

                IMAGE_BITMAP_CACHE.put(requestKey, decodedBitmap);
                imageView.post(() -> {
                    if (hasMatchingPendingRequest(imageView, requestKey)) {
                        imageView.setImageBitmap(decodedBitmap);
                    }
                });
            } catch (Exception e) {
                Log.e("IMAGE_UTILS", "Error loading building image: " + e.getMessage());
                postErrorDrawable(imageView, requestKey, R.drawable.building_placeholder);
            }
        });
    }

    public static String normalizeImageSource(String imageStr) {
        if (imageStr == null) {
            return null;
        }

        String trimmed = imageStr.trim();
        if (trimmed.isEmpty() || trimmed.length() < 10) {
            return null;
        }

        if (trimmed.startsWith("http")) {
            return trimmed;
        }

        return trimmed.replace("data:image/jpeg;base64,", "")
                .replace("data:image/png;base64,", "")
                .replace("data:image/jpg;base64,", "")
                .replaceAll("\\s+", "");
    }

    private static byte[] decodeBase64Image(String normalizedImage) {
        return Base64.decode(normalizedImage, Base64.DEFAULT);
    }

    private static boolean hasMatchingImageRequest(ImageView imageView, String requestKey) {
        if (requestKey == null) {
            return false;
        }

        Object existingKey = imageView.getTag(R.id.tag_image_request_key);
        return requestKey.equals(existingKey) && imageView.getDrawable() != null;
    }

    private static boolean hasMatchingPendingRequest(ImageView imageView, String requestKey) {
        Object existingKey = imageView.getTag(R.id.tag_image_request_key);
        return requestKey.equals(existingKey);
    }

    private static String buildRequestKey(String prefix, String normalizedImage) {
        if (normalizedImage == null) {
            return prefix + ":default";
        }
        return prefix + ":" + normalizedImage.length() + ":" + normalizedImage.hashCode();
    }

    private static String buildByteArrayRequestKey(String prefix, byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return prefix + ":default";
        }
        return prefix + ":" + imageBytes.length + ":" + java.util.Arrays.hashCode(imageBytes);
    }

    private static void loadCachedBitmap(ImageView imageView,
                                         String normalizedImage,
                                         String requestKey,
                                         Drawable fallbackDrawable,
                                         int errorResId,
                                         boolean circleCrop) {
        Bitmap cachedBitmap = IMAGE_BITMAP_CACHE.get(requestKey);
        if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
            imageView.setImageBitmap(cachedBitmap);
            return;
        }

        if (fallbackDrawable != null) {
            imageView.setImageDrawable(fallbackDrawable);
        }

        IMAGE_EXECUTOR.execute(() -> {
            try {
                Bitmap finalBitmap = getOrCreateBitmap(requestKey, normalizedImage, circleCrop);
                if (finalBitmap == null) {
                    postErrorDrawable(imageView, requestKey, errorResId);
                    return;
                }

                Log.d("IMAGE_UTILS", "Bitmap cached for key: " + requestKey);

                imageView.post(() -> {
                    if (hasMatchingPendingRequest(imageView, requestKey)) {
                        imageView.setImageBitmap(finalBitmap);
                    }
                });
            } catch (Exception e) {
                Log.e("IMAGE_UTILS", "Error preparing cached bitmap: " + e.getMessage());
                postErrorDrawable(imageView, requestKey, errorResId);
            }
        });
    }

    private static void postErrorDrawable(ImageView imageView, String requestKey, int errorResId) {
        imageView.post(() -> {
            if (hasMatchingPendingRequest(imageView, requestKey)) {
                if (errorResId == R.drawable.ic_profile_tenant) {
                    showProfilePlaceholder(imageView, requestKey);
                } else {
                    imageView.setImageResource(errorResId);
                }
            }
        });
    }

    private static void showProfilePlaceholder(ImageView imageView, String requestKey) {
        imageView.setTag(R.id.tag_image_request_key, requestKey);
        Drawable placeholder = buildProfilePlaceholderDrawable(imageView);
        if (placeholder != null) {
            imageView.setImageDrawable(placeholder);
            return;
        }

        imageView.setImageResource(R.drawable.ic_profile_tenant);
        if (isDarkMode(imageView)) {
            imageView.setColorFilter(ContextCompat.getColor(imageView.getContext(), R.color.white));
        } else {
            imageView.clearColorFilter();
        }
    }

    private static Drawable buildProfilePlaceholderDrawable(ImageView imageView) {
        Drawable drawable = ContextCompat.getDrawable(imageView.getContext(), R.drawable.ic_profile_tenant);
        if (drawable == null) {
            return null;
        }

        Drawable wrapped = DrawableCompat.wrap(drawable.mutate());
        if (isDarkMode(imageView)) {
            DrawableCompat.setTint(wrapped, ContextCompat.getColor(imageView.getContext(), R.color.white));
        } else {
            wrapped.clearColorFilter();
        }
        return wrapped;
    }

    private static boolean isDarkMode(ImageView imageView) {
        int nightModeFlags = imageView.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    private static Bitmap getOrCreateBitmap(String requestKey, String normalizedImage, boolean circleCrop) {
        Bitmap cachedBitmap = IMAGE_BITMAP_CACHE.get(requestKey);
        if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
            return cachedBitmap;
        }

        byte[] decodedBytes = decodeBase64Image(normalizedImage);
        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        if (decodedBitmap == null) {
            return null;
        }

        Bitmap finalBitmap = circleCrop ? createCircleBitmap(decodedBitmap) : decodedBitmap;
        IMAGE_BITMAP_CACHE.put(requestKey, finalBitmap);
        return finalBitmap;
    }

    private static Bitmap getOrCreateBitmap(String requestKey, byte[] imageBytes, boolean circleCrop) {
        Bitmap cachedBitmap = IMAGE_BITMAP_CACHE.get(requestKey);
        if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
            return cachedBitmap;
        }

        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        if (decodedBitmap == null) {
            return null;
        }

        Bitmap finalBitmap = circleCrop ? createCircleBitmap(decodedBitmap) : decodedBitmap;
        IMAGE_BITMAP_CACHE.put(requestKey, finalBitmap);
        return finalBitmap;
    }

    private static Bitmap createCircleBitmap(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = Math.max(0, (source.getWidth() - size) / 2);
        int y = Math.max(0, (source.getHeight() - size) / 2);

        Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        BitmapShader shader = new BitmapShader(squaredBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);
        return output;
    }

    private static LruCache<String, Bitmap> createImageBitmapCache() {
        final int maxSizeKb = (int) (Runtime.getRuntime().maxMemory() / 1024L / 16L);
        return new LruCache<String, Bitmap>(Math.max(maxSizeKb, 2048)) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value == null ? 0 : Math.max(1, value.getByteCount() / 1024);
            }
        };
    }
}
