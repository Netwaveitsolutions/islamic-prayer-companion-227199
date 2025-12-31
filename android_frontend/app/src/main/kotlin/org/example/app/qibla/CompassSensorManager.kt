package org.example.app.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Provides a stabilized compass heading (azimuth) in degrees.
 *
 * Implementation notes:
 * - Prefers TYPE_ROTATION_VECTOR (sensor fusion from the system).
 * - Falls back to accelerometer + magnetometer (manual fusion) if needed.
 * - Applies angle-aware exponential smoothing and rate-limits UI updates to reduce jitter.
 * - Detects likely magnetic interference based on abnormal field magnitude and low accuracy.
 */
class CompassSensorManager(
    context: Context,
    private val listener: Listener
) : SensorEventListener {

    interface Listener {
        /**
         * Called with a stabilized azimuth (0..360) and the latest sensor accuracy.
         */
        fun onAzimuthDegrees(azimuth: Double, accuracy: Int)
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationVector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // Latest raw sensor readings for fallback path
    private val accelReading = FloatArray(3)
    private val magnetReading = FloatArray(3)
    private var hasAccel = false
    private var hasMagnet = false

    // Filtering + cadence control
    private var smoothedAzimuth: Double? = null
    private var lastUiDispatchMs: Long = 0L

    // Diagnostic signal for calibration UX
    private var lastMagFieldStrengthUt: Double? = null

    /**
     * Latest estimated magnetic interference state.
     * True typically indicates: strong magnetic field, or system reported low/unreliable accuracy.
     */
    var isMagneticInterferenceLikely: Boolean = false
        private set

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
        smoothedAzimuth = null
        lastUiDispatchMs = 0L
        lastMagFieldStrengthUt = null
        isMagneticInterferenceLikely = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val azimuth = azimuthFromRotationVector(event.values)
                // Rotation vector does not directly provide magnetic field magnitude; still respect accuracy.
                updateInterferenceSignals(accuracy = event.accuracy, magStrengthUt = null)
                dispatchFiltered(azimuth, event.accuracy)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelReading, 0, accelReading.size)
                hasAccel = true
                computeFallback(accuracy = event.accuracy)
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetReading, 0, magnetReading.size)
                hasMagnet = true
                val strength = vectorMagnitude(event.values)
                lastMagFieldStrengthUt = strength
                computeFallback(accuracy = event.accuracy)
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
        val azimuth = Math.toDegrees(orientation[0].toDouble()).let { normalize360(it) }

        updateInterferenceSignals(accuracy = accuracy, magStrengthUt = lastMagFieldStrengthUt)
        dispatchFiltered(azimuth, accuracy)
    }

    private fun dispatchFiltered(rawAzimuth: Double, accuracy: Int) {
        // 1) Smooth raw heading (angle-aware) to reduce jitter.
        val filtered = smoothAngle(rawAzimuth)

        // 2) Rate limit UI updates to reduce redraw spam (keeps animation smooth but stable).
        val now = SystemClock.elapsedRealtime()
        if (now - lastUiDispatchMs < UI_MIN_UPDATE_INTERVAL_MS) return
        lastUiDispatchMs = now

        listener.onAzimuthDegrees(filtered, accuracy)
    }

    private fun azimuthFromRotationVector(values: FloatArray): Double {
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotationMatrix, orientation)
        // orientation[0] is azimuth in radians.
        return Math.toDegrees(orientation[0].toDouble()).let { normalize360(it) }
    }

    private fun smoothAngle(newAzimuth: Double): Double {
        val prev = smoothedAzimuth
        if (prev == null) {
            smoothedAzimuth = newAzimuth
            return newAzimuth
        }

        // Use shortest signed difference for circular data (-180..180).
        val diff = shortestSignedAngleDelta(prev, newAzimuth)

        // Adaptive smoothing: stronger smoothing when nearly stable, less smoothing during turns.
        val alpha = when {
            abs(diff) < 1.5 -> 0.08
            abs(diff) < 6.0 -> 0.18
            else -> 0.32
        }

        val result = normalize360(prev + alpha * diff)
        smoothedAzimuth = result
        return result
    }

    /**
     * Heuristic magnetic interference detection:
     * - Earth's magnetic field magnitude is commonly ~25–65 µT depending on location.
     * - Values far outside this range are likely near magnets/metal, cases, speakers, etc.
     * - Also treat SENSOR_STATUS_UNRELIABLE as likely interference.
     */
    private fun updateInterferenceSignals(accuracy: Int, magStrengthUt: Double?) {
        val unreliable = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE

        // Only evaluate magnitude when we have it (fallback path includes magnetometer events).
        val magnitudeSuspicious = magStrengthUt?.let { it < 15.0 || it > 100.0 } ?: false

        isMagneticInterferenceLikely = unreliable || magnitudeSuspicious
    }

    private fun vectorMagnitude(v: FloatArray): Double {
        val x = v.getOrNull(0)?.toDouble() ?: 0.0
        val y = v.getOrNull(1)?.toDouble() ?: 0.0
        val z = v.getOrNull(2)?.toDouble() ?: 0.0
        return sqrt(x * x + y * y + z * z)
    }

    private fun normalize360(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }

    private fun shortestSignedAngleDelta(fromDeg: Double, toDeg: Double): Double {
        var delta = (toDeg - fromDeg) % 360.0
        if (delta > 180.0) delta -= 360.0
        if (delta < -180.0) delta += 360.0
        return delta
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // We intentionally compute calibration/interference UX in the consumer (Activity),
        // using the accuracy passed in onSensorChanged, where available.
    }

    private companion object {
        // ~16 FPS max for sensor-driven UI updates (good compromise between smooth and stable).
        private const val UI_MIN_UPDATE_INTERVAL_MS = 60L
    }
}
