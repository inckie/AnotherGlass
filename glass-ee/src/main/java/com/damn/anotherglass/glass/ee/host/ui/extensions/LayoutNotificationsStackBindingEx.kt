package com.damn.anotherglass.glass.ee.host.ui.extensions

import android.content.Context
import android.graphics.BitmapFactory
import android.text.format.DateUtils
import android.util.Log
import com.damn.anotherglass.glass.ee.host.databinding.LayoutNotificationsStackBinding
import com.damn.anotherglass.shared.notifications.NotificationData

object LayoutNotificationsStackBindingEx {

    private const val TAG = "LayoutNotificationsStackBindingEx"

    fun LayoutNotificationsStackBinding.bindData(
        last: NotificationData,
        context: Context
    ) {
        title.text = last.title
        text.text = last.text
        footer.text = last.tickerText
        timestamp.text = DateUtils.formatDateTime(
            context,
            last.postedTime,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL
        )
        if (last.icon == null) {
            imgIcon.setImageResource(android.R.drawable.ic_dialog_info)
        } else {
            try {
                val bitmap = BitmapFactory.decodeByteArray(last.icon, 0, last.icon.size)
                imgIcon.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode icon: " + e.message, e)
                imgIcon.setImageResource(android.R.drawable.ic_dialog_info)
            }
        }
    }
}