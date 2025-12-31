package org.example.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import org.example.app.R
import org.example.app.data.prefs.AppPreferences
import org.example.app.ui.MainActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = AppPreferences(this)

        setContentView(R.layout.activity_onboarding)

        findViewById<MaterialButton>(R.id.cta).setOnClickListener {
            prefs.setOnboardingComplete(true)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
