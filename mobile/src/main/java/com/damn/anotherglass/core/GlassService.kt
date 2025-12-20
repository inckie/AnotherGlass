package com.damn.anotherglass.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.applicaster.xray.core.Logger
import com.damn.anotherglass.R
import com.damn.anotherglass.extensions.GPSExtension
import com.damn.anotherglass.extensions.notifications.NotificationExtension
import com.damn.anotherglass.logging.ALog
import com.damn.anotherglass.shared.device.BatteryStatusData
import com.damn.anotherglass.shared.device.DeviceAPI
import com.damn.anotherglass.shared.rpc.IRPCHost
import com.damn.anotherglass.shared.rpc.RPCMessage
import com.damn.anotherglass.shared.rpc.RPCMessageListener
import com.damn.anotherglass.ui.MainActivity
import com.damn.anotherglass.utility.isServiceRunning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GlassService
    : LifecycleService(), SharedPreferences.OnSharedPreferenceChangeListener {

    inner class LocalBinder : Binder() {
        val service: GlassService
            get() = this@GlassService
    }

    private val mBinder: IBinder = LocalBinder()

    private lateinit var mHost: IRPCHost
    private lateinit var mNM: NotificationManager
    private lateinit var mSettings: Settings

    private val log = ALog(Logger.get(TAG))

    // Extensions
    // todo: generalize
    private lateinit var mGPS: GPSExtension
    private lateinit var mNotifications: NotificationExtension

    // connected device info
    private val mDeviceName = MutableStateFlow("")
    private val mBatteryStatus = MutableStateFlow<BatteryStatusData?>(null)
    private val mConnectedDeviceData = ConnectedDevice(mDeviceName, mBatteryStatus)
    private val mConnectedDevice = MutableStateFlow<ConnectedDevice?>(null)

    override fun onCreate() {
        super.onCreate()
        mNM = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createChannels()
        startForeground(NOTIFICATION_ID, buildNotification())

        // todo: update notification and UI on changes
        val rpcMessageListener: RPCMessageListener = object : RPCMessageListener {
            override fun onWaiting() {
                log.i(TAG, "Waiting for connection")
                Toast.makeText(this@GlassService, "Waiting for connection", Toast.LENGTH_SHORT)
                    .show()
                mConnectedDevice.value = null
            }

            override fun onConnectionStarted(device: String) {
                log.i(TAG, "Connected to $device")
                Toast.makeText(this@GlassService, "Connected to $device", Toast.LENGTH_SHORT)
                    .show()
                mDeviceName.value = device
                mBatteryStatus.value = null
                mConnectedDevice.value = mConnectedDeviceData
                if (mSettings.isGPSEnabled) mGPS.start()
                if (mSettings.isNotificationsEnabled) mNotifications.start()
            }

            override fun onDataReceived(data: RPCMessage) {
                log.d(TAG, "Received $data")
                if (DeviceAPI.SERVICE_NAME == data.service) {
                    val payload = data.payload
                    if (payload is BatteryStatusData) {
                        mBatteryStatus.value = payload
                    }
                }
            }

            override fun onConnectionLost(error: String?) {
                if (null != error) log.e(TAG, "Disconnected with error: $error")
                else log.i(TAG, "Disconnected")
                Toast.makeText(this@GlassService, "Disconnected", Toast.LENGTH_SHORT).show()
                mGPS.stop()
                mNotifications.stop()
                mConnectedDevice.value = null
            }

            override fun onShutdown() {
                log.i(TAG, "BluetoothHost has stopped, terminating GlassService")
                Toast.makeText(
                    this@GlassService,
                    "BluetoothHost has stopped, terminating GlassService",
                    Toast.LENGTH_SHORT
                ).show()
                mConnectedDevice.value = null
                stopSelf()
            }
        }

        mSettings = Settings(this)
        mNotifications = NotificationExtension(this)
        mGPS = GPSExtension(this)

        val useWifi = Settings.HostMode.WiFi == mSettings.hostMode
        mHost = if (useWifi) WiFiHost(rpcMessageListener) else BluetoothHost(rpcMessageListener)

        mSettings.registerListener(this, this.lifecycle)

        mHost.start(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // from docs: if there are not any pending start commands to be delivered to the service,
        // it will be called with a null intent object, so you must take care to check for this.
        if (null != intent) {
            val cmd = intent.getStringExtra(CMD_NAME)
            if (null != cmd) {
                if (cmd == CMD_STOP) stopSelf()
                return START_NOT_STICKY
            }
        }
        // todo: receive and route messages to Glass
        return START_STICKY
    }

    override fun onDestroy() {
        mGPS.stop()
        mNotifications.stop()
        mHost.stop()
        mConnectedDevice.value = null
        super.onDestroy()
    }

    fun send(message: RPCMessage) {
        mHost.send(message)
    }

    val connectedDevice: StateFlow<ConnectedDevice?>
        get() = mConnectedDevice

    val settings: Settings
        get() = mSettings

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val defaultChannel = NotificationChannel(
            sCHANNEL_DEFAULT,
            getString(R.string.notification_channel_state),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        defaultChannel.setShowBadge(false)
        defaultChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        mNM.createNotificationChannel(defaultChannel)
    }

    private fun buildNotification(): Notification {
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, sCHANNEL_DEFAULT)
            .setOngoing(true)
            .setContentTitle(getString(R.string.app_name))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)

        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

        val contentIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or flag
        )
        builder.setContentIntent(contentIntent)

        val stopIntent = PendingIntent.getService(
            this,
            R.string.btn_stop,
            Intent(this, GlassService::class.java).putExtra(CMD_NAME, CMD_STOP),
            flag
        )
        builder.addAction(R.drawable.ic_baseline_stop_24, getString(R.string.btn_stop), stopIntent)
        return builder.build()
    }


    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return mBinder
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (Settings.GPS_ENABLED == key) {
            if (mSettings.isGPSEnabled)
                mGPS.start() // todo: check if any device is connected
            else
                mGPS.stop()
        } else if (Settings.NOTIFICATIONS_ENABLED == key) {
            if (mSettings.isNotificationsEnabled)
                mNotifications.start() // todo: check if any device is connected
            else
                mNotifications.stop()
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 10101

        const val sCHANNEL_DEFAULT: String = "CHANNEL_DEFAULT"

        private const val CMD_NAME = "CMD_NAME"
        private const val CMD_STOP = "CMD_STOP"
        private const val TAG = "GlassService"

        @JvmStatic
        fun isRunning(context: Context): Boolean = context.isServiceRunning(GlassService::class.java)
    }
}
