package com.damn.anotherglass.extensions.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.notification.StatusBarNotification;

import com.damn.anotherglass.core.GlassService;
import com.damn.anotherglass.shared.RPCMessage;
import com.damn.anotherglass.shared.notifications.NotificationData;
import com.damn.anotherglass.shared.notifications.NotificationsAPI;

public class NotificationExtension extends BroadcastReceiver {

    private final GlassService service;

    @Override
    public void onReceive(Context context, Intent intent) {
        String acton = intent.getStringExtra(Constants.KEY_ACTION);
        StatusBarNotification sbn = intent.getParcelableExtra(Constants.KEY_NOTIFICATION);
        NotificationData notificationData = Converter.convert(service, acton, sbn);
        service.send(new RPCMessage(NotificationsAPI.ID, notificationData));
    }

    public NotificationExtension(final GlassService service) {
        this.service = service;
    }

    public void start() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION);
        service.registerReceiver(this, intentFilter);
    }

    public void stop() {
        service.unregisterReceiver(this);
    }
}
