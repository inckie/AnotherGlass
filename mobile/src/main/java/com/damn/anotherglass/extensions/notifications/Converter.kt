package com.damn.anotherglass.extensions.notifications

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.service.notification.StatusBarNotification
import com.applicaster.xray.core.Logger
import com.damn.anotherglass.logging.ALog
import com.damn.anotherglass.shared.notifications.NotificationData
import java.io.ByteArrayOutputStream
import java.io.IOException
import androidx.core.graphics.createBitmap

object Converter {
    private const val TAG = "IconConverter"
    private val log = ALog(Logger.get(TAG))
    fun convert(
        context: Context,
        acton: NotificationData.Action,
        sbn: StatusBarNotification
    ): NotificationData {
        val data = NotificationData()
        data.action = acton

        // parse basic data
        data.id = sbn.id
        data.packageName = sbn.packageName
        data.postedTime = sbn.postTime
        data.isOngoing = sbn.isOngoing

        // todo: code below this point is not really needed for NotificationData.Action.Removed

        // todo: extract app name

        // parse Notification data
        val notification = sbn.notification
        data.title = notification.extras.getString(Notification.EXTRA_TITLE)
        data.text = notification.extras.getString(Notification.EXTRA_TEXT)

        // todo: https://stackoverflow.com/questions/29363770/how-to-get-text-of-stacked-notifications-in-android/29364414
        if (null != notification.tickerText) {
            data.tickerText = notification.tickerText.toString()
        }
        try {
            extractIcon(context, data, notification)
        } catch (e: Exception) {
            // todo: new Android version do not allow that, add required permission
            log.e(TAG, "Failed to extract icon from notification: " + e.message, e)
        }
        return data
    }

    private fun extractIcon(
        context: Context,
        data: NotificationData,
        notification: Notification
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var icon = notification.getLargeIcon()
            if (null == icon) icon = notification.smallIcon
            if (null != icon) {
                val drawable = icon.loadDrawable(context)
                if (null != drawable) {
                    val bitmap = drawableToBitmap(drawable)
                    setIconData(data, bitmap)
                }
            }
        }
        if (null != data.icon) return
        if (null != notification.largeIcon) setIconData(data, notification.largeIcon)
        if (null != data.icon) return

        // todo: retrieve default icon from the package
    }

    private fun setIconData(data: NotificationData, bitmap: Bitmap) {
        try {
            ByteArrayOutputStream().use { memoryStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, memoryStream)
                data.icon = memoryStream.toByteArray()
            }
        } catch (e: IOException) {
            log.e(TAG, "Failed to compress icon bitmap: " + e.message, e)
        }
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            if (null != drawable.bitmap) {
                return drawable.bitmap
            }
        }
        val bitmap: Bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            // Single color bitmap will be created of 1x1 pixel
            createBitmap(1, 1)
        } else {
            createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
