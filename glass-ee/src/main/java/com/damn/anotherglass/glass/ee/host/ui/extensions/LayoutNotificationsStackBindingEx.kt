package com.damn.anotherglass.glass.ee.host.ui.extensions

import android.content.Context
import android.graphics.BitmapFactory
import android.text.format.DateUtils
import android.util.Log
import com.damn.anotherglass.glass.ee.host.databinding.LayoutNotificationsStackBinding
import com.damn.anotherglass.shared.notifications.NotificationData

fun LayoutNotificationsStackBinding.bindData(notification: NotificationData) {
    title.text = notification.title
    text.text = notification.text
    footer.text = notification.tickerText
    timestamp.text = DateUtils.formatDateTime(
        root.context,
        notification.postedTime,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL
    )
    val bitmap = notification.icon?.bytes?.let { bytes ->
        runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
            .onFailure { Log.e("NotificationBinding", "Failed to decode icon", it) }
            .getOrNull()
    }
    if (bitmap != null) {
        imgIcon.setImageBitmap(bitmap)
    } else {
        imgIcon.setImageResource(android.R.drawable.ic_dialog_info)
    }
}

// Overload for backward compatibility
fun LayoutNotificationsStackBinding.bindData(notification: NotificationData, @Suppress("UNUSED_PARAMETER") context: Context) =
    bindData(notification)