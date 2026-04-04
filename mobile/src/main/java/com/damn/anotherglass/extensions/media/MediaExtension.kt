package com.damn.anotherglass.extensions.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.graphics.scale
import com.applicaster.xray.core.Logger
import com.damn.anotherglass.core.GlassService
import com.damn.anotherglass.extensions.notifications.NotificationService
import com.damn.anotherglass.logging.ALog
import com.damn.anotherglass.shared.BinaryData
import com.damn.anotherglass.shared.media.MediaAPI
import com.damn.anotherglass.shared.media.MediaCommandData
import com.damn.anotherglass.shared.media.MediaStateData
import com.damn.anotherglass.shared.rpc.RPCMessage
import com.damn.anotherglass.utility.AndroidAppDetailsProvider
import com.damn.anotherglass.utility.toJpegBinaryData
import kotlin.math.roundToInt

class MediaExtension(
    private val service: GlassService,
    private val onMediaStateChanged: (MediaStateData) -> Unit,
) {

    private val log = ALog(Logger.get(TAG))
    private val mediaSessionManager =
        service.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val notificationListenerComponent =
        ComponentName(service, NotificationService::class.java)

    private var isStarted = false
    private var activeController: MediaController? = null
    private var lastPayloadFingerprint = ""
    private var lastEmitTimeMs = 0L
    private val mainHandler = Handler(Looper.getMainLooper())
    private val appDetailsProvider = AndroidAppDetailsProvider(service)

    private val activeSessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            selectActiveController(controllers.orEmpty())
        }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            emitState()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            emitState()
        }

        override fun onSessionDestroyed() {
            activeController = null
            selectActiveController(getActiveSessionsSafe())
            emitState()
        }
    }

    fun start() {
        if (isStarted) return
        isStarted = true
        try {
            mediaSessionManager.addOnActiveSessionsChangedListener(
                activeSessionsListener,
                notificationListenerComponent
            )
            selectActiveController(getActiveSessionsSafe())
            emitState(force = true)
            log.i(TAG, "Media extension started")
        } catch (e: SecurityException) {
            log.w(TAG, "Cannot access media sessions: ${e.message}")
            emitEmptyState(force = true)
        }
    }

    fun stop() {
        if (!isStarted) return
        isStarted = false
        mainHandler.removeCallbacksAndMessages(null)
        try {
            mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsListener)
        } catch (_: Exception) {
        }
        detachController()
        emitEmptyState(force = true)
        log.i(TAG, "Media extension stopped")
    }

    fun onCommand(command: MediaCommandData) {
        val controller = activeController ?: run {
            log.d(TAG, "Ignoring media command, no active session")
            return
        }
        val controls = controller.transportControls
        when (command.command) {
            MediaCommandData.Command.Play -> controls.play()
            MediaCommandData.Command.Pause -> controls.pause()
            MediaCommandData.Command.TogglePlayPause -> togglePlayPause(controller, controls)
            MediaCommandData.Command.Next -> controls.skipToNext()
            MediaCommandData.Command.Previous -> controls.skipToPrevious()
            MediaCommandData.Command.SeekTo -> controls.seekTo(command.seekToMs)
        }

        // Some players emit delayed/incomplete callbacks for rapid transport commands.
        // Force a short follow-up sync so Glass receives the final track metadata.
        schedulePostCommandSync()
    }

    private fun schedulePostCommandSync() {
        emitState(force = true)
        mainHandler.postDelayed({ emitState(force = true) }, POST_COMMAND_SYNC_DELAY_MS)
        mainHandler.postDelayed({ emitState(force = true) }, POST_COMMAND_SYNC_DELAY_LATE_MS)
    }

    private fun togglePlayPause(
        controller: MediaController,
        controls: MediaController.TransportControls,
    ) {
        val state = controller.playbackState?.state
        if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
            controls.pause()
        } else {
            controls.play()
        }
    }

    private fun selectActiveController(controllers: List<MediaController>) {
        val next = chooseController(controllers)
        if (next?.sessionToken == activeController?.sessionToken) {
            return
        }
        detachController()
        activeController = next
        next?.registerCallback(controllerCallback)
        emitState(force = true)
    }

    private fun chooseController(controllers: List<MediaController>): MediaController? {
        if (controllers.isEmpty()) return null

        controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        }?.let { return it }

        controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_BUFFERING
        }?.let { return it }

        return controllers.firstOrNull()
    }

    private fun detachController() {
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
    }

    private fun emitState(force: Boolean = false) {
        val payload = buildState(activeController)
        val fingerprint = fingerprint(payload)
        val now = SystemClock.elapsedRealtime()
        val shouldThrottle = !force &&
            fingerprint == lastPayloadFingerprint &&
            now - lastEmitTimeMs < POSITION_THROTTLE_MS

        if (shouldThrottle) return

        lastPayloadFingerprint = fingerprint
        lastEmitTimeMs = now
        onMediaStateChanged(payload)
        service.send(RPCMessage(MediaAPI.ID, payload))
    }

    private fun emitEmptyState(force: Boolean = false) {
        val empty = MediaStateData(lastUpdatedMs = System.currentTimeMillis())
        val fingerprint = fingerprint(empty)
        if (!force && fingerprint == lastPayloadFingerprint) return

        lastPayloadFingerprint = fingerprint
        lastEmitTimeMs = SystemClock.elapsedRealtime()
        onMediaStateChanged(empty)
        service.send(RPCMessage(MediaAPI.ID, empty))
    }

    private fun buildState(controller: MediaController?): MediaStateData {
        if (controller == null) {
            return MediaStateData(lastUpdatedMs = System.currentTimeMillis())
        }

        val playbackState = controller.playbackState
        val metadata = controller.metadata
        val artwork = buildArtwork(metadata)
        return MediaStateData(
            playbackState = mapPlaybackState(playbackState?.state),
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
            album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM),
            artwork = artwork,
            sourceApp = resolveApplicationName(controller.packageName),
            sourcePackage = controller.packageName,
            positionMs = playbackState?.position ?: 0L,
            durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            actionsMask = playbackState?.actions ?: 0L,
            lastUpdatedMs = System.currentTimeMillis(),
        )
    }

    private fun mapPlaybackState(state: Int?): MediaStateData.PlaybackStateValue = when (state) {
        PlaybackState.STATE_PLAYING -> MediaStateData.PlaybackStateValue.Playing
        PlaybackState.STATE_PAUSED -> MediaStateData.PlaybackStateValue.Paused
        PlaybackState.STATE_STOPPED -> MediaStateData.PlaybackStateValue.Stopped
        PlaybackState.STATE_BUFFERING,
        PlaybackState.STATE_CONNECTING,
        PlaybackState.STATE_FAST_FORWARDING,
        PlaybackState.STATE_REWINDING,
        PlaybackState.STATE_SKIPPING_TO_NEXT,
        PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
        PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> MediaStateData.PlaybackStateValue.Buffering
        else -> MediaStateData.PlaybackStateValue.None
    }

    private fun resolveApplicationName(packageName: String): String {
        return appDetailsProvider.getAppDetails(packageName).appName
    }

    private fun getActiveSessionsSafe(): List<MediaController> {
        if (!NotificationService.isEnabled(service)) {
            log.w(TAG, "Notification listener is disabled, media session list may be empty")
        }
        return try {
            mediaSessionManager.getActiveSessions(notificationListenerComponent)
        } catch (e: SecurityException) {
            log.w(TAG, "Cannot read active sessions: ${e.message}")
            emptyList()
        }
    }

    private fun buildArtwork(metadata: MediaMetadata?): BinaryData? {
        val source = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            ?: return null

        if (source.width <= 0 || source.height <= 0) return null

        var scaled: Bitmap? = null
        var cropped: Bitmap? = null
        return try {
            val scale = maxOf(
                ARTWORK_SIZE_PX.toFloat() / source.width.toFloat(),
                ARTWORK_SIZE_PX.toFloat() / source.height.toFloat(),
            )
            val scaledWidth = (source.width * scale).roundToInt().coerceAtLeast(ARTWORK_SIZE_PX)
            val scaledHeight = (source.height * scale).roundToInt().coerceAtLeast(ARTWORK_SIZE_PX)

            scaled = if (scaledWidth == source.width && scaledHeight == source.height) {
                source
            } else {
                source.scale(scaledWidth, scaledHeight)
            }

            val x = ((scaledWidth - ARTWORK_SIZE_PX) / 2).coerceAtLeast(0)
            val y = ((scaledHeight - ARTWORK_SIZE_PX) / 2).coerceAtLeast(0)
            cropped = Bitmap.createBitmap(scaled, x, y, ARTWORK_SIZE_PX, ARTWORK_SIZE_PX)

            cropped.toJpegBinaryData(ARTWORK_JPEG_QUALITY)
        } catch (_: Throwable) {
            null
        } finally {
            if (cropped != null && cropped !== scaled && !cropped.isRecycled) {
                cropped.recycle()
            }
            if (scaled != null && scaled !== source && !scaled.isRecycled) {
                scaled.recycle()
            }
        }
    }

    private fun fingerprint(payload: MediaStateData): String = buildString {
        append(payload.playbackState.name)
        append('|')
        append(payload.title ?: "")
        append('|')
        append(payload.artist ?: "")
        append('|')
        append(payload.album ?: "")
        append('|')
        append(payload.sourcePackage ?: "")
        append('|')
        append(payload.positionMs / 1000)
        append('|')
        append(payload.durationMs)
        append('|')
        append(payload.actionsMask)
        append('|')
        val artwork = payload.artwork?.bytes
        if (artwork == null) {
            append("none")
        } else {
            append(artwork.size)
            append(':')
            append(artwork.contentHashCode())
        }
    }

    companion object {
        private const val TAG = "MediaExtension"
        private const val POSITION_THROTTLE_MS = 1000L
        private const val POST_COMMAND_SYNC_DELAY_MS = 250L
        private const val POST_COMMAND_SYNC_DELAY_LATE_MS = 900L
        private const val ARTWORK_SIZE_PX = 360
        private const val ARTWORK_JPEG_QUALITY = 95
    }
}

