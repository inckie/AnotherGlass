package com.damn.anotherglass.glass.host.media;

import android.app.PendingIntent;
import android.content.Intent;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.damn.anotherglass.glass.host.HostService;
import com.damn.anotherglass.glass.host.R;
import com.damn.anotherglass.shared.media.MediaStateData;
import com.damn.glass.shared.media.MediaController;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.widget.CardBuilder;

public class MediaCardController {

    private static final String LIVE_CARD_TAG = "MediaPlayback";

    private final HostService service;

    @Nullable
    private LiveCard mediaCard;

    public MediaCardController(@NonNull HostService service) {
        this.service = service;
    }

    public void onServiceConnected() {
        MediaController.getInstance().onServiceConnected();
        remove();
    }

    public void onMediaStateUpdate(@NonNull MediaStateData state) {
        if (!hasPlayableSession(state)) {
            remove();
            return;
        }

        if (mediaCard == null) {
            mediaCard = new LiveCard(service, LIVE_CARD_TAG);
        }

        mediaCard.setViews(buildViews(state));
        mediaCard.setAction(getPlaybackPendingIntent());

        if (!mediaCard.isPublished()) {
            mediaCard.publish(LiveCard.PublishMode.REVEAL);
        }
    }

    public void remove() {
        if (mediaCard == null) {
            return;
        }
        if (mediaCard.isPublished()) {
            mediaCard.unpublish();
        }
        mediaCard = null;
    }

    private PendingIntent getPlaybackPendingIntent() {
        Intent intent = new Intent(service, MediaPlaybackActivity.class);
        return PendingIntent.getActivity(service, (int) System.currentTimeMillis(), intent, 0);
    }

    private RemoteViews buildViews(@NonNull MediaStateData state) {
        String title = state.getTitle();
        String artist = state.getArtist();
        String source = state.getSourceApp() != null ? state.getSourceApp() : state.getSourcePackage();

        String heading = title != null ? title : (source != null ? source : service.getString(R.string.media_no_playback));
        String subheading = (artist != null && !artist.isEmpty()) ? artist : source;
        String timestamp = state.getPlaybackState().name();

        CardBuilder builder = new CardBuilder(service.getApplicationContext(), CardBuilder.Layout.AUTHOR)
                .setHeading(heading)
                .setTimestamp(timestamp);
        if (subheading != null) {
            builder.setSubheading(subheading);
        }
        if (source != null) {
            builder.setFootnote(source);
        }
        return builder.getRemoteViews();
    }

    private boolean hasPlayableSession(@NonNull MediaStateData state) {
        return state.getPlaybackState() != MediaStateData.PlaybackStateValue.None;
    }
}

