package com.damn.anotherglass.glass.ee.host.ui

import com.damn.anotherglass.glass.ee.host.core.NotificationController
import com.damn.anotherglass.glass.ee.host.ui.cards.NotificationsCard

object MainActivityEx {

    fun MainActivity.addNotificationsModule(timeLine: ITimeline) {
        val notifications = NotificationController.instance.getNotifications()
        if(false == notifications.value?.isEmpty()){
            timeLine.addFragment(NotificationsCard.newInstance(), 0)
        }
        // todo: use tags instead of classes
        notifications.observe(this, {
            if(it.isEmpty())
                timeLine.removeByType(NotificationsCard::class.java)
            else when (val index = timeLine.indexOfFirst(NotificationsCard::class.java)) {
                -1 -> timeLine.addFragment(NotificationsCard.newInstance(), 0, true)
                else -> timeLine.setCurrent(index, true)
            }
        })
    }

}