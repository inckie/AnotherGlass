package com.damn.anotherglass.glass.ee.host.core

import android.util.Log
import com.damn.anotherglass.shared.notifications.NotificationData
import com.damn.glass.shared.notifications.NotificationId

class NotificationNotifier(
    private val soundController: SoundController
) {
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
                // todo: turn on the screen if app is running
            } else {
                Log.d(TAG, "Notification already seen: $id")
            }
        }
    }

    companion object {
        private const val TAG = "NotificationNotifier"
    }
}