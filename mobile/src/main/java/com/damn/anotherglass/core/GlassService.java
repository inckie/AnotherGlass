package com.damn.anotherglass.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;

import com.applicaster.xray.core.Logger;
import com.damn.anotherglass.R;
import com.damn.anotherglass.extensions.GPSExtension;
import com.damn.anotherglass.extensions.notifications.NotificationExtension;
import com.damn.anotherglass.logging.ALog;
import com.damn.anotherglass.shared.rpc.IRPCHost;
import com.damn.anotherglass.shared.rpc.RPCMessage;
import com.damn.anotherglass.shared.rpc.RPCMessageListener;
import com.damn.anotherglass.ui.MainActivity;
import com.damn.anotherglass.utility.ContextExKt;

public class GlassService
        extends LifecycleService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int NOTIFICATION_ID = 10101;

    public static final String sCHANNEL_DEFAULT = "CHANNEL_DEFAULT";

    private static final String CMD_NAME = "CMD_NAME";
    private static final String CMD_STOP = "CMD_STOP";
    private static final String TAG = "GlassService";

    private final IBinder mBinder = new LocalBinder();

    private IRPCHost mHost;

    private NotificationManager mNM;

    private Settings mSettings;

    private final ALog log = new ALog(Logger.get(TAG));

    // Extensions
    // todo: generalize
    private GPSExtension mGPS;

    private NotificationExtension mNotifications;

    public GlassService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createChannels();
        startForeground(NOTIFICATION_ID, buildNotification());

        // todo: update notification and UI on changes
        RPCMessageListener rpcMessageListener = new RPCMessageListener() {
            @Override
            public void onWaiting() {
                log.i(TAG, "Waiting for connection");
                Toast.makeText(GlassService.this, "Waiting for connection", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onConnectionStarted(@NonNull String device) {
                log.i(TAG, "Connected to " + device);
                Toast.makeText(GlassService.this, "Connected to " + device, Toast.LENGTH_SHORT).show();
                if (mSettings.isGPSEnabled())
                    mGPS.start();
                if (mSettings.isNotificationsEnabled())
                    mNotifications.start();
            }

            @Override
            public void onDataReceived(@NonNull RPCMessage data) {
                log.d(TAG, "Received " + data);
            }

            @Override
            public void onConnectionLost(@Nullable String error) {
                if (null != error)
                    log.e(TAG, "Disconnected with error: " + error);
                else
                    log.i(TAG, "Disconnected");
                Toast.makeText(GlassService.this, "Disconnected", Toast.LENGTH_SHORT).show();
                mGPS.stop();
                mNotifications.stop();
            }

            @Override
            public void onShutdown() {
                log.i(TAG, "BluetoothHost has stopped, terminating GlassService");
                Toast.makeText(GlassService.this, "BluetoothHost has stopped, terminating GlassService", Toast.LENGTH_SHORT).show();
                stopSelf();
            }
        };

        mSettings = new Settings(this);
        mNotifications = new NotificationExtension(this);
        mGPS = new GPSExtension(this);

        final boolean useWifi = Settings.HostMode.WiFi == mSettings.getHostMode();
        mHost = useWifi ? new WiFiHost(rpcMessageListener) : new BluetoothHost(rpcMessageListener);

        mSettings.registerListener(this, this.getLifecycle());

        mHost.start(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        // from docs: if there are not any pending start commands to be delivered to the service,
        // it will be called with a null intent object, so you must take care to check for this.
        if(null != intent) {
            String cmd = intent.getStringExtra(CMD_NAME);
            if (null != cmd) {
                if (cmd.equals(CMD_STOP))
                    stopSelf();
                return Service.START_NOT_STICKY;
            }
        }
        // todo: receive and route messages to Glass
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        mGPS.stop();
        mNotifications.stop();
        mHost.stop();
        super.onDestroy();
    }

    public void send(@NonNull RPCMessage message) {
        mHost.send(message);
    }

    public Settings getSettings() {
        return mSettings;
    }

    private void createChannels() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O)
            return;
        NotificationChannel defaultChannel = new NotificationChannel(
                sCHANNEL_DEFAULT,
                getString(R.string.notification_channel_state),
                NotificationManager.IMPORTANCE_DEFAULT);
        defaultChannel.setShowBadge(false);
        defaultChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        mNM.createNotificationChannel(defaultChannel);
    }

    private Notification buildNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, sCHANNEL_DEFAULT)
                .setOngoing(true)
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?  PendingIntent.FLAG_MUTABLE : 0;

        PendingIntent contentIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | flag);
        builder.setContentIntent(contentIntent);

        PendingIntent stopIntent = PendingIntent.getService(
                this,
                R.string.btn_stop,
                new Intent(this, GlassService.class).putExtra(CMD_NAME, CMD_STOP),
                flag);
        builder.addAction(R.drawable.ic_baseline_stop_24, getString(R.string.btn_stop), stopIntent);
        return builder.build();
    }

    public class LocalBinder extends Binder {
        public GlassService getService() {
            return GlassService.this;
        }
    }

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        super.onBind(intent);
        return mBinder;
    }

    public static boolean isRunning(Context context) {
        return ContextExKt.isServiceRunning(context, GlassService.class);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(Settings.GPS_ENABLED.equals(key)) {
            if(mSettings.isGPSEnabled())
                mGPS.start(); // todo: check if any device is connected
            else
                mGPS.stop();
        }
        else if(Settings.NOTIFICATIONS_ENABLED.equals(key)) {
            if(mSettings.isNotificationsEnabled())
                mNotifications.start(); // todo: check if any device is connected
            else
                mNotifications.stop();
        }
    }
}
