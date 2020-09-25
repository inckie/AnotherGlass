package com.damn.anotherglass.extensions.notifications;

import android.service.notification.StatusBarNotification;

import com.damn.anotherglass.shared.notifications.NotificationData;

public class NotificationEvent {

    public final StatusBarNotification notification;
    public final NotificationData.Action action;

    public NotificationEvent(StatusBarNotification notification,
                             NotificationData.Action action) {
        this.notification = notification;
        this.action = action;
    }
}
