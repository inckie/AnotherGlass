package com.damn.anotherglass.glass.ee.host.core.tiltawake

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.asin
import kotlin.math.sqrt

/**
 * Reusable accelerometer-based tilt detector for Glass EE.
 *
 * Glass EE hardware coordinates:
 * - X: horizontal sideways axis (roll, ear-to-shoulder)
 * - Y: forward line-of-sight axis along the temple (pitch, looking up/down)
 * - Z: vertical axis when wearing glasses level
 */
class TiltDetector(
    context: Context,
    var angleThreshold: Int = 25
) : SensorEventListener {

    private val sensorManager = context.getSystemService(SensorManager::class.java)

    var onTiltSample: ((pitchDegrees: Float, rollDegrees: Float, isTiltedUp: Boolean) -> Unit)? = null
    var onTiltTriggered: (() -> Unit)? = null

    private var holdDuration = 0L
    private var lastSampleTime = 0L
    private var cooldownUntil = 0L

    var isRunning = false
        private set

    fun start(): Boolean {
        if (isRunning) return true
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: run {
                Log.e(TAG, "No accelerometer sensor found on device")
                return false
            }

        val success = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        isRunning = success
        resetHoldTimer()
        cooldownUntil = 0L
        return success
    }

    fun stop() {
        if (!isRunning) return
        sensorManager.unregisterListener(this)
        isRunning = false
        resetHoldTimer()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val values = event?.values ?: return

        val x = values[0]
        val y = values[1]
        val z = values[2]

        val magnitude = sqrt(x * x + y * y + z * z)

        // Sanity filter: reject free-fall or violent shaking, while allowing natural head motion
        if (magnitude < MIN_MAGNITUDE || magnitude > MAX_MAGNITUDE) {
            resetHoldTimer()
            return
        }

        // Sideways roll angle (ear towards shoulder)
        val rollSin = (x / magnitude).coerceIn(-1.0f, 1.0f)
        val rollDegrees = Math.toDegrees(asin(rollSin.toDouble())).toFloat()

        // Forward pitch angle (tilting head up/down)
        val yzNorm = sqrt(y * y + z * z)
        if (yzNorm < 0.001f) {
            resetHoldTimer()
            return
        }
        val pitchSin = (y / yzNorm).coerceIn(-1.0f, 1.0f)
        val pitchDegrees = Math.toDegrees(asin(pitchSin.toDouble())).toFloat()

        // Excessive sideways roll invalidates looking-up gesture
        val rollAcceptable = kotlin.math.abs(rollDegrees) <= MAX_ROLL_DEGREES
        val isTiltedUp = rollAcceptable && pitchDegrees >= angleThreshold

        onTiltSample?.invoke(pitchDegrees, rollDegrees, isTiltedUp)

        val now = System.currentTimeMillis()
        if (now < cooldownUntil) {
            resetHoldTimer()
            return
        }

        if (isTiltedUp) {
            if (lastSampleTime != 0L) {
                holdDuration += (now - lastSampleTime)
                if (holdDuration >= DELAY_MS) {
                    resetHoldTimer()
                    cooldownUntil = now + COOLDOWN_MS
                    Log.d(TAG, "Tilt to wake triggered: pitch=${pitchDegrees.toInt()}° (threshold=$angleThreshold°)")
                    onTiltTriggered?.invoke()
                }
            }
            lastSampleTime = now
        } else {
            resetHoldTimer()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun resetHoldTimer() {
        holdDuration = 0L
        lastSampleTime = 0L
    }

    companion object {
        private const val TAG = "TiltDetector"

        private const val DELAY_MS = 250L
        private const val COOLDOWN_MS = 2000L
        private const val MAX_ROLL_DEGREES = 25.0f

        private const val MIN_MAGNITUDE = 5.0f
        private const val MAX_MAGNITUDE = 16.0f
    }
}
