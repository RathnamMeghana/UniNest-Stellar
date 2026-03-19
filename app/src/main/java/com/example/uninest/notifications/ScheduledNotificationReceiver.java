package com.example.uninest.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ScheduledNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        LocalNotificationHelper.showNotification(
                context,
                intent.getStringExtra(LocalNotificationHelper.EXTRA_TITLE),
                intent.getStringExtra(LocalNotificationHelper.EXTRA_BODY),
                intent.getStringExtra(LocalNotificationHelper.EXTRA_TARGET_SCREEN),
                intent.getStringExtra(LocalNotificationHelper.EXTRA_ENTITY_ID),
                intent.getStringExtra(LocalNotificationHelper.EXTRA_DEDUPE_KEY)
        );
    }
}
