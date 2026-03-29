package com.damn.anotherglass.glass.ee.host.ui

import androidx.lifecycle.lifecycleScope
import com.damn.glass.shared.media.MediaController
import com.damn.glass.shared.notifications.NotificationController
import com.damn.anotherglass.glass.ee.host.ui.cards.MediaCard
import com.damn.anotherglass.glass.ee.host.ui.cards.NotificationsCard
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

object MainActivityEx {

    fun MainActivity.addNotificationsModule(timeLine: ITimeline) {
        val notificationsFlow = NotificationController.instance.getNotifications()
        // Initial check
        if (notificationsFlow.value.isNotEmpty()) {
            timeLine.addFragment(NotificationsCard.newInstance(), 0)
        }

        notificationsFlow.onEach {
            if (it.isEmpty())
                timeLine.removeByType(NotificationsCard::class.java)
            else when (val index = timeLine.indexOfFirst(NotificationsCard::class.java)) {
                -1 -> timeLine.addFragment(NotificationsCard.newInstance(), 0, true)
                else -> timeLine.setCurrent(index, true)
            }
        }.launchIn(this.lifecycleScope) // Assuming MainActivity is a LifecycleOwner
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