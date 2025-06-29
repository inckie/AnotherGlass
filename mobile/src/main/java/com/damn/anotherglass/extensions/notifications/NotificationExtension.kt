package com.damn.anotherglass.extensions.notifications

import com.applicaster.xray.core.Logger
import com.damn.anotherglass.core.GlassService
import com.damn.anotherglass.extensions.notifications.filter.FilterAction
import com.damn.anotherglass.extensions.notifications.filter.NotificationFilterChecker
import com.damn.anotherglass.extensions.notifications.filter.NotificationHistoryRepository
import com.damn.anotherglass.shared.notifications.NotificationData
import com.damn.anotherglass.shared.notifications.NotificationData.DeliveryMode
import com.damn.anotherglass.shared.notifications.NotificationsAPI
import com.damn.anotherglass.shared.rpc.RPCMessage
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
            val action = filterChecker.filter(notificationData) ?: FilterAction.ALLOW_WITH_NOTIFICATION
            if (action == FilterAction.BLOCK) {
                log.d(TAG)
                    .putData(logDetails(notificationData))
                    .message("Notification was blocked by a filter")
            } else {
                notificationData.deliveryMode = deliveryModeFromAction(action)
                service.send(RPCMessage(NotificationsAPI.ID, notificationData))
                log.d(TAG)
                    .putData(logDetails(notificationData))
                    .message("Notification was forwarded to the service")
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

        private fun deliveryModeFromAction(action: FilterAction): DeliveryMode = when (action) {
            FilterAction.ALLOW_SILENTLY -> DeliveryMode.Silent
            FilterAction.ALLOW_WITH_NOTIFICATION -> DeliveryMode.Sound
            FilterAction.BLOCK -> throw IllegalArgumentException("Cannot convert BLOCK action to delivery mode")
        }

        private fun logDetails(notificationData: NotificationData): Map<String, Any?> = mapOf(
            "packageName" to notificationData.packageName,
            "id" to notificationData.id,
            "action" to notificationData.action.name,
            "isOngoing" to notificationData.isOngoing
        )
    }

}
