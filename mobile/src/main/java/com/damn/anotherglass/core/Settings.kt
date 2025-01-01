package com.damn.anotherglass.core

import android.content.Context
import android.content.SharedPreferences

class Settings(context: Context) {

    private val mPreferences = context.getSharedPreferences(sPreferencesName, Context.MODE_PRIVATE)

    enum class HostMode(val value: String) {
        Bluetooth("bluetooth"),
        WiFi("wifi")
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        mPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        mPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    var isGPSEnabled: Boolean
        get() = mPreferences.getBoolean(GPS_ENABLED, false)
        set(enabled) {
            mPreferences.edit().putBoolean(GPS_ENABLED, enabled).apply()
        }
    var isNotificationsEnabled: Boolean
        get() = mPreferences.getBoolean(NOTIFICATIONS_ENABLED, false)
        set(enabled) {
            mPreferences.edit().putBoolean(NOTIFICATIONS_ENABLED, enabled).apply()
        }
    var hostMode: HostMode
        get() = mPreferences.getString(HOST_MODE, HostMode.WiFi.value)?.let { mode ->
            HostMode.entries.firstOrNull { mode == it.value }
        } ?: HostMode.WiFi
        set(mode) {
            mPreferences.edit().putString(HOST_MODE, mode.value).apply()
        }

    companion object {
        private const val sPreferencesName = "anotherglass"
        const val GPS_ENABLED = "gps_enabled"
        const val NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val HOST_MODE = "host_mode"
    }
}
