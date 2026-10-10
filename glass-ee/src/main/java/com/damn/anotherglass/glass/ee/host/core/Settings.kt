package com.damn.anotherglass.glass.ee.host.core

import android.content.Context
import androidx.core.content.edit
import com.damn.anotherglass.glass.ee.host.R

class Settings(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var tiltToWake: Boolean
        get() = sharedPreferences.getBoolean(KEY_TILT_TO_WAKE, false)
        set(value) = sharedPreferences.edit() { putBoolean(KEY_TILT_TO_WAKE, value) }

    var tiltAngleThreshold: Int
        get() = sharedPreferences.getInt(KEY_TILT_ANGLE_THRESHOLD, DEFAULT_TILT_ANGLE_THRESHOLD)
        set(value) = sharedPreferences.edit() { putInt(KEY_TILT_ANGLE_THRESHOLD, value) }

    var lastScannedServerIp: String?
        get() = sharedPreferences.getString(KEY_LAST_SCANNED_SERVER_IP, null)
        set(value) = sharedPreferences.edit() { putString(KEY_LAST_SCANNED_SERVER_IP, value) }

    companion object {
        const val DEFAULT_TILT_ANGLE_THRESHOLD = 25
        const val KEY_TILT_TO_WAKE = "tilt_to_wake"
        const val KEY_TILT_ANGLE_THRESHOLD = "tilt_angle_threshold"
        private const val KEY_LAST_SCANNED_SERVER_IP = "last_scanned_server_ip"
        const val PREFS_NAME = "settings"
    }
}

enum class TiltSensitivity(
    val thresholdDegrees: Int,
    @param:androidx.annotation.StringRes val titleRes: Int,
    @param:androidx.annotation.StringRes val descriptionRes: Int
) {
    HIGH(15, R.string.tilt_sensitivity_high_title, R.string.tilt_sensitivity_high_desc),
    MEDIUM(25, R.string.tilt_sensitivity_medium_title, R.string.tilt_sensitivity_medium_desc),
    LOW(35, R.string.tilt_sensitivity_low_title, R.string.tilt_sensitivity_low_desc);

    companion object {
        fun fromAngle(angle: Int): TiltSensitivity =
            entries.firstOrNull { it.thresholdDegrees == angle } ?: MEDIUM

        fun next(current: TiltSensitivity): TiltSensitivity =
            when (current) {
                MEDIUM -> HIGH
                HIGH -> LOW
                LOW -> MEDIUM
            }
    }
}