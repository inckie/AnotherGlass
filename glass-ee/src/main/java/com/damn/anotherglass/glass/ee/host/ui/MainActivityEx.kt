package com.damn.anotherglass.glass.ee.host.ui

import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.damn.glass.shared.media.MediaController
import com.damn.glass.shared.notifications.NotificationController
import com.damn.glass.shared.notifications.NotificationId
import com.damn.anotherglass.glass.ee.host.core.Settings
import com.damn.anotherglass.glass.ee.host.ui.cards.MapCard
import com.damn.anotherglass.glass.ee.host.ui.cards.MediaCard
import com.damn.anotherglass.glass.ee.host.ui.cards.NotificationsCard
import com.damn.anotherglass.glass.ee.host.ui.cards.ServiceStateCard
import com.damn.anotherglass.glass.ee.host.utility.wakeScreen
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

object MainActivityEx {

    private const val TAG = "MainActivityEx"

    private fun getNotificationInsertIndex(timeLine: ITimeline): Int {
        val mapIndex = timeLine.indexOfFirst(MapCard::class.java)
        if (mapIndex != -1) {
            return mapIndex
        }
        val defaultIndex = timeLine.indexOfFirst(ServiceStateCard::class.java)
        if (defaultIndex != -1) {
            return defaultIndex + 1
        }
        return 0
    }

    fun MainActivity.addNotificationsModule(timeLine: ITimeline) {
        val notificationsFlow = NotificationController.instance.notifications
        var knownNotificationIds = notificationsFlow.value.mapTo(mutableSetOf()) { NotificationId(it) }

        // Initial check — add card without scrolling if notifications already exist
        if (notificationsFlow.value.isNotEmpty()) {
            val insertIndex = getNotificationInsertIndex(timeLine)
            Log.d(TAG, "Initial check: adding NotificationsCard at $insertIndex (${notificationsFlow.value.size} active)")
            timeLine.addFragment(NotificationsCard.newInstance(), insertIndex, false)
        }

        notificationsFlow.onEach { notifications ->
            if (notifications.isEmpty()) {
                Log.d(TAG, "Notifications empty, removing NotificationsCard")
                knownNotificationIds.clear()
                timeLine.removeByType(NotificationsCard::class.java)
                return@onEach
            }

            val currentIds = notifications.mapTo(mutableSetOf()) { NotificationId(it) }
            val newIds = currentIds - knownNotificationIds
            val hasNewNotification = newIds.isNotEmpty()
            knownNotificationIds = currentIds

            val summaries = notifications.map { "${it.packageName}#${it.id}(ongoing=${it.isOngoing}, title='${it.title}')" }
            Log.d(TAG, "Notifications emitted (${notifications.size}): $summaries")
            Log.d(TAG, "hasNewNotification=$hasNewNotification, newIds=$newIds")

            when (val index = timeLine.indexOfFirst(NotificationsCard::class.java)) {
                -1 -> {
                    val insertIndex = getNotificationInsertIndex(timeLine)
                    Log.d(TAG, "Adding NotificationsCard to timeline at $insertIndex (scrollTo=$hasNewNotification)")
                    timeLine.addFragment(NotificationsCard.newInstance(), insertIndex, hasNewNotification)
                }
                else -> {
                    if (hasNewNotification) {
                        Log.d(TAG, "Scrolling timeline to NotificationsCard at index $index for new notifications: $newIds")
                        timeLine.setCurrent(index, true)
                    } else {
                        Log.d(TAG, "NotificationsCard already at index $index, not scrolling (only updates/removals)")
                    }
                }
            }

            if (hasNewNotification && Settings(this@addNotificationsModule).wakeOnNotification) {
                wakeScreen(5000L, "AnotherGlass:NotificationWake")
            }
        }.launchIn(lifecycleScope)
    }

    fun MainActivity.addMediaModule(timeLine: ITimeline) {
        val mediaFlow = MediaController.instance.getState()
        // Initial check — show card if there is already a media state
        if (mediaFlow.value != null) {
            timeLine.addFragment(MediaCard.newInstance(), 0, false)
        }

        mediaFlow.onEach { state ->
            if (state == null)
                timeLine.removeByType(MediaCard::class.java)
            else when (timeLine.indexOfFirst(MediaCard::class.java)) {
                -1 -> timeLine.addFragment(MediaCard.newInstance(), 0, true)
                // card already present — it updates itself via its own flow observer
            }
        }.launchIn(this.lifecycleScope)
    }
}