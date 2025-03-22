package com.damn.anotherglass.glass.ee.host.core

import android.content.Context
import androidx.core.content.edit

class Settings(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var tiltToWake: Boolean
        get() = sharedPreferences.getBoolean(KEY_TILT_TO_WAKE, false)
        set(value) = sharedPreferences.edit() { putBoolean(KEY_TILT_TO_WAKE, value) }

    companion object {
        private const val KEY_TILT_TO_WAKE = "tilt_to_wake"
        private const val PREFS_NAME = "settings"
    }
}