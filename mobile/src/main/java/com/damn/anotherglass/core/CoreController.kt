package com.damn.anotherglass.core

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.damn.anotherglass.R
import com.damn.anotherglass.extensions.GPSExtension
import com.damn.anotherglass.extensions.notifications.NotificationService
import com.damn.anotherglass.ui.mainscreen.MainActivity
import com.damn.anotherglass.ui.mainscreen.SettingsController
import com.damn.anotherglass.ui.createGPSPermissionLauncher
import com.damn.anotherglass.utility.hasPermission
import kotlinx.coroutines.launch

class CoreController(private val activity: MainActivity) : SettingsController() {

    private var gpsPermissionLauncher: ActivityResultLauncher<String>
    private var servicePermissionLauncher: ActivityResultLauncher<Array<String>>

    private val mSettings: Settings = Settings(activity)

    private val _serviceState: MutableLiveData<Boolean> = MutableLiveData(GlassService.isRunning(activity))
    private var _hostMode = MutableLiveData(mSettings.hostMode)
    private var _isNotificationsEnabled = MutableLiveData(mSettings.isNotificationsEnabled && NotificationService.isEnabled(activity))
    private var _isGPSEnabled = MutableLiveData(mSettings.isGPSEnabled)

    override val isServiceRunning: LiveData<Boolean> = _serviceState
    override val hostMode: LiveData<Settings.HostMode> = _hostMode
    override val notificationsEnabled: LiveData<Boolean> = _isNotificationsEnabled
    override val isGPSEnabled: LiveData<Boolean> = _isGPSEnabled

    init {
        gpsPermissionLauncher = activity.createGPSPermissionLauncher {
            mSettings.isGPSEnabled = it // will trigger update of GPS extension
        }

        servicePermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            if (it.values.all { it }) activity.startService()
        }

        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                refresh()
            }
        }

        mSettings.registerListener({ _, key ->
            when (key) {
                Settings.GPS_ENABLED -> _isGPSEnabled.postValue(mSettings.isGPSEnabled)
                Settings.NOTIFICATIONS_ENABLED -> syncNotificationsState()
            }
        }, activity.lifecycle)
    }

    private fun refresh() {
        _serviceState.postValue(GlassService.isRunning(activity))
        syncNotificationsState()
    }

    private fun syncNotificationsState() {
        _isNotificationsEnabled.postValue(
            mSettings.isNotificationsEnabled
                    && NotificationService.isEnabled(activity)
        )
    }

    private fun askEnableNotificationService() {
        AlertDialog.Builder(activity)
            .setTitle(R.string.msg_notification_listener_service_title)
            .setMessage(R.string.notification_listener_service_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                activity.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun checkServicePermissions(): Boolean {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !activity.hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        ) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !activity.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissions.isNotEmpty()) {
            servicePermissionLauncher.launch(permissions.toTypedArray())
            return false
        }
        return true
    }

    override fun setGPSEnabled(enabled: Boolean) {
        if (enabled && !GPSExtension.hasGeoPermission(activity)) {
            gpsPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        mSettings.isGPSEnabled = enabled
    }

    override fun setNotificationsEnabled(enabled: Boolean) {
        if (enabled && !NotificationService.isEnabled(activity)) {
            askEnableNotificationService()
            return
        }
        mSettings.isNotificationsEnabled = enabled
    }

    override fun setServiceRunning(checked: Boolean) {
        when {
            checked -> if (checkServicePermissions()) activity.startService()
            else -> activity.stopService()
        }
    }

    override fun setHostMode(mode: Settings.HostMode) {
        mSettings.hostMode = mode
        _hostMode.postValue(mode)
    }

    fun onServiceDisconnected() {
        _serviceState.postValue(false)
    }

    fun onServiceConnected() {
        _serviceState.postValue(true)
    }
}