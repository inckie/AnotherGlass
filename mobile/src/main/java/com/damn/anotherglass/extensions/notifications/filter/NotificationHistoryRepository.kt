package com.damn.anotherglass.extensions.notifications.filter

import com.damn.anotherglass.shared.notifications.NotificationData // Import your class
import java.util.ArrayDeque

object NotificationHistoryRepository {

    private const val MAX_HISTORY_SIZE = 100

    private val notificationHistory: ArrayDeque<NotificationData> = ArrayDeque(MAX_HISTORY_SIZE)

    fun addNotification(item: NotificationData) {
        if (item.action != NotificationData.Action.Posted) return
        synchronized(notificationHistory) {
            if (notificationHistory.any { it.id == item.id }) {
                return
            }
            if (notificationHistory.size >= MAX_HISTORY_SIZE) {
                notificationHistory.removeLast()
            }
            notificationHistory.addFirst(item)
        }
    }

    fun getHistory(): List<NotificationData> = synchronized(notificationHistory) {
        return ArrayList(notificationHistory)
    }

    fun clearHistory() = synchronized(notificationHistory) {
        notificationHistory.clear()
    }
}
