package com.damn.anotherglass.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.damn.anotherglass.R
import com.damn.anotherglass.core.GlassService
import com.damn.anotherglass.core.GlassService.LocalBinder
import com.damn.anotherglass.core.Settings
import com.damn.anotherglass.databinding.ActivityMainBinding
import com.damn.anotherglass.extensions.GPSExtension
import com.damn.anotherglass.extensions.notifications.NotificationService
import com.damn.anotherglass.logging.LogActivity
import com.damn.anotherglass.shared.RPCMessage
import com.damn.anotherglass.shared.wifi.WiFiAPI
import com.damn.anotherglass.shared.wifi.WiFiConfiguration
import com.damn.anotherglass.utility.hasPermission


class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mSettings: Settings
    private val mConnection = GlassServiceConnection()

    private val gpsPermissionLauncher = createGPSPermissionLauncher {
        if (it) mSettings.isGPSEnabled = true
        else mBinding.toggleGps.isChecked = false
    }

    private val servicePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it.values.all { it }) start()
        else mBinding.toggleService.isChecked = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mSettings = Settings(this)

        // Start/Stop
        mBinding.toggleService.apply {
            isChecked = GlassService.isRunning(context)
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked == GlassService.isRunning(context)) return@setOnCheckedChangeListener
                if (isChecked) start() else stop()
                // Tiny hack to avoid real checks and subscriptions to service lifecycle.
                // We rely on a fact that, if service will start,
                // we will be able to bind and then receive onServiceDisconnected
                // even if it will stop right away due to missing BT connection or something else.
                this.post { this.isChecked = GlassService.isRunning(context) }
            }
        }

        // GPS
        mBinding.toggleGps.apply {
            isChecked = mSettings.isGPSEnabled
            setOnCheckedChangeListener { _, isChecked -> toggleGPS(isChecked) }
        }

        // Notifications
        mBinding.toggleNotifications.apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                visibility = View.GONE
            } else {
                isChecked =
                    mSettings.isNotificationsEnabled && NotificationService.isEnabled(context)
                val changeListener = object : CompoundButton.OnCheckedChangeListener {
                    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                        if (toggleNotifications(isChecked)) return
                        buttonView.setOnCheckedChangeListener(null)
                        buttonView.isChecked = !isChecked
                        buttonView.setOnCheckedChangeListener(this)
                    }
                }
                setOnCheckedChangeListener(changeListener)
            }
        }

        // WiFi
        mBinding.btnConnectWifi.setOnClickListener { connectWiFi() }
        updateUI()
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun toggleNotifications(enabled: Boolean): Boolean {
        if (enabled && !NotificationService.isEnabled(this)) {
            askEnableNotificationService()
            return false
        }
        mSettings.isNotificationsEnabled = enabled
        return true
    }

    private fun toggleGPS(isChecked: Boolean) {
        if (isChecked && !GPSExtension.hasGeoPermission(this)) {
            gpsPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        mSettings.isGPSEnabled = isChecked
    }

    override fun onResume() {
        super.onResume()
        mSettings.registerListener(this)
        updateUI() // will hide UI until we bind
        if (GlassService.isRunning(this)) mConnection.bindGlassService()
    }

    override fun onPause() {
        super.onPause()
        mSettings.unregisterListener(this)
        mConnection.unbindGlassService()
    }

    private fun updateUI() {
        val isRunning = GlassService.isRunning(this)
        if (isRunning != mBinding.toggleService.isChecked) {
            mBinding.toggleService.isChecked = isRunning // only update if different in order to avoid callback
        }
        if (isRunning) {
            mBinding.toggleGps.isChecked = mSettings.isGPSEnabled
        }
        val isConnected = null != mConnection.service
        mBinding.cntControls.visibility = if (isConnected) View.VISIBLE else View.GONE
    }

    private fun start() {

        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
            ) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPermission(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty()) {
            servicePermissionLauncher.launch(permissions.toTypedArray())
            mBinding.toggleService.isChecked = false
            return
        }

        if (!GlassService.isRunning(this)) {
            startService(Intent(this@MainActivity, GlassService::class.java))
        }
        mConnection.bindGlassService()
    }

    private fun stop() {
        stopService(Intent(this@MainActivity, GlassService::class.java))
    }

    private fun connectWiFi() {
        val view = layoutInflater.inflate(R.layout.view_wifi_dialog, null)
        val ssid = view.findViewById<EditText>(R.id.ed_ssid)
        val pass = view.findViewById<EditText>(R.id.ed_password)
        AlertDialog.Builder(this)
            .setPositiveButton(android.R.string.ok) { _, _ -> connectToWiFi(ssid.text, pass.text) }
            .setNegativeButton(android.R.string.cancel, null)
            .setView(view)
            .show()
    }

    private fun connectToWiFi(
        ssid: CharSequence,
        pass: CharSequence
    ) {
        if (TextUtils.isEmpty(ssid)) return
        // pass can be empty
        mConnection.service?.send(
            RPCMessage(WiFiAPI.ID, WiFiConfiguration(ssid.toString(), pass.toString()))
        )
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun askEnableNotificationService() {
        AlertDialog.Builder(this)
            .setTitle(R.string.msg_notification_listener_service_title)
            .setMessage(R.string.notification_listener_service_message)
            .setPositiveButton(android.R.string.ok) { _, _ -> startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private inner class GlassServiceConnection : ServiceConnection {
        var service: GlassService? = null
            private set
        private var bound = false
        fun bindGlassService() {
            try {
                if (bound) {
                    unbindService(mConnection)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            bound = bindService(Intent(this@MainActivity, GlassService::class.java), mConnection, 0)
        }

        fun unbindGlassService() {
            if (!bound) return
            bound = false
            service = null
            unbindService(mConnection)
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            this.service = (service as LocalBinder).service
            updateUI()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            mBinding.toggleService.isChecked = false
            unbindGlassService()
            updateUI()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_xray -> startActivity(Intent(this, LogActivity::class.java))
            else -> return super.onContextItemSelected(item)
        }
        return true
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            Settings.GPS_ENABLED -> mBinding.toggleGps.isChecked = mSettings.isGPSEnabled
        }
    }
}