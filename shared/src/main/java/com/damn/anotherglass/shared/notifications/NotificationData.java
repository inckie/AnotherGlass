package com.damn.anotherglass.shared.notifications;

import androidx.annotation.NonNull;

import com.damn.anotherglass.shared.BinaryData;

import java.io.Serializable;

public class NotificationData implements Serializable {

    public enum Action implements Serializable {
        Posted, Removed
    }

    public enum DeliveryMode implements Serializable {
        Silent,
        Sound // also turns on screen
    }

    @NonNull
    public Action action;
    public int id;
    public String packageName;
    public long postedTime;
    public boolean isOngoing;
    public String title;
    public String text;
    public String tickerText;
    public BinaryData icon;
    public DeliveryMode deliveryMode;
}
