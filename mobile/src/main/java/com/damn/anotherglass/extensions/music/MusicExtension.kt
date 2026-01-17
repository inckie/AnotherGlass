package com.damn.anotherglass.extensions.music

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.applicaster.xray.core.Logger
import com.damn.anotherglass.core.GlassService
import com.damn.anotherglass.extensions.notifications.NotificationService
import com.damn.anotherglass.shared.music.MusicAPI
import com.damn.anotherglass.shared.music.MusicControl
import com.damn.anotherglass.shared.music.MusicData
import com.damn.anotherglass.shared.rpc.RPCMessage

class MusicExtension(private val service: GlassService) {

    private val log = Logger.get(TAG)
    private var mediaSessionManager: MediaSessionManager? = null
    private var currentController: MediaController? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = false

    private val progressRunnable = object : Runnable {
        override fun run() {
            if (isPlaying && currentController != null) {
                sendUpdate()
                handler.postDelayed(this, PROGRESS_UPDATE_INTERVAL)
            }
        }
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateController(controllers)
    }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            val wasPlaying = isPlaying
            isPlaying = state?.state == PlaybackState.STATE_PLAYING
            sendUpdate()
            
            if (isPlaying && !wasPlaying) {
                handler.removeCallbacks(progressRunnable)
                handler.postDelayed(progressRunnable, PROGRESS_UPDATE_INTERVAL)
            } else if (!isPlaying) {
                handler.removeCallbacks(progressRunnable)
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            sendUpdate()
        }

        override fun onSessionDestroyed() {
            handler.removeCallbacks(progressRunnable)
            currentController = null
            isPlaying = false
        }
    }

    fun start() {
        try {
            mediaSessionManager = service.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = ComponentName(service, NotificationService::class.java)
            
            if (NotificationService.isEnabled(service)) {
                 val controllers = mediaSessionManager?.getActiveSessions(componentName)
                 updateController(controllers)
                 mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsListener, componentName)
                 log.i(TAG).message("MusicExtension started")
            } else {
                log.w(TAG).message("NotificationService not enabled, cannot get media sessions")
            }

        } catch (e: SecurityException) {
            log.e(TAG).exception(e).message("Failed to access media sessions")
        } catch (e: Exception) {
            log.e(TAG).exception(e).message("Error starting MusicExtension")
        }
    }

    fun stop() {
        try {
            handler.removeCallbacks(progressRunnable)
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
            currentController?.unregisterCallback(callback)
            currentController = null
            isPlaying = false
            log.i(TAG).message("MusicExtension stopped")
        } catch (e: Exception) {
            log.e(TAG).exception(e).message("Error stopping MusicExtension")
        }
    }

    fun onMessage(payload: Any?) {
        if (payload is MusicControl && currentController != null) {
            val controls = currentController?.transportControls
            when (payload) {
                MusicControl.Play -> controls?.play()
                MusicControl.Pause -> controls?.pause()
                MusicControl.Next -> controls?.skipToNext()
                MusicControl.Previous -> controls?.skipToPrevious()
            }
        }
    }

    private fun updateController(controllers: List<MediaController>?) {
        // Find YouTube Music controller
        val ytMusicController = controllers?.find { it.packageName == "com.google.android.apps.youtube.music" }

        if (ytMusicController != null) {
            if (currentController?.sessionToken != ytMusicController.sessionToken) {
                currentController?.unregisterCallback(callback)
                currentController = ytMusicController
                currentController?.registerCallback(callback, android.os.Handler(service.mainLooper))
                sendUpdate()
                log.d(TAG).message("Locked onto YouTube Music session")
            }
        } else {
             if (currentController != null && controllers?.contains(currentController) == false) {
                 currentController?.unregisterCallback(callback)
                 currentController = null
             }
        }
    }

    private fun sendUpdate() {
        val controller = currentController ?: return
        val metadata = controller.metadata ?: return
        val playbackState = controller.playbackState

        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val track = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING

        // Calculate current position accounting for time elapsed since last update
        var position = playbackState?.position ?: 0L
        if (isPlaying && playbackState != null) {
            val timeDelta = SystemClock.elapsedRealtime() - playbackState.lastPositionUpdateTime
            val speed = playbackState.playbackSpeed
            position += (timeDelta * speed).toLong()
        }

        val data = MusicData(artist, track, null, isPlaying, position, duration)
        service.send(RPCMessage(MusicAPI.ID, data))
    }

    companion object {
        private const val TAG = "MusicExtension"
        private const val PROGRESS_UPDATE_INTERVAL = 1000L // 1 second
    }
}
