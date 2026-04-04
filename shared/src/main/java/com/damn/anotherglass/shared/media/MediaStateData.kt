package com.damn.anotherglass.shared.media

import com.damn.anotherglass.shared.BinaryData
import java.io.Serializable

data class MediaStateData(
    val playbackState: PlaybackStateValue = PlaybackStateValue.None,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val artwork: BinaryData? = null,
    val sourceApp: String? = null,
    val sourcePackage: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val actionsMask: Long = 0L,
    val lastUpdatedMs: Long = 0L,
) : Serializable {
    enum class PlaybackStateValue {
        None,
        Playing,
        Paused,
        Stopped,
        Buffering,
    }
}


