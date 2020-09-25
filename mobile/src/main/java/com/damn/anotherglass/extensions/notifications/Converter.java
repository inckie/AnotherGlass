package com.damn.anotherglass.extensions.notifications;

import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import androidx.annotation.NonNull;

import com.damn.anotherglass.shared.notifications.NotificationData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Converter {

    @NonNull
    public static NotificationData convert(@NonNull Context context,
                                           @NonNull NotificationData.Action acton,
                                           @NonNull StatusBarNotification sbn) {

        NotificationData data = new NotificationData();
        data.action = acton;

        // parse basic data
        data.id = sbn.getId();
        data.packageName = sbn.getPackageName();
        data.postedTime = sbn.getPostTime();
        data.isOngoing = sbn.isOngoing();

        // todo: code below this point is not really needed for NotificationData.Action.Removed

        // todo: extract app name

        // parse Notification data
        Notification notification = sbn.getNotification();
        data.title = notification.extras.getString(Notification.EXTRA_TITLE);
        data.text = notification.extras.getString(Notification.EXTRA_TEXT);

        // todo: https://stackoverflow.com/questions/29363770/how-to-get-text-of-stacked-notifications-in-android/29364414

        if(null != notification.tickerText) {
            data.tickerText = notification.tickerText.toString();
        }

        extractIcon(context, data, notification);
        return data;
    }

    private static void extractIcon(@NonNull Context context,
                                    @NonNull NotificationData data,
                                    @NonNull Notification notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Icon icon = notification.getLargeIcon();
            if (null == icon)
                icon = notification.getSmallIcon();

            if (null != icon) {
                Drawable drawable = icon.loadDrawable(context);
                if(null != drawable) {
                    Bitmap bitmap = drawableToBitmap(drawable);
                    setIconData(data, bitmap);
                }
            }
        }

        if (null != data.icon)
            return;

        if (null != notification.largeIcon)
            setIconData(data, notification.largeIcon);

        if (null != data.icon)
            return;

        // todo: retrieve default icon from the package
    }

    private static void setIconData(@NonNull NotificationData data, @NonNull Bitmap bitmap) {
        try (ByteArrayOutputStream memoryStream = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, memoryStream);
            data.icon = memoryStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    public static Bitmap drawableToBitmap(@NonNull Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (null != bitmapDrawable.getBitmap()) {
                return bitmapDrawable.getBitmap();
            }
        }

        Bitmap bitmap;
        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            // Single color bitmap will be created of 1x1 pixel
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(
                    drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
