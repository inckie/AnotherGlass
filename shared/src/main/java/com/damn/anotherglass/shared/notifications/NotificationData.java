package com.damn.anotherglass.shared.notifications;

import java.io.Serializable;

public class NotificationData implements Serializable {

    public enum Action implements Serializable {
        Posted, Removed
    }

    public Action action;
    public int id;
    public String packageName;
    public long postedTime;
    public boolean isOngoing;
    public String title;
    public String text;
    public String tickerText;
    public byte[] icon;
}
