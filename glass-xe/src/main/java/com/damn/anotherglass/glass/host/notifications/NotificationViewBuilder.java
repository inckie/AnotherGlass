package com.damn.anotherglass.glass.host.notifications;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.format.DateUtils;

import com.damn.anotherglass.shared.notifications.NotificationData;
import com.google.android.glass.widget.CardBuilder;

import java.text.DateFormat;
import java.util.Date;

public class NotificationViewBuilder {

    public static CardBuilder buildView(Context context, NotificationData data) {
        // basic layout selection
        CardBuilder.Layout layout;
        if (null != data.image && null != data.image.bytes) {
            layout = CardBuilder.Layout.CAPTION;
        } else if (null != data.icon && null != data.icon.bytes) {
            layout = CardBuilder.Layout.COLUMNS;
        } else if (null != data.messages && !data.messages.isEmpty()) {
            layout = CardBuilder.Layout.COLUMNS;
        } else {
            layout = CardBuilder.Layout.TEXT;
        }

        CardBuilder builder = new CardBuilder(context, layout);

        // handle text / conversation
        StringBuilder text = new StringBuilder();
        if (null != data.conversationTitle) {
            text.append(data.conversationTitle).append("\n");
            if (null != data.title && !data.isGroupConversation) {
                // For 1-on-1, title is redundant if conversationTitle is present
            } else if (null != data.title) {
                text.append(data.title).append(": ");
            }
        } else if (null != data.title) {
            text.append(data.title).append("\n");
        }

        if (null != data.messages && !data.messages.isEmpty()) {
            for (NotificationData.Message msg : data.messages) {
                if (text.length() > 0 && text.charAt(text.length() - 1) != '\n') {
                    text.append("\n");
                }
                if (null != msg.sender && (data.isGroupConversation || !msg.sender.equals(data.title))) {
                    text.append(msg.sender).append(": ");
                }
                text.append(msg.text);
            }
        } else if (null != data.text) {
            if (text.length() > 0 && text.charAt(text.length() - 1) != '\n') {
                text.append("\n");
            }
            text.append(data.text);
        }
        builder.setText(text.toString());
        builder.setFootnote(null != data.appName ? data.appName : data.packageName);

        // icon (App Icon)
        if (null != data.icon && null != data.icon.bytes) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data.icon.bytes, 0, data.icon.bytes.length);
            builder.setIcon(bitmap);
            
            // In COLUMNS layout, if we have no images but have an icon, 
            // putting the icon in addImage makes it a nice large side-image
            if (layout == CardBuilder.Layout.COLUMNS && (null == data.messages || data.messages.isEmpty())) {
                builder.addImage(bitmap);
            }
        }

        // image (Background or Mosaic)
        if (null != data.image && null != data.image.bytes) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data.image.bytes, 0, data.image.bytes.length);
            builder.addImage(bitmap);
        } else if (null != data.messages && !data.messages.isEmpty()) {
            // Mosaic of senders
            int added = 0;
            for (NotificationData.Message msg : data.messages) {
                if (null != msg.senderIcon && null != msg.senderIcon.bytes) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(msg.senderIcon.bytes, 0, msg.senderIcon.bytes.length);
                    builder.addImage(bitmap);
                    if (++added >= 5) break; // Glass mosaic limit
                }
            }
        }

        // time
        CharSequence elapsed = DateUtils.formatSameDayTime(
                data.postedTime,
                new Date().getTime(),
                DateFormat.MEDIUM,
                DateFormat.DEFAULT);
        builder.setTimestamp(elapsed);
        return builder;
    }

}
