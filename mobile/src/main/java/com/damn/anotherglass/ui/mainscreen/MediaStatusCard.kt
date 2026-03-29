package com.damn.anotherglass.ui.mainscreen

import android.media.session.PlaybackState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.damn.anotherglass.R
import com.damn.anotherglass.shared.media.MediaCommandData
import com.damn.anotherglass.shared.media.MediaStateData
import com.damn.anotherglass.ui.theme.AnotherGlassTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

@Composable
internal fun MediaStatusCard(
    mediaState: MediaStateData,
    serviceController: IServiceController?,
) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = "Media", style = MaterialTheme.typography.titleSmall)

            val hasMedia = mediaState.playbackState != MediaStateData.PlaybackStateValue.None

            if (!hasMedia) {
                Text(text = "No media playing")
            } else {
                Text(text = mediaState.sourceApp ?: mediaState.sourcePackage ?: "")
                Text(
                    text = mediaState.title ?: "Unknown track",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!mediaState.artist.isNullOrBlank()) {
                    Text(
                        text = mediaState.artist ?: "",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "${formatDuration(mediaState.positionMs)} / ${formatDuration(mediaState.durationMs)}",
                    style = MaterialTheme.typography.bodySmall
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        enabled = mediaState.actionsMask.hasMediaAction(PlaybackState.ACTION_SKIP_TO_PREVIOUS),
                        onClick = {
                            serviceController?.sendMediaCommand(MediaCommandData(MediaCommandData.Command.Previous))
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_fast_rewind_24),
                            contentDescription = "Previous"
                        )
                    }

                    val isPlaying = mediaState.playbackState == MediaStateData.PlaybackStateValue.Playing
                    IconButton(
                        enabled = mediaState.actionsMask.hasMediaAction(PlaybackState.ACTION_PLAY_PAUSE) ||
                            mediaState.actionsMask.hasMediaAction(PlaybackState.ACTION_PLAY) ||
                            mediaState.actionsMask.hasMediaAction(PlaybackState.ACTION_PAUSE),
                        onClick = {
                            serviceController?.sendMediaCommand(MediaCommandData(MediaCommandData.Command.TogglePlayPause))
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isPlaying) R.drawable.ic_pause_24 else R.drawable.ic_play_arrow_24
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play"
                        )
                    }

                    IconButton(
                        enabled = mediaState.actionsMask.hasMediaAction(PlaybackState.ACTION_SKIP_TO_NEXT),
                        onClick = {
                            serviceController?.sendMediaCommand(MediaCommandData(MediaCommandData.Command.Next))
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_fast_forward_24),
                            contentDescription = "Next"
                        )
                    }
                }
            }
        }
    }
}

internal fun Long.hasMediaAction(action: Long): Boolean = this and action == action

internal fun formatDuration(valueMs: Long): String {
    if (valueMs <= 0L) return "00:00"
    val totalSeconds = valueMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

@Preview(showBackground = true)
@Composable
private fun MediaStatusCardPreviewPlaying() {
    AnotherGlassTheme {
        MediaStatusCard(
            mediaState = mockMediaState(),
            serviceController = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaStatusCardPreviewNoMedia() {
    AnotherGlassTheme {
        MediaStatusCard(
            mediaState = MediaStateData(),
            serviceController = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaStatusCardPreviewIServiceController() {
    AnotherGlassTheme {
        MediaStatusCard(
            mediaState = mockMediaState(),
            serviceController = object : IServiceController {
                override val connectedDevice = MutableStateFlow(null)
                override val mediaState: StateFlow<MediaStateData?> = MutableStateFlow(null)
                override fun startService() = Unit
                override fun stopService() = Unit
                override fun sendMediaCommand(command: MediaCommandData) = Unit
                override fun send(message: com.damn.anotherglass.shared.rpc.RPCMessage) = Unit
            },
        )
    }
}

fun mockMediaState(): MediaStateData = MediaStateData(
    playbackState = MediaStateData.PlaybackStateValue.Playing,
    title = "Preview song",
    artist = "Preview artist",
    sourceApp = "Preview app",
    positionMs = 12_000,
    durationMs = 240_000,
    actionsMask = PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_SKIP_TO_PREVIOUS or
            PlaybackState.ACTION_SKIP_TO_NEXT,
)
