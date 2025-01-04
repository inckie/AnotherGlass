package com.damn.anotherglass.glass.host.notifications;

import com.damn.anotherglass.shared.notifications.NotificationData;

class NotificationId {
    public String packageName;
    public int id;

    public NotificationId(NotificationData data) {
        packageName = data.packageName;
        id = data.id;
    }

    @Override
    public int hashCode() {
        return packageName.hashCode() * id;
    }

    @Override
    public String toString() {
        return "NotificationId{" +
                "packageName='" + packageName + '\'' +
                ", id=" + id +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NotificationId)) return false;
        NotificationId that = (NotificationId) o;
        return id == that.id &&
                packageName.equals(that.packageName);
    }
}
