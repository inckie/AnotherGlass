package com.damn.anotherglass.extensions.notifications;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

// todo: filter self notifications
// todo: add whitelist

public class NotificationService extends NotificationListenerService {

    public NotificationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Intent intent = new Intent(Constants.ACTION);
        intent.putExtra(Constants.KEY_ACTION, Constants.ACTION_NOTIFICATION_POSTED);
        intent.putExtra(Constants.KEY_NOTIFICATION, sbn);
        sendBroadcast(intent);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Intent intent = new Intent(Constants.ACTION);
        intent.putExtra(Constants.KEY_ACTION, Constants.ACTION_NOTIFICATION_REMOVED);
        intent.putExtra(Constants.KEY_NOTIFICATION, sbn);
        sendBroadcast(intent);
    }

    // https://stackoverflow.com/a/51724784
    public static boolean isEnabled(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        if(TextUtils.isEmpty(enabledNotificationListeners))
            return false;
        String packageName = context.getPackageName();
        return enabledNotificationListeners.contains(packageName);
    }

}
