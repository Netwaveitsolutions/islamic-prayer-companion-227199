package org.example.app.ui.qibla

import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.example.app.R
import org.example.app.data.prefs.AppPreferences
import org.example.app.qibla.CompassSensorManager
import org.example.app.qibla.QiblaCalculator
import org.example.app.qibla.ui.QiblaCompassView

class QiblaActivity : AppCompatActivity() {

    private var compassManager: CompassSensorManager? = null

    private lateinit var prefs: AppPreferences
    private lateinit var compassView: QiblaCompassView
    private lateinit var tvCalibration: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qibla)

        prefs = AppPreferences(this)

        compassView = findViewById(R.id.qiblaCompass)
        tvCalibration = findViewById(R.id.tvCalibration)

        // Manager is created once; it will be started/stopped in lifecycle.
        compassManager = CompassSensorManager(this, object : CompassSensorManager.Listener {
            override fun onAzimuthDegrees(azimuth: Double, accuracy: Int) {
                // UI update is already rate-limited in CompassSensorManager.
                compassView.setDeviceAzimuthDegrees(azimuth)

                // Calibration / interference UX
                renderCalibrationHint(accuracy = accuracy, interference = compassManager?.isMagneticInterferenceLikely == true)
            }
        })

        // Initial bearing using selected city coordinates.
        updateQiblaBearingFromSelectedCity()
    }

    override fun onResume() {
        super.onResume()
        // Ensure we recompute if user changed city in Settings.
        updateQiblaBearingFromSelectedCity()
        compassManager?.start()
    }

    override fun onPause() {
        super.onPause()
        compassManager?.stop()
    }

    private fun updateQiblaBearingFromSelectedCity() {
        val city = prefs.getSelectedCity()
        val qiblaBearing = QiblaCalculator.bearingToKaabaDegrees(city.latitude, city.longitude)
        compassView.setQiblaBearingDegrees(qiblaBearing)
    }

    private fun renderCalibrationHint(accuracy: Int, interference: Boolean) {
        val msg = when {
            interference -> {
                "Magnetic interference detected. Move away from metal/magnets, remove magnetic case, then wave your phone in a gentle figure‑8."
            }

            accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE -> {
                "Compass accuracy is low. Move your phone in a gentle figure‑8 to calibrate."
            }

            accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW -> {
                "Tip: For best accuracy, calibrate with a gentle figure‑8 and keep the phone flat."
            }

            else -> {
                // Hide extra hint when good.
                null
            }
        }

        if (msg == null) {
            tvCalibration.visibility = View.GONE
        } else {
            tvCalibration.text = msg
            tvCalibration.visibility = View.VISIBLE
        }
    }
}
