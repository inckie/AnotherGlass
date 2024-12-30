package com.damn.anotherglass.ui.mainscreen

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.damn.anotherglass.R
import com.damn.anotherglass.core.CoreController
import com.damn.anotherglass.core.GlassService
import com.damn.anotherglass.core.GlassService.LocalBinder
import com.damn.anotherglass.core.Settings
import com.damn.anotherglass.shared.rpc.RPCMessage
import com.damn.anotherglass.shared.wifi.WiFiAPI
import com.damn.anotherglass.shared.wifi.WiFiConfiguration
import com.damn.anotherglass.ui.theme.AnotherGlassTheme


class MainActivity : ComponentActivity() {

    private lateinit var controller: CoreController
    private val glassServiceConnection = GlassServiceConnection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = CoreController(this)
        //enableEdgeToEdge()
        setContent {
            AnotherGlassTheme {
                MainScreen(controller)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (GlassService.isRunning(this)) glassServiceConnection.bindGlassService()
    }

    override fun onPause() {
        super.onPause()
        glassServiceConnection.unbindGlassService()
    }

    fun startService() {
        if (!GlassService.isRunning(this)) {
            startService(Intent(this, GlassService::class.java))
        }
        glassServiceConnection.bindGlassService()
    }

    fun stopService() {
        stopService(Intent(this, GlassService::class.java))
    }

    private inner class GlassServiceConnection : ServiceConnection {
        var service: GlassService? = null
            private set
        private var bound = false
        fun bindGlassService() {
            try {
                if (bound) {
                    unbindService(glassServiceConnection)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            bound = bindService(Intent(this@MainActivity, GlassService::class.java), glassServiceConnection, 0)
        }

        fun unbindGlassService() {
            if (!bound) return
            bound = false
            service = null
            unbindService(glassServiceConnection)
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

    fun connectWiFi() {
        val view = layoutInflater.inflate(R.layout.view_wifi_dialog, null)
        val ssid = view.findViewById<EditText>(R.id.ed_ssid)
        val pass = view.findViewById<EditText>(R.id.ed_password)
        AlertDialog.Builder(this)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (TextUtils.isEmpty(ssid.text)) return@setPositiveButton
                // pass can be empty
                glassServiceConnection.service?.send(
                    RPCMessage(WiFiAPI.ID, WiFiConfiguration(ssid.toString(), pass.toString()))
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setView(view)
            .show()
    }

}

abstract class SettingsController {
    open val isServiceRunning: LiveData<Boolean> = MutableLiveData(true)
    open val hostMode: LiveData<Settings.HostMode> = MutableLiveData(Settings.HostMode.WiFi)
    open val notificationsEnabled: LiveData<Boolean> = MutableLiveData(true)
    open val isGPSEnabled: LiveData<Boolean> = MutableLiveData(true)

    abstract fun setServiceRunning(checked: Boolean)
    abstract fun setHostMode(mode: Settings.HostMode)
    abstract fun setGPSEnabled(enabled: Boolean)
    abstract fun setNotificationsEnabled(enabled: Boolean)
}
