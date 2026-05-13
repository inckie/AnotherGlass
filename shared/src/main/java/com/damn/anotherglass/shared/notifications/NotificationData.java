package com.damn.anotherglass.shared.notifications;

import androidx.annotation.NonNull;

import com.damn.anotherglass.shared.BinaryData;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NotificationData implements Serializable {

    public enum Action implements Serializable {
        Posted, Removed
    }

    public enum DeliveryMode implements Serializable {
        Silent,
        Sound // also turns on screen
    }

    public static class Message implements Serializable {
        public String sender;
        public String text;
        public long time;
        public BinaryData senderIcon;
    }

    @NonNull
    public Action action;
    public int id;
    public String packageName;
    public String appName;
    public long postedTime;
    public boolean isOngoing;
    public String title;
    public String text;
    public String tickerText;
    public BinaryData icon;
    public BinaryData image;
    public DeliveryMode deliveryMode;

    // MessagingStyle / Conversation data
    public String conversationTitle;
    public boolean isGroupConversation;
    public List<Message> messages = new ArrayList<>();
}
