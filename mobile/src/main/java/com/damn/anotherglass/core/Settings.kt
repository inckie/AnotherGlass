package com.damn.anotherglass.core

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

class Settings(context: Context) {

    private val preferences = context.getSharedPreferences(sPreferencesName, Context.MODE_PRIVATE)

    enum class HostMode(val value: String) {
        Bluetooth("bluetooth"),
        WiFi("wifi")
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun registerListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
        lifecycle: Lifecycle
    ) {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                preferences.registerOnSharedPreferenceChangeListener(listener)
            }

            override fun onPause(owner: LifecycleOwner) {
                super.onPause(owner)
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        })
    }

    var isGPSEnabled: Boolean
        get() = preferences.getBoolean(GPS_ENABLED, false)
        set(enabled) = preferences.edit { putBoolean(GPS_ENABLED, enabled) }

    var isNotificationsEnabled: Boolean
        get() = preferences.getBoolean(NOTIFICATIONS_ENABLED, false)
        set(enabled) = preferences.edit { putBoolean(NOTIFICATIONS_ENABLED, enabled) }

    var hostMode: HostMode
        get() = preferences.getString(HOST_MODE, HostMode.WiFi.value)?.let { mode ->
            HostMode.entries.firstOrNull { mode == it.value }
        } ?: HostMode.WiFi
        set(mode) = preferences.edit { putString(HOST_MODE, mode.value) }

    companion object {
        private const val sPreferencesName = "anotherglass"
        const val GPS_ENABLED = "gps_enabled"
        const val NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val HOST_MODE = "host_mode"
    }
}
