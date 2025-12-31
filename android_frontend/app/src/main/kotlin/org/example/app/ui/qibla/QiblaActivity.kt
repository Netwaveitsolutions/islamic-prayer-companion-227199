package org.example.app.ui.qibla

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.example.app.R
import org.example.app.data.prefs.AppPreferences
import org.example.app.qibla.CompassSensorManager
import org.example.app.qibla.QiblaCalculator
import org.example.app.qibla.ui.QiblaCompassView

class QiblaActivity : AppCompatActivity() {

    private var compassManager: CompassSensorManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qibla)

        val prefs = AppPreferences(this)
        val city = prefs.getSelectedCity()
        val qiblaBearing = QiblaCalculator.bearingToKaabaDegrees(city.latitude, city.longitude)

        val compassView = findViewById<QiblaCompassView>(R.id.qiblaCompass)
        compassView.setQiblaBearingDegrees(qiblaBearing)

        compassManager = CompassSensorManager(this, object : CompassSensorManager.Listener {
            override fun onAzimuthDegrees(azimuth: Double, accuracy: Int) {
                compassView.setDeviceAzimuthDegrees(azimuth)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        compassManager?.start()
    }

    override fun onPause() {
        super.onPause()
        compassManager?.stop()
    }
}
