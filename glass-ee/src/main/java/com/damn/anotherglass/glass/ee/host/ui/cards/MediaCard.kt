package com.damn.anotherglass.glass.ee.host.ui.cards

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import com.damn.anotherglass.glass.ee.host.R
import com.damn.anotherglass.glass.ee.host.databinding.LayoutCardMediaBinding
import com.damn.anotherglass.shared.media.MediaCommandData
import com.damn.anotherglass.shared.media.MediaStateData
import com.damn.glass.shared.media.MediaController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MediaCard : BaseFragment() {

    private var binding: LayoutCardMediaBinding? = null
    private var pendingSingleTapJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = LayoutCardMediaBinding.inflate(inflater).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MediaController.instance.getState()
            .onEach { state ->
                if (state == null) return@onEach // card is about to be removed
                binding?.update(state)
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        pendingSingleTapJob?.cancel()
        pendingSingleTapJob = null
        super.onDestroyView()
        binding = null
    }

    // Single tap toggles play/pause unless a second tap arrives quickly.
    override fun onSingleTapUp() {
        if (pendingSingleTapJob != null) {
            pendingSingleTapJob?.cancel()
            pendingSingleTapJob = null
            sendCommand(MediaCommandData.Command.Next)
            return
        }
        pendingSingleTapJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(DOUBLE_TAP_TIMEOUT_MS)
            sendCommand(MediaCommandData.Command.TogglePlayPause)
            pendingSingleTapJob = null
        }
    }

    // Two-finger tap → stop
    override fun onTwoFingerTap() {
        pendingSingleTapJob?.cancel()
        pendingSingleTapJob = null
        sendCommand(MediaCommandData.Command.Pause)
    }

    private fun sendCommand(command: MediaCommandData.Command) {
        MediaController.instance.sendCommand(MediaCommandData(command))
    }

    private fun formatAppName(state: MediaStateData): String? {
        val app = state.sourceApp?.takeIf { it.isNotBlank() }
        if (app != null) return app

        val pkg = state.sourcePackage?.takeIf { it.isNotBlank() } ?: return null
        return when (pkg) {
            "com.spotify.music" -> "Spotify"
            "com.google.android.apps.youtube.music" -> "YouTube Music"
            "com.apple.android.music" -> "Apple Music"
            "com.amazon.mp3" -> "Amazon Music"
            "com.soundcloud.android" -> "SoundCloud"
            "com.pandora.android" -> "Pandora"
            "com.tidal.mobile" -> "TIDAL"
            "deezer.android.app" -> "Deezer"
            else -> {
                val lastPart = pkg.substringAfterLast('.')
                if (lastPart.length in 2..15) {
                    lastPart.replaceFirstChar { it.uppercase() }
                } else null
            }
        }
    }

    private fun LayoutCardMediaBinding.update(state: MediaStateData) {
        val appName = formatAppName(state)

        val (stateStr, iconRes) = when (state.playbackState) {
            MediaStateData.PlaybackStateValue.Playing ->
                Pair(root.context.getString(R.string.media_state_playing), R.drawable.ic_play_arrow)
            MediaStateData.PlaybackStateValue.Paused ->
                Pair(root.context.getString(R.string.media_state_paused), R.drawable.ic_pause)
            MediaStateData.PlaybackStateValue.Buffering ->
                Pair(root.context.getString(R.string.media_state_buffering), null)
            MediaStateData.PlaybackStateValue.Stopped ->
                Pair(root.context.getString(R.string.media_state_stopped), R.drawable.ic_pause)
            MediaStateData.PlaybackStateValue.None ->
                Pair(null, null)
        }

        if (iconRes != null) {
            mediaStateIcon.visibility = View.VISIBLE
            mediaStateIcon.setImageResource(iconRes)
        } else {
            mediaStateIcon.visibility = View.GONE
        }

        val badgeText = when {
            stateStr != null && appName != null ->
                root.context.getString(R.string.media_status_with_app, stateStr, appName)
            stateStr != null -> stateStr
            appName != null -> appName
            else -> ""
        }
        mediaStatus.text = badgeText.uppercase()

        mediaTitle.text = state.title ?: ""
        mediaArtist.text = state.artist ?: ""

        footer.text = when (state.playbackState) {
            MediaStateData.PlaybackStateValue.Playing -> root.context.getString(R.string.media_hint_playing)
            MediaStateData.PlaybackStateValue.Paused  -> root.context.getString(R.string.media_hint_paused)
            else                                     -> root.context.getString(R.string.media_hint)
        }

        val artworkBytes = state.artwork?.bytes
        if (artworkBytes == null || artworkBytes.isEmpty()) {
            mediaArtwork.setImageResource(R.drawable.ic_music_50)
            mediaArtwork.scaleType = ImageView.ScaleType.FIT_CENTER
            val padding = (root.resources.displayMetrics.density * 16).toInt()
            mediaArtwork.setPadding(padding, padding, padding, padding)
            return
        }

        try {
            val bitmap = BitmapFactory.decodeByteArray(artworkBytes, 0, artworkBytes.size)
            if (bitmap != null) {
                mediaArtwork.setImageBitmap(bitmap)
                mediaArtwork.scaleType = ImageView.ScaleType.CENTER_CROP
                mediaArtwork.setPadding(0, 0, 0, 0)
            } else {
                mediaArtwork.setImageResource(R.drawable.ic_music_50)
                mediaArtwork.scaleType = ImageView.ScaleType.FIT_CENTER
                val padding = (root.resources.displayMetrics.density * 16).toInt()
                mediaArtwork.setPadding(padding, padding, padding, padding)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode media artwork: " + e.message, e)
            mediaArtwork.setImageResource(R.drawable.ic_music_50)
            mediaArtwork.scaleType = ImageView.ScaleType.FIT_CENTER
            val padding = (root.resources.displayMetrics.density * 16).toInt()
            mediaArtwork.setPadding(padding, padding, padding, padding)
        }
    }

    companion object {
        private const val TAG = "MediaCard"
        private const val DOUBLE_TAP_TIMEOUT_MS = 280L

        @JvmStatic
        fun newInstance(): MediaCard = MediaCard()
    }
}
