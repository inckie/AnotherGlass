package com.damn.anotherglass.glass.ee.host.ui

import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.damn.glass.shared.media.MediaController
import com.damn.glass.shared.notifications.NotificationController
import com.damn.glass.shared.notifications.NotificationId
import com.damn.anotherglass.glass.ee.host.ui.cards.MediaCard
import com.damn.anotherglass.glass.ee.host.ui.cards.NotificationsCard
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

object MainActivityEx {

    private const val TAG = "MainActivityEx"

    fun MainActivity.addNotificationsModule(timeLine: ITimeline) {
        val notificationsFlow = NotificationController.instance.notifications
        var knownNotificationIds = notificationsFlow.value.mapTo(mutableSetOf()) { NotificationId(it) }

        // Initial check — add card without scrolling if notifications already exist
        if (notificationsFlow.value.isNotEmpty()) {
            Log.d(TAG, "Initial check: adding NotificationsCard (${notificationsFlow.value.size} active)")
            timeLine.addFragment(NotificationsCard.newInstance(), 0)
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
                    Log.d(TAG, "Adding NotificationsCard to timeline at 0 (scrollTo=$hasNewNotification)")
                    timeLine.addFragment(NotificationsCard.newInstance(), 0, hasNewNotification)
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
        }.launchIn(lifecycleScope)
    }

    fun MainActivity.addMediaModule(timeLine: ITimeline) {
        val mediaFlow = MediaController.instance.getState()
        // Initial check — show card if there is already a media state
        if (mediaFlow.value != null) {
            timeLine.addFragment(MediaCard.newInstance(), 0)
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