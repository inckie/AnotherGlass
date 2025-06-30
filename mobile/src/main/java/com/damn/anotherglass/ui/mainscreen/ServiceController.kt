package com.damn.anotherglass.ui.mainscreen

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.damn.anotherglass.core.CoreController
import com.damn.anotherglass.core.GlassService
import com.damn.anotherglass.core.GlassService.LocalBinder

class ServiceController(private val activity: ComponentActivity) : DefaultLifecycleObserver {

    private val glassServiceConnection = GlassServiceConnection()
    var controller: CoreController

    init {
        activity.lifecycle.addObserver(this)
        controller = CoreController(activity, this)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        if (GlassService.isRunning(activity))
            glassServiceConnection.bindGlassService()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        glassServiceConnection.unbindGlassService()
    }

    fun startService() {
        if (!GlassService.isRunning(activity)) {
            activity.startService(Intent(activity, GlassService::class.java))
        }
        glassServiceConnection.bindGlassService()
    }

    fun stopService() {
        activity.stopService(Intent(activity, GlassService::class.java))
    }

    fun getService() = glassServiceConnection.service

    private inner class GlassServiceConnection : ServiceConnection {
        var service: GlassService? = null
            private set
        private var bound = false
        fun bindGlassService() {
            try {
                if (bound) {
                    activity.unbindService(glassServiceConnection)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            bound = activity.bindService(
                Intent(activity, GlassService::class.java),
                glassServiceConnection,
                0
            )
        }

        fun unbindGlassService() {
            if (!bound) return
            bound = false
            service = null
            activity.unbindService(glassServiceConnection)
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            this.service = (service as LocalBinder).service
            controller.onServiceConnected()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            controller.onServiceDisconnected()
            unbindGlassService()
        }
    }

}