package com.damn.anotherglass.glass.ee.host.core

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.damn.anotherglass.shared.notifications.NotificationData

class NotificationController {

    private val notifications = MutableLiveData(listOf<NotificationData>())

    fun getNotifications(): LiveData<List<NotificationData>> = notifications

    fun onNotificationUpdate(notification: NotificationData) {
        // We expect that modification will always happen, so make mutable copy in any case
        val current = notifications.value!!.toMutableList()
        when (notification.action) {
            NotificationData.Action.Posted -> {
                when (val index = current.indexOfFirst { it.id == notification.id }) {
                    -1 -> current += notification
                    else -> current[index] = notification
                }
                notifications.postValue(current)
            }

            NotificationData.Action.Removed -> {
                if (current.removeIf { it.id == notification.id }) {
                    notifications.postValue(current)
                }
            }
        }
    }

    fun onServiceConnected() {
        // Clear all notifications on service reconnection to avoid stale ones
        notifications.postValue(emptyList())
    }

    companion object {
        private val TAG = "NotificationController"
        val instance: NotificationController by lazy { NotificationController() }
    }
}
