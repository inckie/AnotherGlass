package com.damn.anotherglass.extensions.notifications;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import com.damn.anotherglass.core.GlassService;
import com.damn.anotherglass.core.Settings;
import com.damn.anotherglass.shared.notifications.NotificationData;

import org.greenrobot.eventbus.EventBus;

// todo: filter self notifications
// todo: add whitelist

public class NotificationService extends NotificationListenerService {

    private Settings mSettings;

    public NotificationService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSettings = new Settings(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        emit(sbn, NotificationData.Action.Posted);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        emit(sbn, NotificationData.Action.Removed);
    }

    private void emit(StatusBarNotification sbn, NotificationData.Action posted) {
        if (mSettings.isNotificationsEnabled() &&
                GlassService.isRunning(this)) {
            // do not convert notification yet, send as is
            EventBus.getDefault().post(new NotificationEvent(sbn, posted));
        }
    }

    // https://stackoverflow.com/a/51724784
    public static boolean isEnabled(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        String enabledNotificationListeners = android.provider.Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        if(TextUtils.isEmpty(enabledNotificationListeners))
            return false;
        String packageName = context.getPackageName();
        return enabledNotificationListeners.contains(packageName);
    }

}
