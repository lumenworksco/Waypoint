package com.florian.waypoint

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detects when the phone is in free fall (airborne) using the linear accelerometer.
 *
 * Linear acceleration returns acceleration without gravity, so when the phone is
 * truly in free fall, all three axes read close to zero.
 *
 * Calls [onAirTime] with the duration in milliseconds whenever an airborne event
 * of at least 500 ms is detected.
 */
class AirTimeDetector(
    context: Context,
    private val onAirTime: (durationMs: Long) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private var airStart: Long = 0
    private var inAir = false

    fun start() {
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        inAir = false
        airStart = 0
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()

        // Low magnitude (≈ 0) means all non-gravity forces have ceased → free fall
        if (magnitude < 2.5f) {
            if (!inAir) {
                inAir = true
                airStart = now
            }
        } else {
            if (inAir) {
                val duration = now - airStart
                // Only count airborne events of at least 0.5 s to filter out noise
                if (duration in 500..10000) {
                    onAirTime(duration)
                }
                inAir = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
