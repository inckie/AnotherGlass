package com.damn.anotherglass.ui.mainscreen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.damn.anotherglass.core.Settings

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