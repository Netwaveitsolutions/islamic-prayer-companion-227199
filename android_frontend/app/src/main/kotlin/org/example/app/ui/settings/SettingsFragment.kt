package org.example.app.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import org.example.app.R
import org.example.app.data.prefs.AppPreferences
import org.example.app.prayer.notifications.PrayerNotificationScheduler

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var prefs: AppPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        prefs = AppPreferences(requireContext())

        val cityPref = findPreference<Preference>("pref_city_picker")
        cityPref?.summary = "${prefs.getSelectedCity().name}, ${prefs.getSelectedCity().country}"
        cityPref?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), CityPickerActivity::class.java))
            true
        }

        val themePref = findPreference<ListPreference>(AppPreferences.PREF_THEME_MODE)
        themePref?.setOnPreferenceChangeListener { _, newValue ->
            when (newValue?.toString()) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            true
        }

        val notificationsPref = findPreference<SwitchPreferenceCompat>(AppPreferences.PREF_NOTIFICATIONS_ENABLED)
        notificationsPref?.setOnPreferenceChangeListener { _, _ ->
            // Re-schedule with new setting on next Home refresh, and attempt immediate schedule update too.
            val scheduler = PrayerNotificationScheduler(requireContext())
            scheduler.cancelAll()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        // Update city summary if user changed it.
        val cityPref = findPreference<Preference>("pref_city_picker")
        cityPref?.summary = "${prefs.getSelectedCity().name}, ${prefs.getSelectedCity().country}"
    }
}
