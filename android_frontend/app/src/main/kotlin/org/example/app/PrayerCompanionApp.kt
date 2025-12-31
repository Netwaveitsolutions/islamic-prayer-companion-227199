package org.example.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import org.example.app.data.prefs.AppPreferences
import org.example.app.prayer.notifications.NotificationChannels

class PrayerCompanionApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val prefs = AppPreferences(this)
        when (prefs.getThemeMode()) {
            AppPreferences.ThemeMode.SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            AppPreferences.ThemeMode.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            AppPreferences.ThemeMode.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        NotificationChannels.ensureCreated(this)
    }
}
