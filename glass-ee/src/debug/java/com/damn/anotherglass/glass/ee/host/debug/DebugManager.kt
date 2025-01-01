package com.damn.anotherglass.glass.ee.host.debug

import android.os.Build
import android.text.format.DateUtils
import com.damn.anotherglass.glass.ee.host.core.NotificationController
import com.damn.anotherglass.shared.notifications.NotificationData

class DebugManager {

    private var notificationsCount = 0

    fun postNotification() {
        NotificationController.instance.onNotificationUpdate(makeDebugNotification(true))
    }

    fun removeNotification() {
        // note: this will only work if notifications are not dismissed by user
        if (notificationsCount != 0) {
            NotificationController.instance.onNotificationUpdate(makeDebugNotification(false))
        }
    }

    private fun makeDebugNotification(add: Boolean) = NotificationData().apply {
        id = if(add) notificationsCount++ else --notificationsCount
        action = if(add) NotificationData.Action.Posted else NotificationData.Action.Removed
        title = "Debug notification #$notificationsCount"
        text = "This is a debug notification #$notificationsCount"
        tickerText = "Debug notification #$notificationsCount"
        postedTime = Build.TIME + notificationsCount * DateUtils.DAY_IN_MILLIS // will be pretty old date
        packageName = "com.damn.anotherglass.glass.ee.host"
    }
}