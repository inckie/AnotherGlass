package com.damn.anotherglass.extensions.notifications

import com.applicaster.xray.core.Logger
import com.damn.anotherglass.core.GlassService
import com.damn.anotherglass.extensions.notifications.filter.NotificationFilterChecker
import com.damn.anotherglass.extensions.notifications.filter.NotificationHistoryRepository
import com.damn.anotherglass.shared.rpc.RPCMessage
import com.damn.anotherglass.shared.notifications.NotificationsAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class NotificationExtension(private val service: GlassService) {

    private val log = Logger.get(TAG)
    private val filterChecker = NotificationFilterChecker(service)

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: NotificationEvent) {
        val notificationData = Converter.convert(service, event.action, event.notification)

        CoroutineScope(Dispatchers.IO).launch {
            if (filterChecker.shouldSendToGlass(notificationData)) {
                service.send(RPCMessage(NotificationsAPI.ID, notificationData))
                log.d(TAG).putData(mapOf(
                    "packageName" to notificationData.packageName,
                    "id" to notificationData.id,
                    "action" to notificationData.action.name,
                    "isOngoing" to notificationData.isOngoing
                )).message("Notification was forwarded to the service")
            } else {
                log.d(TAG).putData(mapOf(
                    "packageName" to notificationData.packageName,
                    "id" to notificationData.id,
                    "action" to notificationData.action.name
                )).message("Notification was blocked by a filter")
            }
            // Always add to history, even if blocked, so user can see what was blocked.
            NotificationHistoryRepository.addNotification(notificationData)
        }
    }

    fun start() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        log.i(TAG).message("Notification extension started")
    }

    fun stop() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        log.i(TAG).message("Notification extension stopped")
    }

    companion object {
        private const val TAG = "NotificationExtension"
    }
}