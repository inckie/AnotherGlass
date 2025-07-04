package com.damn.anotherglass.glass.ee.host.core

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.damn.anotherglass.shared.gps.GPSServiceAPI
import com.damn.anotherglass.shared.gps.Location
import com.damn.anotherglass.shared.notifications.NotificationData
import com.damn.anotherglass.shared.notifications.NotificationsAPI
import com.damn.anotherglass.shared.rpc.RPCMessage
import com.damn.anotherglass.shared.rpc.RPCMessageListener
import com.damn.glass.shared.gps.MockGPS
import org.greenrobot.eventbus.EventBus


interface IService {

    enum class ServiceState {
        INITIALIZING,
        WAITING,
        CONNECTED,
        DISCONNECTED
    }

    val state: ServiceState // todo: global LiveData?
}

class HostService : Service(), IService {

    // todo: move to proper place (maybe global in Application class?)
    private lateinit var sounds: SoundController
    private lateinit var notificationNotifier: NotificationNotifier
    private lateinit var gps: MockGPS

    private var client: WiFiClient? = null

    inner class LocalBinder : Binder() {
        fun getService(): IService = this@HostService
    }

    private val _binder = LocalBinder()

    override var state: IService.ServiceState = IService.ServiceState.INITIALIZING
        private set(value) {
            field = value
            EventBus.getDefault().post(value)
        }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // do not allow restart for now
        if(null == client) {
            client = WiFiClient(intent?.getStringExtra(EXTRA_IP))
            start()
        }
        return START_STICKY
    }

    @Override
    override fun onCreate() {
        super.onCreate()
        gps = MockGPS(this)
        sounds = SoundController(this)
        notificationNotifier = NotificationNotifier(sounds)
    }

    @Override
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "HostService stopped")
        gps.remove()
        client?.stop()
        sounds.release()
    }

    private fun start() {
        Log.i(TAG, "HostService started")

        val listener: RPCMessageListener = object : RPCMessageListener {
            override fun onWaiting() {
                Log.d(TAG, "Waiting")
                state = IService.ServiceState.WAITING
            }

            override fun onConnectionStarted(device: String) {
                Log.d(TAG, "Connected to $device")
                state = IService.ServiceState.CONNECTED
                NotificationController.instance.onServiceConnected()
            }

            override fun onDataReceived(data: RPCMessage) {
                when (data.service) {
                    GPSServiceAPI.ID -> {
                        Log.d(TAG, "GPS data received")
                        if (data.type.equals(Location::class.java.name))
                            gps.publish(data.payload as Location)
                    }

                    NotificationsAPI.ID -> {
                        Log.d(TAG, "Notification data received")
                        val notificationData = data.payload as NotificationData
                        NotificationController.instance.onNotificationUpdate(notificationData)
                        notificationNotifier.notify(notificationData)
                    }

                    else -> Log.e(TAG, "Unknown service: ${data.service}")
                }
            }

            override fun onConnectionLost(error: String?) {
                Log.e(TAG, "onConnectionLost: $error")
                state = IService.ServiceState.DISCONNECTED
                sounds.playSound(SoundController.SoundEffect.ConnectionLost)
            }

            override fun onShutdown() {
                Log.d(TAG, "onShutdown")
                stopSelf()
            }
        }
        try {
            gps.start()
        } catch (e: SecurityException) {
            Log.e(TAG, "GPS mocking is not enabled: $e")
            Toast.makeText(
                this,
                "GPS provider not available, please enable location mocking using ADB or developer settings",
                Toast.LENGTH_LONG
            ).show()
        }
        client?.start(this, listener)
    }

    override fun onBind(intent: Intent?): IBinder? = _binder

    companion object {
        private const val TAG = "HostService"

        private const val EXTRA_IP = "ip"

        fun startService(context: Context, ip: String? = null) {
            Intent(context, HostService::class.java).apply {
                ip?.let { putExtra(EXTRA_IP, ip) }
            }.also {
                context.startService(it)
            }
        }
    }
}
