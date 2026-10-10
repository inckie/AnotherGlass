package com.damn.anotherglass.glass.ee.host.core.tiltawake

import android.content.Context
import android.content.SharedPreferences
import android.os.PowerManager
import android.util.Log
import com.damn.anotherglass.glass.ee.host.core.Settings

class TiltToWake(context: Context) {

    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val prefs = context.getSharedPreferences(Settings.PREFS_NAME, Context.MODE_PRIVATE)

    private val detector = TiltDetector(
        context,
        prefs.getInt(Settings.KEY_TILT_ANGLE_THRESHOLD, Settings.DEFAULT_TILT_ANGLE_THRESHOLD)
    )

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == Settings.KEY_TILT_ANGLE_THRESHOLD) {
            val newThreshold = prefs.getInt(Settings.KEY_TILT_ANGLE_THRESHOLD, Settings.DEFAULT_TILT_ANGLE_THRESHOLD)
            detector.angleThreshold = newThreshold
            Log.i(TAG, "Updated tilt threshold to $newThreshold°")
        }
    }

    init {
        detector.onTiltTriggered = {
            if (!powerManager.isInteractive) {
                awake()
            }
        }
    }

    fun start() {
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
        detector.start()
        Log.i(TAG, "Tilt to wake started (threshold=${detector.angleThreshold}°)")
    }

    fun stop() {
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        detector.stop()
        Log.i(TAG, "Tilt to wake stopped")
    }

    @Suppress("DEPRECATION")
    private fun awake() {
        // Wake up, Neo…
        Log.i(TAG, "Waking up screen via Tilt to Wake!")
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "AnotherGlass:TiltToWake"
        )
        // Acquire wake lock with a 2.5s timeout. Do NOT immediately release,
        // so the system has adequate time to turn the display backlight on.
        wakeLock.acquire(2500L)
    }

    companion object {
        private const val TAG = "TiltToWake"
    }
}