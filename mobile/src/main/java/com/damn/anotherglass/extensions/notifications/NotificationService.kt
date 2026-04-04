package com.damn.anotherglass.extensions.notifications

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.applicaster.xray.core.Logger
import com.damn.anotherglass.core.GlassService.Companion.isRunning
import com.damn.anotherglass.core.Settings
import com.damn.anotherglass.extensions.notifications.Converter.convert
import com.damn.anotherglass.extensions.notifications.filter.NotificationHistoryRepository.addNotification
import com.damn.anotherglass.logging.ALog
import com.damn.anotherglass.shared.notifications.NotificationData
import org.greenrobot.eventbus.EventBus

// todo: filter self notifications
// todo: add whitelist
class NotificationService : NotificationListenerService() {
    private var mSettings: Settings? = null

    private val log = ALog(Logger.get(TAG))

    override fun onCreate() {
        super.onCreate()
        mSettings = Settings(this)
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        emit(sbn, NotificationData.Action.Posted)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) = emit(sbn, NotificationData.Action.Removed)

    private fun emit(sbn: StatusBarNotification, posted: NotificationData.Action) {
        if (mSettings!!.isNotificationsEnabled &&
            !(mSettings!!.isMediaNotificationsIgnored && isMediaNotification(sbn)) &&
            isRunning(this)
        ) {
            log.d(TAG, "Notification received")
            // do not convert notification yet, send as is
            EventBus.getDefault().post(NotificationEvent(sbn, posted))

            val convert = convert(this, posted, sbn)
            addNotification(convert)
        }
    }

    companion object {
        private const val TAG = "NotificationService"

        // https://stackoverflow.com/a/51724784
        fun isEnabled(context: Context): Boolean {
            val contentResolver = context.contentResolver
            val enabledNotificationListeners = android.provider.Settings.Secure.getString(
                contentResolver,
                "enabled_notification_listeners"
            )
            if (enabledNotificationListeners.isNullOrEmpty()) return false
            val packageName = context.packageName
            return enabledNotificationListeners.contains(packageName)
        }

        fun isMediaNotification(sbn: StatusBarNotification): Boolean {
            val notification = sbn.notification ?: return false
            val extras = notification.extras ?: return false

            // 1. Check for MediaSession Token (The most reliable indicator)
            val hasMediaSession = extras.get(Notification.EXTRA_MEDIA_SESSION) != null

            // 2. Check for MediaStyle Template
            val template = extras.getString(Notification.EXTRA_TEMPLATE)
            val isMediaStyle = template == $$"android.app.Notification$MediaStyle"

            // 3. Check Category (Optional fallback/verification)
            val isTransportCategory = notification.category == Notification.CATEGORY_TRANSPORT

            return hasMediaSession || isMediaStyle || isTransportCategory
        }
    }
}
