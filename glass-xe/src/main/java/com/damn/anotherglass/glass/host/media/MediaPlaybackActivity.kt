package com.damn.anotherglass.glass.host.media

import android.app.Activity
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import com.damn.anotherglass.glass.host.R
import com.damn.anotherglass.shared.media.MediaCommandData
import com.damn.anotherglass.shared.media.MediaStateData
import com.damn.glass.shared.media.MediaController
import com.google.android.glass.media.Sounds
import com.google.android.glass.widget.CardBuilder
import com.google.android.glass.widget.CardScrollAdapter
import com.google.android.glass.widget.CardScrollView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MediaPlaybackActivity : Activity() {

    private lateinit var cardScroller: CardScrollView
    private lateinit var audioManager: AudioManager

    private var mediaState: MediaStateData? = null
    private var pendingSingleTap: Job? = null

    // API 19 compatibility: no lifecycleScope
    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        mediaState = MediaController.instance.getState().value

        if (!hasMedia(mediaState)) {
            finish()
            return
        }

        cardScroller = CardScrollView(this)
        cardScroller.adapter = object : CardScrollAdapter() {
            override fun getCount(): Int = 3

            override fun getItem(position: Int): Any = position

            override fun getPosition(item: Any?): Int {
                if (item !is Int) return AdapterView.INVALID_POSITION
                return item
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val current = mediaState
                return when (position) {
                    POSITION_PREV -> CardBuilder(this@MediaPlaybackActivity, CardBuilder.Layout.MENU)
                        .setText(getString(R.string.media_prev_title))
                        .setFootnote(getString(R.string.media_prev_hint))
                        .getView(convertView, parent)

                    POSITION_CENTER -> {
                        val title = current?.title ?: getString(R.string.media_no_playback)
                        val artist = current?.artist
                        val state = current?.playbackState?.name ?: "None"
                        val text = if (!artist.isNullOrBlank()) "$title\n$artist" else title
                        CardBuilder(this@MediaPlaybackActivity, CardBuilder.Layout.MENU)
                            .setText(text)
                            .setFootnote("$state | ${getString(R.string.media_center_hint)}")
                            .getView(convertView, parent)
                    }

                    POSITION_NEXT -> CardBuilder(this@MediaPlaybackActivity, CardBuilder.Layout.MENU)
                        .setText(getString(R.string.media_next_title))
                        .setFootnote(getString(R.string.media_next_hint))
                        .getView(convertView, parent)

                    else -> CardBuilder(this@MediaPlaybackActivity, CardBuilder.Layout.MENU)
                        .setText(getString(R.string.media_no_playback))
                        .getView(convertView, parent)
                }
            }
        }

        cardScroller.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                POSITION_PREV -> {
                    sendCommand(MediaCommandData.Command.Previous)
                    audioManager.playSoundEffect(Sounds.DISMISSED)
                }
                POSITION_CENTER -> onCenterTap()
                POSITION_NEXT -> {
                    sendCommand(MediaCommandData.Command.Next)
                    audioManager.playSoundEffect(Sounds.SELECTED)
                }
            }
        }

        setContentView(cardScroller)
        cardScroller.setSelection(POSITION_CENTER)

        scope.launch {
            MediaController.instance.getState().collect {
                mediaState = it
                if (!hasMedia(it)) {
                    finish()
                } else {
                    cardScroller.adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    private fun onCenterTap() {
        if (pendingSingleTap != null) {
            pendingSingleTap?.cancel()
            pendingSingleTap = null
            sendCommand(MediaCommandData.Command.Next)
            audioManager.playSoundEffect(Sounds.SELECTED)
            return
        }

        pendingSingleTap = scope.launch {
            delay(DOUBLE_TAP_TIMEOUT_MS)
            sendCommand(MediaCommandData.Command.TogglePlayPause)
            audioManager.playSoundEffect(Sounds.TAP)
            pendingSingleTap = null
        }
    }

    private fun sendCommand(command: MediaCommandData.Command) {
        MediaController.instance.sendCommand(MediaCommandData(command))
    }

    private fun hasMedia(state: MediaStateData?): Boolean {
        return state != null && state.playbackState != MediaStateData.PlaybackStateValue.None
    }

    override fun onResume() {
        super.onResume()
        cardScroller.activate()
        // Ensure the middle control card is the visible default after activation.
        cardScroller.setSelection(POSITION_CENTER)
    }

    override fun onPause() {
        cardScroller.deactivate()
        super.onPause()
    }

    override fun onDestroy() {
        pendingSingleTap?.cancel()
        pendingSingleTap = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val POSITION_PREV = 0
        private const val POSITION_CENTER = 1
        private const val POSITION_NEXT = 2
        private const val DOUBLE_TAP_TIMEOUT_MS = 280L
    }
}

