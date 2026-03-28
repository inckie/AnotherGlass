package com.damn.anotherglass.shared.media

import java.io.Serializable

data class MediaCommandData(
    val command: Command,
    val seekToMs: Long = 0L,
) : Serializable {
    enum class Command {
        Play,
        Pause,
        TogglePlayPause,
        Next,
        Previous,
        SeekTo,
    }
}

