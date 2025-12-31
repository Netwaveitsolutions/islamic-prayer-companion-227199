package org.example.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import kotlinx.coroutines.launch
import org.example.app.R
import org.example.app.data.prefs.AppPreferences
import org.example.app.prayer.notifications.NotificationPermissionHelper
import org.example.app.prayer.notifications.NotificationRescheduler
import org.example.app.prayer.notifications.PrayerNotificationScheduler

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var prefs: AppPreferences

    private val scheduler by lazy { PrayerNotificationScheduler(requireContext()) }

    private val REQUEST_POST_NOTIFICATIONS = 9001

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

        val notificationsEnabledPref = findPreference<SwitchPreferenceCompat>(AppPreferences.PREF_NOTIFICATIONS_ENABLED)
        notificationsEnabledPref?.setOnPreferenceChangeListener { pref, newValue ->
            val enabled = newValue as? Boolean ?: false
            if (!enabled) {
                scheduler.cancelAll()
                updateNextScheduledSummary()
                return@setOnPreferenceChangeListener true
            }

            // Enabling: on Android 13+ request permission if needed.
            if (!NotificationPermissionHelper.canPostNotifications(requireContext())) {
                // Optimistically keep switch off until user grants permission.
                (pref as? SwitchPreferenceCompat)?.isChecked = false
                NotificationPermissionHelper.requestPostNotifications(this, REQUEST_POST_NOTIFICATIONS)
                return@setOnPreferenceChangeListener false
            }

            // We have permission; schedule immediately (best effort).
            scheduleFromCurrentSettings(reason = "settings_toggle_on")
            true
        }

        // Notify-before change should reschedule immediately if enabled.
        val notifyBeforePref = findPreference<ListPreference>(AppPreferences.PREF_NOTIFY_BEFORE)
        notifyBeforePref?.setOnPreferenceChangeListener { _, _ ->
            scheduleFromCurrentSettings(reason = "notify_before_changed")
            true
        }

        // Calculation settings changes affect prayer times => reschedule.
        findPreference<ListPreference>(AppPreferences.PREF_CALC_METHOD)?.setOnPreferenceChangeListener { _, _ ->
            scheduleFromCurrentSettings(reason = "calc_method_changed")
            true
        }
        findPreference<ListPreference>(AppPreferences.PREF_MADHAB)?.setOnPreferenceChangeListener { _, _ ->
            scheduleFromCurrentSettings(reason = "madhab_changed")
            true
        }
        findPreference<ListPreference>(AppPreferences.PREF_HIGH_LAT_RULE)?.setOnPreferenceChangeListener { _, _ ->
            scheduleFromCurrentSettings(reason = "high_lat_rule_changed")
            true
        }

        // Exact alarms management (best effort).
        val exactPref = findPreference<Preference>("pref_notifications_exact_alarm")
        exactPref?.setOnPreferenceClickListener {
            openExactAlarmSettings()
            true
        }

        // Initial UI state.
        updateNextScheduledSummary()
    }

    override fun onResume() {
        super.onResume()
        // Update city summary if user changed it.
        val cityPref = findPreference<Preference>("pref_city_picker")
        cityPref?.summary = "${prefs.getSelectedCity().name}, ${prefs.getSelectedCity().country}"

        updateNextScheduledSummary()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != REQUEST_POST_NOTIFICATIONS) return

        val enabledPref = findPreference<SwitchPreferenceCompat>(AppPreferences.PREF_NOTIFICATIONS_ENABLED) ?: return
        if (NotificationPermissionHelper.canPostNotifications(requireContext())) {
            enabledPref.isChecked = true
            scheduleFromCurrentSettings(reason = "post_notifications_granted")
        } else {
            enabledPref.isChecked = false
            Toast.makeText(requireContext(), "Notifications permission not granted.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateNextScheduledSummary() {
        val nextPref = findPreference<Preference>("pref_notifications_next_scheduled") ?: return
        val next = scheduler.getNextScheduledEpochMillis()
        nextPref.summary = PrayerNotificationScheduler.formatNextScheduledLabel(next)
    }

    private fun scheduleFromCurrentSettings(reason: String) {
        if (!prefs.notificationsEnabled()) {
            scheduler.cancelAll()
            updateNextScheduledSummary()
            return
        }
        // Fire and forget; receiver/rescheduler does IO safely.
        // (Settings is UI thread; don't block.)
        viewLifecycleOwner.lifecycleScope.launch {
            NotificationRescheduler.rescheduleNow(requireContext().applicationContext, reason = reason)
            updateNextScheduledSummary()
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Dedicated exact-alarm permission screen (best effort).
            runCatching {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                return
            }
        }

        // Fallback: app details settings
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }
}
