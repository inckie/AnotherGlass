package com.damn.anotherglass.glass.ee.host.core

import com.damn.anotherglass.shared.notifications.NotificationData

class NotificationNotifier(
    private val soundController: SoundController
) {
    fun notify(notification: NotificationData) {
        synchronized(seenNotifications) {
            if (notification.action == NotificationData.Action.Removed) {
                seenNotifications.removeAll { it.id == notification.id && it.packageName == notification.packageName }
                return
            }
            // only track non-silent notifications
            if (notification.deliveryMode == NotificationData.DeliveryMode.Silent) {
                return
            }
            if (!seenNotifications.any { it.id == notification.id && it.packageName == notification.packageName }) {
                seenNotifications.add(SeenNotification(notification.id, notification.packageName))
                soundController.playSound(SoundController.SoundEffect.NotificationPosted)
                // todo: turn on the screen if app is running
            }
        }
    }

    data class SeenNotification(
        val id: Int, // probably its system-wide unique ID, but just in case we will also track package name
        val packageName: String,
    )

    private val seenNotifications = mutableSetOf<SeenNotification>()
}