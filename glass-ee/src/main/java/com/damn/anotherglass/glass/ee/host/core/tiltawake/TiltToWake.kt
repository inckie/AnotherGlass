package com.damn.anotherglass.glass.ee.host.core.tiltawake

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.util.Log
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt


class TiltToWake(context: Context): SensorEventListener {

    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val sensorManager = context.getSystemService(SensorManager::class.java)

    private var tiltTimer = 0L
    private var lastUpdate = 0L
    private var lastWakeTime = 0L

    fun start() {
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    @Suppress("DEPRECATION")
    private fun awake() {
        // Wake up, Neo…
        Log.i(TAG, "Awake!")
        lastWakeTime = System.currentTimeMillis()
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE, "AnotherGlass:WakeLock"
        )
        wakeLock.acquire(1000)
        wakeLock.release()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        @Suppress("DEPRECATION")
        if(powerManager.isScreenOn) {
            resetTimer()
            return
        }

        if(lastWakeTime + COOLDOWN_TIME > System.currentTimeMillis()) {
            // should be awake already
            return
        }

        event?.let {
            val magnitudeSq = it.values[0] * it.values[0] + it.values[1] * it.values[1] + it.values[2] * it.values[2]
            // Only works on Earth for now
            if(abs(SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH - magnitudeSq) > MOTION_THRESHOLD) {
                // some movement is happening
                resetTimer()
                return
            }

            val norm = sqrt(magnitudeSq)
            val yzNorm = sqrt(it.values[1] * it.values[1] + it.values[2] * it.values[2])

            // if is looking up, but not too tilted sideways
            if (abs(it.values[0] / norm) > X_THRESHOLD
                || it.values[1] / yzNorm < Y_POSITIVE_THRESHOLD) {
                resetTimer()
                return
            }
            if(0L != lastUpdate) {
                tiltTimer += System.currentTimeMillis() - lastUpdate
                if(tiltTimer > DELAY) {
                    resetTimer()
                    awake()
                    return
                }
            }
            lastUpdate = System.currentTimeMillis()
        }
    }

    private fun resetTimer() {
        tiltTimer = 0L
        lastUpdate = 0L
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val TAG = "TiltToWake"

        private const val MOTION_THRESHOLD = 2.0 // just magic number from real device

        private const val ANGLE_THRESHOLD = 15.0 / 180.0 * Math.PI
        private const val X_THRESHOLD = 0.36 // just magic number (~20 degree sideways tilt)
        private val Y_POSITIVE_THRESHOLD = sin(ANGLE_THRESHOLD)

        private const val DELAY = 300L
        private const val COOLDOWN_TIME = 2000L
    }
}