package com.damn.anotherglass.glass.ee.host.core

import android.content.Context
import android.util.Log
import com.damn.anotherglass.glass.ee.host.utility.wakeScreen
import com.damn.anotherglass.shared.notifications.NotificationData
import com.damn.glass.shared.notifications.NotificationId

class NotificationNotifier(
    private val context: Context,
    private val soundController: SoundController
) {
    private val settings = Settings(context)
    private val seenNotifications = mutableSetOf<NotificationId>()

    fun notify(notification: NotificationData) {
        val id = NotificationId(notification)
        synchronized(seenNotifications) {
            if (notification.action == NotificationData.Action.Removed) {
                seenNotifications.remove(id)
                Log.d(TAG, "Removed seen notification: $id")
                return
            }
            if (notification.deliveryMode == NotificationData.DeliveryMode.Silent) {
                Log.d(TAG, "Ignoring silent notification: $id")
                return
            }
            if (seenNotifications.add(id)) {
                Log.d(TAG, "Playing sound for new notification: $id (ongoing=${notification.isOngoing})")
                soundController.playSound(SoundController.SoundEffect.NotificationPosted)
                if (settings.wakeOnNotification) {
                    Log.d(TAG, "Waking screen on new notification: $id")
                    context.wakeScreen(5000L, "AnotherGlass:NotificationWake")
                }
            } else {
                Log.d(TAG, "Notification already seen: $id")
            }
        }
    }

    companion object {
        private const val TAG = "NotificationNotifier"
    }
}