package com.damn.glass.shared.notifications

import android.util.Log
import com.damn.anotherglass.shared.notifications.NotificationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NotificationController {

    private val _notifications = MutableStateFlow<List<NotificationData>>(emptyList())
    val notifications: StateFlow<List<NotificationData>> = _notifications.asStateFlow()

    fun onNotificationUpdate(notification: NotificationData): Boolean {
        var isNew = false
        _notifications.update { current ->
            when (notification.action) {
                NotificationData.Action.Posted -> {
                    val index = current.indexOfFirst { it.matches(notification) }
                    if (index == -1) {
                        isNew = true
                        current + notification
                    } else {
                        current.toMutableList().apply { set(index, notification) }
                    }
                }
                NotificationData.Action.Removed -> current.filterNot { it.matches(notification) }
            }
        }
        Log.d(TAG, "onNotificationUpdate: pkg=${notification.packageName}, id=${notification.id}, action=${notification.action}, isOngoing=${notification.isOngoing}, isNew=$isNew, totalActive=${_notifications.value.size}")
        return isNew
    }

    fun onServiceConnected() {
        // Clear all notifications on service reconnection to avoid stale ones
        _notifications.value = emptyList()
    }

    fun dismissNotification(notificationId: NotificationId) {
        _notifications.update { current ->
            current.filterNot { it.matches(notificationId) }
        }
    }

    private fun NotificationData.matches(target: NotificationData) =
        id == target.id && packageName == target.packageName

    private fun NotificationData.matches(target: NotificationId) =
        id == target.id && packageName == target.packageName

    companion object {
        private const val TAG = "NotificationController"
        @JvmStatic
        val instance: NotificationController by lazy { NotificationController() }
    }
}