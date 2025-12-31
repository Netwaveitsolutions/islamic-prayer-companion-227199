package org.example.app.qibla

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Qibla bearing calculator.
 *
 * Uses the great-circle initial bearing from a given (lat, lon) to the Kaaba.
 */
object QiblaCalculator {

    private const val KAABA_LAT = 21.4225
    private const val KAABA_LON = 39.8262

    /**
     * PUBLIC_INTERFACE
     * Returns the bearing (0..360 degrees) from the provided coordinate to the Kaaba.
     *
     * @param fromLat Latitude in degrees.
     * @param fromLon Longitude in degrees.
     */
    fun bearingToKaabaDegrees(fromLat: Double, fromLon: Double): Double {
        val lat1 = Math.toRadians(fromLat)
        val lon1 = Math.toRadians(fromLon)
        val lat2 = Math.toRadians(KAABA_LAT)
        val lon2 = Math.toRadians(KAABA_LON)

        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        var brng = Math.toDegrees(atan2(y, x))
        brng = (brng + 360.0) % 360.0
        return brng
    }
}
