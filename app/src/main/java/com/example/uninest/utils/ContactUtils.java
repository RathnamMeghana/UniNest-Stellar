package com.example.uninest.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.example.uninest.SessionManager;

public final class ContactUtils {

    public static final String EMERGENCY_CONTACT_DISPLAY = "+353 87 123 4567";
    public static final String SUPPORT_EMAIL = "support@uninest.com";
    private static final String EMERGENCY_CONTACT_URI = "tel:+353871234567";
    private static final String GMAIL_PACKAGE = "com.google.android.gm";

    private ContactUtils() {}

    // Keep support and emergency actions consistent anywhere they appear in the app.
    public static void dialEmergency(Context context) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(EMERGENCY_CONTACT_URI));
        context.startActivity(intent);
    }

    public static void emailSupport(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        String tenantEmail = firstNonBlank(sessionManager.getUserEmail(), "tenant@uninestmail.com");
        String fullName = firstNonBlank(sessionManager.getUserFullName(), "Tenant");
        String houseCode = firstNonBlank(sessionManager.fetchHouseCode(), "Not available");
        String subject = "UniNest support request";
        String body =  "Hi UniNest Support,"
                + "\n\nName: " + fullName
                + "\nHouse code: " + houseCode
                + "\n\nPlease help me with:"
                + "\n";

        Intent gmailComposeIntent = new Intent(Intent.ACTION_SEND);
        gmailComposeIntent.setClassName(GMAIL_PACKAGE, "com.google.android.gm.ComposeActivityGmailExternal");
        gmailComposeIntent.setType("text/plain");
        gmailComposeIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{SUPPORT_EMAIL});
        gmailComposeIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        gmailComposeIntent.putExtra(Intent.EXTRA_TEXT, body);
        if (startIfPossible(context, gmailComposeIntent)) {
            return;
        }

        Intent gmailMailtoIntent = new Intent(Intent.ACTION_SENDTO);
        gmailMailtoIntent.setData(Uri.parse("mailto:"));
        gmailMailtoIntent.setPackage(GMAIL_PACKAGE);
        gmailMailtoIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{SUPPORT_EMAIL});
        gmailMailtoIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        gmailMailtoIntent.putExtra(Intent.EXTRA_TEXT, body);
        if (startIfPossible(context, gmailMailtoIntent)) {
            return;
        }

        Intent gmailSendIntent = new Intent(Intent.ACTION_SEND);
        gmailSendIntent.setType("message/rfc822");
        gmailSendIntent.setPackage(GMAIL_PACKAGE);
        gmailSendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{SUPPORT_EMAIL});
        gmailSendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        gmailSendIntent.putExtra(Intent.EXTRA_TEXT, body);
        if (startIfPossible(context, gmailSendIntent)) {
            return;
        }

        Intent genericEmailIntent = new Intent(Intent.ACTION_SENDTO);
        genericEmailIntent.setData(Uri.parse("mailto:" + Uri.encode(SUPPORT_EMAIL)));
        genericEmailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{SUPPORT_EMAIL});
        genericEmailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        genericEmailIntent.putExtra(Intent.EXTRA_TEXT, body);
        if (startIfPossible(context, genericEmailIntent)) {
            return;
        }

        Intent chooserBaseIntent = new Intent(Intent.ACTION_SEND);
        chooserBaseIntent.setType("text/plain");
        chooserBaseIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{SUPPORT_EMAIL});
        chooserBaseIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        chooserBaseIntent.putExtra(Intent.EXTRA_TEXT, body);
        Intent chooserIntent = Intent.createChooser(chooserBaseIntent, "Contact UniNest Support");
        if (startIfPossible(context, chooserIntent)) {
            return;
        }

        String gmailWebUrl = "https://mail.google.com/mail/?view=cm&fs=1&tf=1"
                + "&to=" + Uri.encode(SUPPORT_EMAIL)
                + "&su=" + Uri.encode(subject)
                + "&body=" + Uri.encode(body);
        Intent gmailWebIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(gmailWebUrl));
        if (startIfPossible(context, gmailWebIntent)) {
            return;
        }

        Toast.makeText(context, "No email app available", Toast.LENGTH_SHORT).show();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static boolean startIfPossible(Context context, Intent intent) {
        if (context == null || intent == null) {
            return false;
        }
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException ignored) {
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }
}
