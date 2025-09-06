package com.damn.glass.shared.notifications

import com.damn.anotherglass.shared.notifications.NotificationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NotificationController {

    private val notifications = MutableStateFlow(listOf<NotificationData>())

    fun getNotifications(): StateFlow<List<NotificationData>> = notifications

    fun onNotificationUpdate(notification: NotificationData) {
        // We expect that modification will always happen, so make mutable copy in any case
        val current = notifications.value.toMutableList()
        when (notification.action) {
            NotificationData.Action.Posted -> {
                when (val index = current.indexOfFirst { it.id == notification.id && it.packageName == notification.packageName }) {
                    -1 -> current += notification
                    else -> current[index] = notification
                }
                notifications.value = current
            }

            NotificationData.Action.Removed -> {
                if (current.removeAll { it.id == notification.id && it.packageName == notification.packageName }) {
                    notifications.value = current
                }
            }
        }
    }

    fun onServiceConnected() {
        // Clear all notifications on service reconnection to avoid stale ones
        notifications.value = emptyList()
    }

    fun dismissNotification(notificationId: NotificationId) {
        val current = notifications.value.toMutableList()
        if (current.removeAll { it.id == notificationId.id && it.packageName == notificationId.packageName }) {
            notifications.value = current
        }
    }

    companion object {
        private val TAG = "NotificationController"
        @JvmStatic
        val instance: NotificationController by lazy { NotificationController() }
    }
}