package com.damn.anotherglass.glass.ee.host.ui.cards

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private fun LayoutCardMediaBinding.update(state: MediaStateData) {
        mediaApp.text = state.sourceApp ?: state.sourcePackage ?: ""
        mediaTitle.text = state.title ?: ""
        mediaArtist.text = state.artist ?: ""
        mediaState.text = when (state.playbackState) {
            MediaStateData.PlaybackStateValue.Playing    -> root.context.getString(R.string.media_state_playing)
            MediaStateData.PlaybackStateValue.Paused     -> root.context.getString(R.string.media_state_paused)
            MediaStateData.PlaybackStateValue.Buffering  -> root.context.getString(R.string.media_state_buffering)
            MediaStateData.PlaybackStateValue.Stopped    -> root.context.getString(R.string.media_state_stopped)
            MediaStateData.PlaybackStateValue.None       -> ""
        }
        footer.text = root.context.getString(R.string.media_hint)

        val artworkBytes = state.artwork?.bytes
        if (artworkBytes == null || artworkBytes.isEmpty()) {
            mediaArtwork.setImageResource(R.drawable.ic_music_50)
            return
        }

        try {
            val bitmap = BitmapFactory.decodeByteArray(artworkBytes, 0, artworkBytes.size)
            if (bitmap != null) {
                mediaArtwork.setImageBitmap(bitmap)
            } else {
                mediaArtwork.setImageResource(R.drawable.ic_music_50)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode media artwork: " + e.message, e)
            mediaArtwork.setImageResource(R.drawable.ic_music_50)
        }
    }

    companion object {
        private const val TAG = "MediaCard"
        private const val DOUBLE_TAP_TIMEOUT_MS = 280L

        @JvmStatic
        fun newInstance(): MediaCard = MediaCard()
    }
}

