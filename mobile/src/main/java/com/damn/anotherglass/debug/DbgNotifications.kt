package com.damn.anotherglass.debug

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.damn.anotherglass.R
import com.damn.anotherglass.core.GlassService
import com.damn.anotherglass.utility.getService

object DbgNotifications {

    fun postNotification(context: Context) {
        // Service should be running for these calls, it will register the channel
        ++notificationId
        val notification = NotificationCompat
            .Builder(context, GlassService.sCHANNEL_DEFAULT)
            .setContentTitle("Debug notification $notificationId")
            .setContentText("This is a test notification $notificationId")
            .setTicker("Debug notification $notificationId")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager(context).notify(NOTIFICATION_BASE_ID + notificationId, notification)
    }

    fun removeNotification(context: Context): Boolean {
        if (notificationId > 0)
            notificationManager(context).cancel(NOTIFICATION_BASE_ID + notificationId--)
        return notificationId > 0
    }

    var notificationId: Int = 0
        private set

    private fun notificationManager(context: Context) =
        context.getService<NotificationManager>(Context.NOTIFICATION_SERVICE)

    private const val NOTIFICATION_BASE_ID: Int = 10101 + 100
}
