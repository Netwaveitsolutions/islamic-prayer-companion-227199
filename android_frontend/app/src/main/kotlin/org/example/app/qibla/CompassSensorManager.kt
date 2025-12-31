package org.example.app.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

class CompassSensorManager(
    context: Context,
    private val listener: Listener
) : SensorEventListener {

    interface Listener {
        fun onAzimuthDegrees(azimuth: Double, accuracy: Int)
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationVector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private var lastAzimuth: Double? = null

    private val accelReading = FloatArray(3)
    private val magnetReading = FloatArray(3)

    private var hasAccel = false
    private var hasMagnet = false

    fun start() {
        // Prefer rotation vector for best real-world behavior.
        if (rotationVector != null) {
            sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_GAME)
        } else {
            if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            if (magnetometer != null) sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        hasAccel = false
        hasMagnet = false
        lastAzimuth = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                // orientation[0] is azimuth in radians.
                val azimuth = Math.toDegrees(orientation[0].toDouble()).let { (it + 360.0) % 360.0 }
                listener.onAzimuthDegrees(smooth(azimuth), event.accuracy)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelReading, 0, accelReading.size)
                hasAccel = true
                computeFallback(event.accuracy)
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetReading, 0, magnetReading.size)
                hasMagnet = true
                computeFallback(event.accuracy)
            }
        }
    }

    private fun computeFallback(accuracy: Int) {
        if (!hasAccel || !hasMagnet) return
        val rotationMatrix = FloatArray(9)
        val ok = SensorManager.getRotationMatrix(rotationMatrix, null, accelReading, magnetReading)
        if (!ok) return
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)
        val azimuth = Math.toDegrees(orientation[0].toDouble()).let { (it + 360.0) % 360.0 }
        listener.onAzimuthDegrees(smooth(azimuth), accuracy)
    }

    private fun smooth(newAzimuth: Double): Double {
        // Exponential smoothing on the circular angle to reduce jitter but keep responsiveness.
        val prev = lastAzimuth
        if (prev == null) {
            lastAzimuth = newAzimuth
            return newAzimuth
        }

        // Make smallest signed difference (-180..180).
        var diff = newAzimuth - prev
        while (diff > 180) diff -= 360.0
        while (diff < -180) diff += 360.0

        // More smoothing if tiny changes, less smoothing if quick movement.
        val alpha = if (abs(diff) < 3.0) 0.10 else 0.25

        var result = prev + alpha * diff
        result = (result + 360.0) % 360.0
        lastAzimuth = result
        return result
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // handled via onSensorChanged where available
    }
}
