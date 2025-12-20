package com.damn.anotherglass.ui.mainscreen

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.damn.anotherglass.core.ConnectedDevice
import com.damn.anotherglass.core.CoreController
import com.damn.anotherglass.core.GlassService
import com.damn.anotherglass.core.GlassService.LocalBinder
import com.damn.anotherglass.shared.rpc.RPCMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ServiceController(private val activity: ComponentActivity) : DefaultLifecycleObserver, IServiceController {

    private val glassServiceConnection = GlassServiceConnection()
    var controller: CoreController

    private val _connectedDevice = MutableStateFlow<ConnectedDevice?>(null)
    override val connectedDevice: StateFlow<ConnectedDevice?> = _connectedDevice

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

    override fun startService() {
        if (!GlassService.isRunning(activity)) {
            activity.startService(Intent(activity, GlassService::class.java))
        }
        glassServiceConnection.bindGlassService()
    }

    override fun stopService() {
        activity.stopService(Intent(activity, GlassService::class.java))
    }

    override fun send(message: RPCMessage) {
        glassServiceConnection.service?.send(message)
    }

    override fun getService() = glassServiceConnection.service

    private inner class GlassServiceConnection : ServiceConnection {
        var service: GlassService? = null
            private set
        private var bound = false
        private var deviceStateJob: Job? = null

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
            cleanupConnection()
            activity.unbindService(glassServiceConnection)
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            this.service = (service as LocalBinder).service
            controller.onServiceConnected()
            deviceStateJob = activity.lifecycleScope.launch {
                this@ServiceController.glassServiceConnection.service?.connectedDevice?.collect {
                    _connectedDevice.value = it
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            cleanupConnection()
            // service framework will not call unbind on its own
            if(bound) {
                bound = false
                activity.unbindService(glassServiceConnection)
            }
        }

        private fun cleanupConnection() {
            deviceStateJob?.cancel()
            deviceStateJob = null
            _connectedDevice.value = null
            service = null
            controller.onServiceDisconnected()
        }
    }

}