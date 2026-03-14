package com.example.uninest.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public final class ContactUtils {

    public static final String EMERGENCY_CONTACT_DISPLAY = "+353 87 123 4567";
    public static final String SUPPORT_EMAIL = "support@uninest.com";
    private static final String EMERGENCY_CONTACT_URI = "tel:+353871234567";

    private ContactUtils() {}

    // Keep support and emergency actions consistent anywhere they appear in the app.
    public static void dialEmergency(Context context) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(EMERGENCY_CONTACT_URI));
        context.startActivity(intent);
    }

    public static void emailSupport(Context context) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + SUPPORT_EMAIL));
        intent.putExtra(Intent.EXTRA_SUBJECT, "UniNest support request");

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "No email app available", Toast.LENGTH_SHORT).show();
        }
    }
}
