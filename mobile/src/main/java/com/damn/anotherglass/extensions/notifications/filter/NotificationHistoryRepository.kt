package com.damn.anotherglass.extensions.notifications.filter

import com.damn.anotherglass.shared.notifications.NotificationData // Import your class
import java.util.ArrayDeque

object NotificationHistoryRepository {
    private const val MAX_HISTORY_SIZE = 100
    private val notificationHistory: ArrayDeque<NotificationData> = ArrayDeque(MAX_HISTORY_SIZE)
    private val lock = Any()

    fun addNotification(item: NotificationData) {
        synchronized(lock) {
            if (item.action == NotificationData.Action.Posted) {
                if(notificationHistory.any { it.id == item.id }) {
                    return
                }
                if (notificationHistory.size >= MAX_HISTORY_SIZE) {
                    notificationHistory.removeLast()
                }
                notificationHistory.addFirst(item)
            }
        }
    }

    fun getHistory(): List<NotificationData> {
        synchronized(lock) {
            return ArrayList(notificationHistory)
        }
    }

    fun clearHistory() {
        synchronized(lock) {
            notificationHistory.clear()
        }
    }

    // getItemById would likely use the 'id' field from NotificationData
    fun getItemByNotificationId(notificationId: Int): NotificationData? {
        synchronized(lock) {
            // Find the latest entry for this ID if multiple updates were posted
            return notificationHistory.find { it.id == notificationId && it.action == NotificationData.Action.Posted }
        }
    }
}