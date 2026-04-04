package com.damn.anotherglass.glass.host.media

import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import com.damn.anotherglass.glass.host.HostService
import com.damn.anotherglass.glass.host.R
import com.damn.anotherglass.shared.media.MediaStateData
import com.damn.anotherglass.shared.media.MediaStateData.PlaybackStateValue
import com.damn.glass.shared.media.MediaController.Companion.instance
import com.google.android.glass.timeline.LiveCard

class MediaCardController(private val service: HostService) {
    private var mediaCard: LiveCard? = null

    fun onServiceConnected() {
        instance.onServiceConnected()
        remove()
    }

    fun onMediaStateUpdate(state: MediaStateData) {
        if (!hasPlayableSession(state)) {
            remove()
            return
        }

        if (mediaCard == null) {
            mediaCard = LiveCard(service, LIVE_CARD_TAG)
        }

        mediaCard!!.setViews(buildViews(state))
        mediaCard!!.setAction(this.playbackPendingIntent)

        if (!mediaCard!!.isPublished) {
            mediaCard!!.publish(LiveCard.PublishMode.REVEAL)
        }
    }

    fun remove() {
        if (mediaCard == null) {
            return
        }
        if (mediaCard!!.isPublished) {
            mediaCard!!.unpublish()
        }
        mediaCard = null
    }

    private val playbackPendingIntent: PendingIntent? = PendingIntent.getActivity(
        service,
        System.currentTimeMillis().toInt(),
        Intent(service, MediaPlaybackActivity::class.java),
        0
    )

    private fun buildViews(state: MediaStateData): RemoteViews {
        val title = state.title
        val artist = state.artist
        val source = if (state.sourceApp != null) state.sourceApp else state.sourcePackage

        val heading = title ?: (source ?: service.getString(R.string.media_no_playback))
        val subheading = if (!artist.isNullOrEmpty()) artist else source
        val views = RemoteViews(service.packageName, R.layout.layout_media_card)
        views.setTextViewText(R.id.media_app, source ?: "")
        views.setTextViewText(R.id.media_title, heading)
        views.setTextViewText(R.id.media_artist, subheading ?: "")
        views.setTextViewText(R.id.media_state, getPlaybackLabel(state))
        views.setTextViewText(R.id.media_footer, source ?: "")

        val artwork = state.artwork
        val artworkBytes = artwork?.bytes
        if (artworkBytes != null && artworkBytes.isNotEmpty()) {
            val bitmap = BitmapFactory.decodeByteArray(artworkBytes, 0, artworkBytes.size)
            if (bitmap != null) {
                views.setImageViewBitmap(R.id.media_artwork, bitmap)
            } else {
                views.setImageViewResource(R.id.media_artwork, R.drawable.ic_music_50)
            }
        } else {
            views.setImageViewResource(R.id.media_artwork, R.drawable.ic_music_50)
        }
        return views
    }

    private fun getPlaybackLabel(state: MediaStateData): String = when (state.playbackState) {
        PlaybackStateValue.Playing -> service.getString(R.string.media_state_playing)
        PlaybackStateValue.Paused -> service.getString(R.string.media_state_paused)
        PlaybackStateValue.Buffering -> service.getString(R.string.media_state_buffering)
        PlaybackStateValue.Stopped -> service.getString(R.string.media_state_stopped)
        PlaybackStateValue.None -> service.getString(R.string.media_state_none)
    }

    private fun hasPlayableSession(state: MediaStateData): Boolean = state.playbackState != PlaybackStateValue.None

    companion object {
        private const val LIVE_CARD_TAG = "MediaPlayback"
    }
}

