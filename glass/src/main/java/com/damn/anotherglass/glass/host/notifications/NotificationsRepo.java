package com.damn.anotherglass.glass.host.notifications;

import com.damn.anotherglass.shared.notifications.NotificationData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsRepo {

    private static NotificationsRepo instance;

    private final Map<NotificationId, NotificationData> notifications = new HashMap<>();

    public static NotificationsRepo get() {
        if (null == instance) {
            synchronized (NotificationsRepo.class) {
                if (instance == null)
                    instance = new NotificationsRepo();
            }
        }
        return instance;
    }

    public synchronized void update(NotificationData data) {
        NotificationId notificationId = new NotificationId(data);
        if (data.action == NotificationData.Action.Removed) {
            notifications.remove(notificationId);
        } else {
            notifications.put(notificationId, data);
        }
    }

    public synchronized List<NotificationData> getActiveNotifications() {
        return new ArrayList<>(notifications.values());
    }

    public synchronized void clear() {
        notifications.clear();
    }
}