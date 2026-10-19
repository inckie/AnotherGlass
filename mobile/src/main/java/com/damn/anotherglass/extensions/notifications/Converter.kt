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
import com.damn.xray.core.Logger
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
                            loadIconToBitmap(it, context, data.packageName)?.let { bitmap ->
                                msg.senderIcon = bitmap.toPngBinaryData()
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
            extractImage(context, data, notification)
        } catch (e: Exception) {
            log.e(TAG, "Failed to extract icon or image from notification", e)
        }
        return data
    }

    private fun extractImage(context: Context, data: NotificationData, notification: Notification) {
        val extras = notification.extras
        // Try BigPicture as Bitmap
        @Suppress("DEPRECATION")
        val pictureBitmap = (extras.getParcelable(Notification.EXTRA_PICTURE) as? Bitmap)
            ?: (extras.getParcelable("android.pictureIcon") as? Bitmap)
        if (pictureBitmap != null && !pictureBitmap.isRecycled) {
            data.image = pictureBitmap.toPngBinaryData()
            return
        }

        // Try BigPicture as Icon (Android 9+)
        @Suppress("DEPRECATION")
        val pictureIcon = (extras.getParcelable(Notification.EXTRA_PICTURE) as? android.graphics.drawable.Icon)
            ?: (extras.getParcelable("android.pictureIcon") as? android.graphics.drawable.Icon)
        if (pictureIcon != null) {
            loadIconToBitmap(pictureIcon, context, data.packageName)?.let {
                data.image = it.toPngBinaryData()
            }
        }
    }

    private fun extractIcon(
        context: Context,
        data: NotificationData,
        notification: Notification
    ) {
        val packageName = data.packageName

        // 1. Try to extract large icon first (custom notification icon, contact photo, thumbnail)
        @Suppress("DEPRECATION")
        val largeIcon = notification.getLargeIcon()
            ?: (notification.extras.getParcelable(Notification.EXTRA_LARGE_ICON) as? android.graphics.drawable.Icon)
            ?: (notification.extras.getParcelable("android.largeIcon.big") as? android.graphics.drawable.Icon)

        if (largeIcon != null) {
            loadIconToBitmap(largeIcon, context, packageName)?.let {
                setIconData(data, it)
                return
            }
        }

        // Try large icon as Bitmap (from extras or deprecated field)
        @Suppress("DEPRECATION")
        val largeBitmap = (notification.extras.getParcelable(Notification.EXTRA_LARGE_ICON) as? Bitmap)
            ?: (notification.extras.getParcelable("android.largeIcon.big") as? Bitmap)
            ?: notification.largeIcon

        if (largeBitmap != null && !largeBitmap.isRecycled) {
            setIconData(data, largeBitmap)
            return
        }

        // 2. Try small icon from notification
        @Suppress("DEPRECATION")
        val smallIcon = notification.smallIcon
            ?: (notification.extras.getParcelable(Notification.EXTRA_SMALL_ICON) as? android.graphics.drawable.Icon)

        if (smallIcon != null) {
            loadIconToBitmap(smallIcon, context, packageName)?.let {
                setIconData(data, it)
                return
            }
        }

        // Try legacy small icon resource ID
        @Suppress("DEPRECATION")
        val iconResId = notification.icon
        if (iconResId != 0 && !packageName.isNullOrBlank()) {
            try {
                val packageContext = context.createPackageContext(packageName, 0)
                val drawable = packageContext.getDrawable(iconResId)
                if (drawable != null) {
                    setIconData(data, drawableToBitmap(drawable))
                    return
                }
            } catch (e: Exception) {
                // Ignore and fall back to app icon
            }
        }

        // 3. Fallback to Application icon (guaranteed to exist for every installed app)
        if (!packageName.isNullOrBlank()) {
            try {
                val pm = context.packageManager
                val appDrawable = pm.getApplicationIcon(packageName)
                setIconData(data, drawableToBitmap(appDrawable))
            } catch (e: Exception) {
                log.e(TAG, "Failed to load application icon as fallback for $packageName", e)
            }
        }
    }

    private fun loadIconToBitmap(
        icon: android.graphics.drawable.Icon,
        context: Context,
        packageName: String?
    ): Bitmap? {
        var drawable: Drawable? = null
        if (!packageName.isNullOrBlank()) {
            try {
                val packageContext = context.createPackageContext(packageName, 0)
                drawable = icon.loadDrawable(packageContext)
            } catch (e: Exception) {
                // Ignore and fall back to context
            }
        }
        if (drawable == null) {
            try {
                drawable = icon.loadDrawable(context)
            } catch (e: Exception) {
                // Ignore
            }
        }
        return drawable?.let { drawableToBitmap(it) }
    }

    private fun setIconData(data: NotificationData, bitmap: Bitmap) {
        data.icon = bitmap.toPngBinaryData()
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            if (null != drawable.bitmap && !drawable.bitmap.isRecycled) {
                val bmp = drawable.bitmap
                if (bmp.width in 1..256 && bmp.height in 1..256) {
                    return bmp
                }
            }
        }
        val defaultSize = 96
        val rawWidth = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else defaultSize
        val rawHeight = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else defaultSize

        val maxDim = 192
        val (finalWidth, finalHeight) = if (rawWidth > maxDim || rawHeight > maxDim) {
            val ratio = rawWidth.toFloat() / rawHeight.toFloat()
            if (rawWidth > rawHeight) {
                maxDim to (maxDim / ratio).toInt().coerceAtLeast(1)
            } else {
                (maxDim * ratio).toInt().coerceAtLeast(1) to maxDim
            }
        } else {
            rawWidth to rawHeight
        }

        val bitmap = createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
