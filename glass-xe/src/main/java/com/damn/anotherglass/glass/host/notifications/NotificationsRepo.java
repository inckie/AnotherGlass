package com.damn.anotherglass.glass.host.notifications;

import com.damn.anotherglass.shared.notifications.NotificationData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsRepo {

    private final Map<NotificationId, NotificationData> notifications = new HashMap<>();

    private static final class InstanceHolder {
        static final NotificationsRepo instance = new NotificationsRepo();
    }

    public static NotificationsRepo get() {
        return InstanceHolder.instance;
    }

    // synchronization is not really needed, all updates are in UI thread now

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

    public List<NotificationData> getDismissibleNotifications() {
        // no java 8 streams/kotlin
        List<NotificationData> res = new ArrayList<>();
        List<NotificationData> activeNotifications = getActiveNotifications();
        for (NotificationData n : activeNotifications) {
            if(!n.isOngoing)
                res.add(n);
        }
        return res;
    }

    public boolean hasDismissibleNotifications() {
        // no java 8 streams/kotlin
        List<NotificationData> activeNotifications = getActiveNotifications();
        for (NotificationData n : activeNotifications) {
            if(!n.isOngoing)
                return true;
        }
        return false;
    }

    public synchronized void clear() {
        notifications.clear();
    }
}
