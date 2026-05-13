package com.damn.anotherglass.extensions.notifications

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.graphics.createBitmap
import com.applicaster.xray.core.Logger
import com.damn.anotherglass.logging.ALog
import com.damn.anotherglass.shared.notifications.NotificationData
import com.damn.anotherglass.utility.toPngBinaryData

object Converter {
    private const val TAG = "IconConverter"
    private val log = ALog(Logger.get(TAG))

    fun convert(
        context: Context,
        action: NotificationData.Action,
        sbn: StatusBarNotification
    ): NotificationData {
        val data = NotificationData()
        data.action = action

        // parse basic data
        data.id = sbn.id
        data.packageName = sbn.packageName
        data.postedTime = sbn.postTime
        data.isOngoing = sbn.isOngoing

        val pm = context.packageManager
        try {
            val ai = pm.getApplicationInfo(data.packageName, 0)
            data.appName = pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            data.appName = data.packageName
        }

        if (action == NotificationData.Action.Removed) return data

        val notification = sbn.notification
        val extras = notification.extras

        data.title = extras.getString(Notification.EXTRA_TITLE)
        data.text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

        // handle BigText
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let {
            data.text = it.toString()
        }

        // handle InboxStyle
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.let { lines ->
            if (lines.isNotEmpty()) {
                data.text = lines.joinToString("\n")
            }
        }

        // handle MessagingStyle (Manual Bundle Parsing for maximum compatibility and avoiding GDK compile issues)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.let {
                data.conversationTitle = it.toString()
            }
            
            // Check for group conversation (API 28+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                data.isGroupConversation = extras.getBoolean("android.isGroupConversation")
            }

            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (messages != null) {
                for (m in messages) {
                    if (m is Bundle) {
                        val msg = NotificationData.Message()
                        msg.text = m.getCharSequence("text")?.toString()
                        msg.time = m.getLong("time")
                        
                        // Extract sender info (API 28+ Person or legacy String)
                        val senderPerson = m.get("sender_person")
                        var icon: android.graphics.drawable.Icon? = null
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && senderPerson is android.app.Person) {
                            msg.sender = senderPerson.name?.toString()
                            icon = senderPerson.icon
                        } else if (senderPerson is Bundle) {
                            msg.sender = senderPerson.getCharSequence("name")?.toString()
                            @Suppress("DEPRECATION")
                            icon = senderPerson.getParcelable("icon")
                        } else {
                            msg.sender = m.getCharSequence("sender")?.toString()
                        }

                        icon?.let {
                            try {
                                it.loadDrawable(context)?.let { drawable ->
                                    msg.senderIcon = drawableToBitmap(drawable).toPngBinaryData()
                                }
                            } catch (e: Exception) {
                                log.e(TAG, "Failed to load person icon", e)
                            }
                        }
                        data.messages.add(msg)
                    }
                }
            }
        }

        if (null != notification.tickerText) {
            data.tickerText = notification.tickerText.toString()
        }

        try {
            extractIcon(context, data, notification)
            extractImage(data, notification)
        } catch (e: Exception) {
            log.e(TAG, "Failed to extract icon or image from notification", e)
        }
        return data
    }

    private fun extractImage(data: NotificationData, notification: Notification) {
        val extras = notification.extras
        // Try BigPicture
        (extras.getParcelable<Bitmap>(Notification.EXTRA_PICTURE) 
            ?: extras.getParcelable<Bitmap>("android.pictureIcon"))?.let {
            data.image = it.toPngBinaryData()
        }
    }

    private fun extractIcon(
        context: Context,
        data: NotificationData,
        notification: Notification
    ) {
        var icon = notification.getLargeIcon()
        if (null == icon) icon = notification.smallIcon
        if (null != icon) {
            try {
                val drawable = icon.loadDrawable(context)
                if (null != drawable) {
                    val bitmap = drawableToBitmap(drawable)
                    setIconData(data, bitmap)
                }
            } catch (e: Exception) {
                log.e(TAG, "Failed to load icon drawable", e)
            }
        }
        if (null != data.icon) return
        
        @Suppress("DEPRECATION")
        if (null != notification.largeIcon) setIconData(data, notification.largeIcon)
    }

    private fun setIconData(data: NotificationData, bitmap: Bitmap) {
        data.icon = bitmap.toPngBinaryData()
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            if (null != drawable.bitmap) {
                return drawable.bitmap
            }
        }
        val bitmap: Bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            createBitmap(1, 1)
        } else {
            createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
