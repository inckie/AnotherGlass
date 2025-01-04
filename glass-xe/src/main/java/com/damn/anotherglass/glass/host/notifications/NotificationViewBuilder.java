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
        // basic
        CardBuilder builder = new CardBuilder(context, CardBuilder.Layout.AUTHOR)
                .setHeading(data.title)
                .setSubheading(data.packageName) // todo: should be application name
                .setText(data.text);

        // icon
        if (null != data.icon) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data.icon, 0, data.icon.length);
            builder.setIcon(bitmap);
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
