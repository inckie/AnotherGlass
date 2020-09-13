package com.damn.anotherglass.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Switch
import com.damn.anotherglass.R
import com.damn.anotherglass.core.GlassService
import com.damn.anotherglass.core.GlassService.LocalBinder
import com.damn.anotherglass.core.Settings
import com.damn.anotherglass.extensions.notifications.NotificationService
import com.damn.anotherglass.shared.RPCMessage
import com.damn.anotherglass.shared.wifi.WiFiAPI
import com.damn.anotherglass.shared.wifi.WiFiConfiguration

class MainActivity : AppCompatActivity() {

    private lateinit var mSettings: Settings
    private val mConnection = GlassServiceConnection()
    private var mSwService: Switch? = null
    private var mCntControls: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSettings = Settings(this)
        mCntControls = findViewById(R.id.cnt_controls)

        // Start/Stop
        mSwService = findViewById<Switch>(R.id.toggle_service).apply {
            isChecked = GlassService.isRunning(context)
            setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
                if (isChecked == GlassService.isRunning(context)) return@OnCheckedChangeListener
                if (isChecked) start() else stop()
                // Tiny hack to avoid real checks and subscriptions to service lifecycle.
                // We rely on a fact that, if service will start,
                // we will be able to bind and then receive onServiceDisconnected
                // even if it will stop right away due to missing BT connection or something else.
                this.post { this.isChecked = GlassService.isRunning(context) }
            })
        }

        // GPS
        findViewById<Switch>(R.id.toggle_gps).apply {
            isChecked = mSettings.isGPSEnabled
            setOnCheckedChangeListener { _, isChecked -> mSettings.isGPSEnabled = isChecked }
        }

        // Notifications
        findViewById<Switch>(R.id.toggle_notifications).apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                visibility = View.GONE
            } else {
                isChecked = mSettings.isNotificationsEnabled && NotificationService.isEnabled(context)
                val changeListener = object : CompoundButton.OnCheckedChangeListener {
                    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                        if (toggleNotifications(isChecked)) return
                        this@apply.setOnCheckedChangeListener(null)
                        this@apply.isChecked = !isChecked
                        this@apply.setOnCheckedChangeListener(this)
                    }
                }
                setOnCheckedChangeListener(changeListener)
            }
        }

        //WiFi
        findViewById<View>(R.id.btn_connect_wifi).setOnClickListener { connectWiFi() }
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

    override fun onResume() {
        super.onResume()
        updateUI()
        if (GlassService.isRunning(this)) mConnection.bindGlassService()
    }

    override fun onPause() {
        super.onPause()
        mConnection.unbindGlassService()
    }

    private fun updateUI() {
        val running = GlassService.isRunning(this)
        mCntControls!!.visibility = if (running) View.VISIBLE else View.GONE
    }

    private fun start() {
        // don't bother and always require all permissions
        if (!hasGeoPermission()) {
            ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION), 0)
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

    private fun connectToWiFi(ssid: CharSequence,
                              pass: CharSequence) {
        if (TextUtils.isEmpty(ssid)) return
        // pass can be empty
        val service = mConnection.service ?: return
        service.send(RPCMessage(
                WiFiAPI.ID,
                WiFiConfiguration(ssid.toString(), pass.toString())))
    }

    private fun hasGeoPermission(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) &&
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!hasGeoPermission()) return
        startService(Intent(this@MainActivity, GlassService::class.java))
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun askEnableNotificationService() {
        AlertDialog.Builder(this)
                .setTitle(R.string.msg_notification_listener_service_title)
                .setMessage(R.string.notification_listener_service_message)
                .setPositiveButton(android.R.string.yes) { _, _ -> startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                .setNegativeButton(android.R.string.no, null)
                .show()
    }

    private inner class GlassServiceConnection : ServiceConnection {
        var service: GlassService? = null
            private set
        private var mBound = false
        fun bindGlassService() {
            try {
                if (mBound) {
                    unbindService(mConnection)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mBound = bindService(Intent(this@MainActivity, GlassService::class.java), mConnection, 0)
        }

        fun unbindGlassService() {
            if (!mBound) return
            mBound = false
            service = null
            unbindService(mConnection)
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            this.service = (service as LocalBinder).service
            updateUI()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            if (mBound) {
                // service stopped on its own, unbind from it (it won't restart on its own)
                mSwService!!.isChecked = false
                unbindGlassService()
                updateUI()
            }
        }
    }
}