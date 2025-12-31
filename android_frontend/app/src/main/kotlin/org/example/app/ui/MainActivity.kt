package org.example.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.example.app.R
import org.example.app.data.prefs.AppPreferences
import org.example.app.ui.home.HomeFragment
import org.example.app.ui.learn.LearnFragment
import org.example.app.ui.profile.ProfileFragment
import org.example.app.ui.settings.SettingsFragment
import org.example.app.ui.onboarding.OnboardingActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = AppPreferences(this)

        // Gate: show onboarding once on first launch.
        if (!prefs.isOnboardingComplete()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        // Default tab.
        if (savedInstanceState == null) {
            switchTo(HomeFragment(), "home")
            bottomNav.selectedItemId = R.id.nav_home
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchTo(HomeFragment(), "home")
                R.id.nav_learn -> switchTo(LearnFragment(), "learn")
                R.id.nav_settings -> switchTo(SettingsFragment(), "settings")
                R.id.nav_profile -> switchTo(ProfileFragment(), "profile")
                else -> false
            }
        }
    }

    private fun switchTo(fragment: Fragment, tag: String): Boolean {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .commit()
        return true
    }
}
