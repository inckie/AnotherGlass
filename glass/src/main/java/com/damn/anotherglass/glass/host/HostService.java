package com.damn.anotherglass.glass.host;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.damn.anotherglass.glass.host.bluetooth.BluetoothHost;
import com.damn.anotherglass.glass.host.gps.MockGPS;
import com.damn.anotherglass.glass.host.ui.ICardViewProvider;
import com.damn.anotherglass.glass.host.ui.MapCard;
import com.damn.anotherglass.glass.host.wifi.WiFiActivity;
import com.damn.shared.gps.GPSServiceAPI;
import com.damn.shared.gps.Location;
import com.damn.shared.RPCMessage;
import com.damn.shared.wifi.WiFiAPI;
import com.damn.shared.wifi.WiFiConfiguration;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.glass.widget.CardBuilder;

/**
 * A {@link Service} that publishes a {@link LiveCard} in the timeline.
 */

public class HostService extends Service {

    private static final String LIVE_CARD_TAG = "HostService";

    private LiveCard mLiveCard;

    private MockGPS mGPS;

    private BluetoothHost mBt;

    private ICardViewProvider mCardProvider;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mLiveCard == null) {
            mLiveCard = new LiveCard(this, LIVE_CARD_TAG);

            RemoteViews remoteView = new CardBuilder(getApplicationContext(), CardBuilder.Layout.MENU)
                    .setText(R.string.title_updating)
                    .getRemoteViews();
            mLiveCard.setViews(remoteView);

            // Display the options menu when the live card is tapped.
            Intent menuIntent = new Intent(this, LiveCardMenuActivity.class);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
            mLiveCard.publish(PublishMode.REVEAL);

            mGPS = new MockGPS(this);

            mBt = new BluetoothHost(this) {

                @Override
                public void onWaiting() {
                    displayStatusCard(getString(R.string.msg_waiting_for_connection));
                }

                @Override
                public void onConnectionStarted(@NonNull String device) {
                    Toast.makeText(HostService.this, device, Toast.LENGTH_SHORT).show();
                    mCardProvider = new MapCard(mLiveCard, HostService.this);
                }

                @Override
                public void onDataReceived(@NonNull RPCMessage data) {
                    route(data);
                }

                @Override
                public void onConnectionLost(@Nullable String error) {
                    displayStatusCard(error);
                }
            };
            mBt.start();
        } else {
            mLiveCard.navigate();
        }
        return START_STICKY;
    }

    private void route(@NonNull RPCMessage data) {
        if(GPSServiceAPI.ID.equals(data.service)) {
            if(data.type.equals(Location.class.getName()))
                mGPS.publish((Location)data.payload);
        }
        else if(WiFiAPI.ID.equals(data.service)) {
            if(data.type.equals(WiFiConfiguration.class.getName()))
                WiFiActivity.start(this, (WiFiConfiguration) data.payload);
        }
    }

    private void displayStatusCard(String status) {
        if (mLiveCard == null || !mLiveCard.isPublished())
            return;
        if(null != mCardProvider) {
            mCardProvider.onRemoved();
            mCardProvider = null;
        }
        RemoteViews remoteView = new CardBuilder(getApplicationContext(), CardBuilder.Layout.MENU)
                .setText(status)
                .getRemoteViews();
        mLiveCard.setViews(remoteView);
    }

    @Override
    public void onDestroy() {
        mBt.stop();
        mGPS.remove();
        if(null != mCardProvider) {
            mCardProvider.onRemoved();
            mCardProvider = null;
        }
        if (mLiveCard != null && mLiveCard.isPublished()) {
            mLiveCard.unpublish();
            mLiveCard = null;
        }
        super.onDestroy();
    }
}
