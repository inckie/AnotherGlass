package com.damn.anotherglass.glass.ee.host.ui.cards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
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
            MediaStateData.PlaybackStateValue.Playing    -> "▶ Playing"
            MediaStateData.PlaybackStateValue.Paused     -> "⏸ Paused"
            MediaStateData.PlaybackStateValue.Buffering  -> "⏳ Buffering"
            MediaStateData.PlaybackStateValue.Stopped    -> "⏹ Stopped"
            MediaStateData.PlaybackStateValue.None       -> ""
        }
        footer.text = "Tap: play/pause  •  Double tap: next  •  ✌ tap: stop"
    }

    companion object {
        private const val DOUBLE_TAP_TIMEOUT_MS = 280L

        @JvmStatic
        fun newInstance(): MediaCard = MediaCard()
    }
}

