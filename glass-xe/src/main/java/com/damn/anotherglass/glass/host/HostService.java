package com.damn.anotherglass.glass.host;

import android.annotation.SuppressLint;
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
import com.damn.anotherglass.glass.host.media.MediaCardController;
import com.damn.anotherglass.shared.rpc.IRPCClient;
import com.damn.glass.shared.gps.MockGPS;
import com.damn.glass.shared.media.MediaController;
import com.damn.glass.shared.rpc.WiFiClient;
import com.damn.anotherglass.glass.host.notifications.NotificationsCardController;
import com.damn.anotherglass.glass.host.ui.ICardViewProvider;
import com.damn.anotherglass.glass.host.ui.MapCard;
import com.damn.anotherglass.glass.host.wifi.WiFiActivity;
import com.damn.anotherglass.shared.device.DeviceAPI;
import com.damn.anotherglass.shared.rpc.RPCMessage;
import com.damn.anotherglass.shared.rpc.RPCMessageListener;
import com.damn.anotherglass.shared.gps.GPSServiceAPI;
import com.damn.anotherglass.shared.gps.Location;
import com.damn.anotherglass.shared.media.MediaAPI;
import com.damn.anotherglass.shared.media.MediaStateData;
import com.damn.anotherglass.shared.notifications.NotificationData;
import com.damn.anotherglass.shared.notifications.NotificationsAPI;
import com.damn.anotherglass.shared.wifi.WiFiAPI;
import com.damn.anotherglass.shared.wifi.WiFiConfiguration;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.glass.widget.CardBuilder;
import com.damn.anotherglass.glass.host.core.BatteryStatus;

/**
 * A {@link Service} that publishes a {@link LiveCard} in the timeline.
 */

public class HostService extends Service {

    private static final String LIVE_CARD_TAG = "HostService";
    private static final String PREFS_NAME = "host_service";
    private static final String PREF_CONNECTION_TYPE = "connection_type";

    public static final String EXTRA_CONNECTION_TYPE = "connection_type";
    public static final String EXTRA_IP = "ip";

    public static final String CONNECTION_TYPE_BLUETOOTH = "bluetooth";
    public static final String CONNECTION_TYPE_WIFI = "wifi";

    public static final String DEFAULT_WIFI_IP = "192.168.1.180"; // kept for source compatibility, prefer gateway auto-detection

    private LiveCard mLiveCard;

    private MockGPS mGPS;

    private IRPCClient mRPCClient;

    private ICardViewProvider mCardProvider;

    private NotificationsCardController mNotificationsCardController;
    private MediaCardController mMediaCardController;

    private BatteryStatus mBatteryStatus;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    @SuppressLint("WrongConstant")
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

            try {
                mGPS.start();
            } catch (SecurityException e) {
                // Will not happen on Explorer Edition
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }

            mNotificationsCardController = new NotificationsCardController(this);
            mMediaCardController = new MediaCardController(this);

            mBatteryStatus = new BatteryStatus(this, data -> {
                if(null != mRPCClient) {
                    mRPCClient.send(new RPCMessage(DeviceAPI.SERVICE_NAME, data));
                }
            });
            mBatteryStatus.start();

            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            final String connectionType = resolveConnectionType(intent);
            final String wifiIp = intent != null ? intent.getStringExtra(EXTRA_IP) : null;
            mRPCClient = createClient(connectionType, wifiIp);
            MediaController.getInstance().setService(message -> {
                if (mRPCClient != null) {
                    mRPCClient.send(message);
                }
            });
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
                    mMediaCardController.onServiceConnected();
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
        } else if (MediaAPI.ID.equals(data.service)) {
            if (data.type.equals(MediaStateData.class.getName())) {
                MediaStateData state = (MediaStateData) data.payload;
                MediaController.getInstance().onMediaStateUpdate(state);
                if (mMediaCardController != null) {
                    mMediaCardController.onMediaStateUpdate(state);
                }
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

    @NonNull
    private IRPCClient createClient(@NonNull String connectionType, @Nullable String wifiIp) {
        if (CONNECTION_TYPE_WIFI.equals(connectionType)) {
            return new WiFiClient(wifiIp); // null → WiFiClient auto-detects gateway via ConnectionUtils
        }
        return new BluetoothClient();
    }

    @NonNull
    private String resolveConnectionType(@Nullable Intent intent) {
        String requestedType = intent != null ? intent.getStringExtra(EXTRA_CONNECTION_TYPE) : null;
        if (requestedType != null && isKnownConnectionType(requestedType)) {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(PREF_CONNECTION_TYPE, requestedType)
                    .apply();
            return requestedType;
        }

        String persistedType = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(PREF_CONNECTION_TYPE, CONNECTION_TYPE_BLUETOOTH);
        return isKnownConnectionType(persistedType) ? persistedType : CONNECTION_TYPE_BLUETOOTH;
    }

    private boolean isKnownConnectionType(@Nullable String type) {
        return CONNECTION_TYPE_BLUETOOTH.equals(type) || CONNECTION_TYPE_WIFI.equals(type);
    }

    @Override
    public void onDestroy() {
        if(null != mBatteryStatus) {
            mBatteryStatus.stop();
            mBatteryStatus = null;
        }
        if (mRPCClient != null) {
            mRPCClient.stop();
        }
        MediaController.getInstance().clearService();
        mNotificationsCardController.remove();
        if (mMediaCardController != null) {
            mMediaCardController.remove();
            mMediaCardController = null;
        }
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