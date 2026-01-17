package com.damn.anotherglass.glass.host.music;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.RemoteViews;

import com.damn.anotherglass.glass.host.HostService;
import com.damn.anotherglass.shared.music.MusicAPI;
import com.damn.anotherglass.shared.music.MusicControl;
import com.damn.anotherglass.shared.music.MusicData;
import com.damn.anotherglass.shared.rpc.IRPCClient;
import com.damn.anotherglass.shared.rpc.RPCMessage;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.widget.CardBuilder;

public class MusicCardController extends BroadcastReceiver {

    private static final String CARD_TAG = "MusicCard";
    private final HostService service;
    private final IRPCClient rpcClient;
    private LiveCard liveCard;
    private MusicData lastData;

    public MusicCardController(HostService service, IRPCClient rpcClient) {
        this.service = service;
        this.rpcClient = rpcClient;
        IntentFilter filter = new IntentFilter(MusicMenuActivity.ACTION_BROADCAST);
        service.registerReceiver(this, filter);
    }

    public void update(MusicData data) {
        this.lastData = data;
        if (liveCard == null) {
            liveCard = new LiveCard(service, CARD_TAG);
        }

        CardBuilder builder = new CardBuilder(service, CardBuilder.Layout.CAPTION);
        builder.setText(data.track != null ? data.track : "Unknown Track");
        builder.setFootnote(data.artist != null ? data.artist : "Unknown Artist");

        if (data.albumArt != null && data.albumArt.length > 0) {
            Bitmap art = BitmapFactory.decodeByteArray(data.albumArt, 0, data.albumArt.length);
            builder.addImage(art);
        }

        RemoteViews views = builder.getRemoteViews();
        liveCard.setViews(views);

        Intent menuIntent = new Intent(service, MusicMenuActivity.class);
        menuIntent.putExtra(MusicMenuActivity.EXTRA_IS_PLAYING, data.isPlaying);
        
        liveCard.setAction(PendingIntent.getActivity(service, 0, menuIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        if (!liveCard.isPublished()) {
            liveCard.publish(LiveCard.PublishMode.REVEAL);
        } else {
            // liveCard.navigate();
        }
    }

    public void remove() {
        try {
            service.unregisterReceiver(this);
        } catch (IllegalArgumentException e) {
            // Ignore if not registered
        }
        if (liveCard != null && liveCard.isPublished()) {
            liveCard.unpublish();
        }
        liveCard = null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (MusicMenuActivity.ACTION_BROADCAST.equals(intent.getAction())) {
            String action = intent.getStringExtra(MusicMenuActivity.EXTRA_ACTION);
            MusicControl control = null;
            if ("Play".equals(action)) control = MusicControl.Play;
            else if ("Pause".equals(action)) control = MusicControl.Pause;
            else if ("Next".equals(action)) control = MusicControl.Next;
            else if ("Previous".equals(action)) control = MusicControl.Previous;

            if (control != null) {
                rpcClient.send(new RPCMessage(MusicAPI.ID, control));
                // Optimistically update UI?
                if (lastData != null) {
                    if (control == MusicControl.Play) lastData.isPlaying = true;
                    if (control == MusicControl.Pause) lastData.isPlaying = false;
                    // For next/prev we can't guess.
                    update(lastData);
                }
            }
        }
    }
}
