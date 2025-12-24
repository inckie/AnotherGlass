package com.damn.glass.shared.notifications;

import androidx.annotation.NonNull;

import com.damn.anotherglass.shared.notifications.NotificationData;

public class NotificationId {
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

    @NonNull
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
        //noinspection PatternVariableCanBeUsed Java 1.8 target
        NotificationId that = (NotificationId) o;
        return id == that.id &&
                packageName.equals(that.packageName);
    }
}
