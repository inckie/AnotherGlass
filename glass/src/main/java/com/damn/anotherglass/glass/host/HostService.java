package com.damn.anotherglass.glass.host;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.damn.anotherglass.glass.host.bluetooth.BluetoothClient;
import com.damn.anotherglass.shared.rpc.IRPCClient;
import com.damn.anotherglass.glass.host.gps.MockGPS;
import com.damn.anotherglass.glass.host.notifications.NotificationsCardController;
import com.damn.anotherglass.glass.host.ui.ICardViewProvider;
import com.damn.anotherglass.glass.host.ui.MapCard;
import com.damn.anotherglass.glass.host.wifi.WiFiActivity;
import com.damn.anotherglass.shared.rpc.RPCMessage;
import com.damn.anotherglass.shared.rpc.RPCMessageListener;
import com.damn.anotherglass.shared.gps.GPSServiceAPI;
import com.damn.anotherglass.shared.gps.Location;
import com.damn.anotherglass.shared.notifications.NotificationData;
import com.damn.anotherglass.shared.notifications.NotificationsAPI;
import com.damn.anotherglass.shared.wifi.WiFiAPI;
import com.damn.anotherglass.shared.wifi.WiFiConfiguration;
import com.google.android.glass.media.Sounds;
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

    private IRPCClient mRPCClient;

    private ICardViewProvider mCardProvider;

    private NotificationsCardController mNotificationsCardController;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

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

            mNotificationsCardController = new NotificationsCardController(this);

            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            mRPCClient = new BluetoothClient();
            mRPCClient.start(this, new RPCMessageListener() {

                @Override
                public void onWaiting() {
                    displayStatusCard(getString(R.string.msg_waiting_for_connection));
                }

                @Override
                public void onConnectionStarted(@NonNull String device) {
                    //noinspection ConstantConditions
                    audio.playSoundEffect(Sounds.SUCCESS);
                    // map can take a while or not show at all, so show status card
                    displayStatusCard(getString(R.string.msg_connected_to_s, device));
                    mCardProvider = new MapCard(mLiveCard, HostService.this);
                }

                @Override
                public void onDataReceived(@NonNull RPCMessage data) {
                    route(data);
                }

                @Override
                public void onConnectionLost(@Nullable String error) {
                    //noinspection ConstantConditions
                    audio.playSoundEffect(Sounds.ERROR);
                    Toast.makeText(
                            HostService.this,
                            null != error ? error : getString(R.string.msg_disconnected),
                            Toast.LENGTH_LONG).show();
                    stopSelf(); // do not restart for now
                }

                @Override
                public void onShutdown() {
                    // already stopped in onConnectionLost
                }
            });
        } else {
            mLiveCard.navigate();
        }
        return START_STICKY;
    }

    private void route(@NonNull RPCMessage data) {
        // can use instanceof instead of .type, but for future sub-routing strings are more convenient
        if (GPSServiceAPI.ID.equals(data.service)) {
            if (data.type.equals(Location.class.getName()))
                mGPS.publish((Location) data.payload);
        } else if (NotificationsAPI.ID.equals(data.service)) {
            if (data.type.equals(NotificationData.class.getName())) {
                mNotificationsCardController.onNotificationUpdate((NotificationData) data.payload);
            }
        } else if (WiFiAPI.ID.equals(data.service)) {
            if (data.type.equals(WiFiConfiguration.class.getName()))
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
        mLiveCard.setViews(new CardBuilder(getApplicationContext(), CardBuilder.Layout.MENU)
                .setText(status)
                .getRemoteViews());
    }

    @Override
    public void onDestroy() {
        mRPCClient.stop();
        mNotificationsCardController.remove();
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
