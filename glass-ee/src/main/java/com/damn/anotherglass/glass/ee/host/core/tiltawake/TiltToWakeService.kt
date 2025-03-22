package com.damn.anotherglass.glass.ee.host.core.tiltawake

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

class TiltToWakeService : Service() {

    private var tiltToWake: TiltToWake? = null

    override fun onCreate() {
        super.onCreate()
        tiltToWake = TiltToWake(this)
        tiltToWake?.start()
        Log.i(TAG, "Service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        tiltToWake?.stop()
        Log.i(TAG, "Service destroyed")
    }

    private val binder = Binder()

    override fun onBind(intent: Intent): IBinder = binder

    companion object {
        private const val TAG = "TiltToWakeService"

        fun startService(context: Context) {
            val intent = Intent(context, TiltToWakeService::class.java)
            context.startService(intent)
        }
        fun stopService(context: Context) {
            val intent = Intent(context, TiltToWakeService::class.java)
            context.stopService(intent)
        }
    }
}