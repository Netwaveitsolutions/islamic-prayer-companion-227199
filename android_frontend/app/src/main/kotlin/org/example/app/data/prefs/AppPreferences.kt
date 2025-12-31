package org.example.app.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.example.app.domain.City
import org.example.app.domain.UserMode

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    fun isOnboardingComplete(): Boolean = prefs.getBoolean(KEY_ONBOARDING_DONE, false)

    fun setOnboardingComplete(done: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply()
    }

    fun getSelectedCity(): City {
        val name = prefs.getString(KEY_CITY_NAME, null)
        val country = prefs.getString(KEY_CITY_COUNTRY, null)
        val lat = prefs.getString(KEY_CITY_LAT, null)
        val lon = prefs.getString(KEY_CITY_LON, null)

        return if (name != null && country != null && lat != null && lon != null) {
            City(name = name, country = country, latitude = lat.toDouble(), longitude = lon.toDouble())
        } else {
            // Default city for first run.
            City(name = "Makkah", country = "Saudi Arabia", latitude = 21.3891, longitude = 39.8579)
        }
    }

    fun setSelectedCity(city: City) {
        prefs.edit()
            .putString(KEY_CITY_NAME, city.name)
            .putString(KEY_CITY_COUNTRY, city.country)
            .putString(KEY_CITY_LAT, city.latitude.toString())
            .putString(KEY_CITY_LON, city.longitude.toString())
            .apply()
    }

    fun getCalculationMethod(): Int {
        return prefs.getString(PREF_CALC_METHOD, "2")?.toIntOrNull() ?: 2
    }

    fun getMadhabSchool(): Int {
        // 0 = Shafi, 1 = Hanafi (Aladhan 'school')
        return prefs.getString(PREF_MADHAB, "0")?.toIntOrNull() ?: 0
    }

    fun notificationsEnabled(): Boolean = prefs.getBoolean(PREF_NOTIFICATIONS_ENABLED, false)

    fun notifyBeforeMinutes(): Int = prefs.getString(PREF_NOTIFY_BEFORE, "10")?.toIntOrNull() ?: 10

    enum class ThemeMode { SYSTEM, LIGHT, DARK }

    fun getThemeMode(): ThemeMode {
        return when (prefs.getString(PREF_THEME_MODE, "system")) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    fun getUserMode(): UserMode {
        return when (prefs.getString(KEY_USER_MODE, UserMode.GUEST.value)) {
            UserMode.GOOGLE.value -> UserMode.GOOGLE
            else -> UserMode.GUEST
        }
    }

    fun setUserMode(mode: UserMode) {
        prefs.edit().putString(KEY_USER_MODE, mode.value).apply()
    }

    fun getGoogleAccountId(): String? = prefs.getString(KEY_GOOGLE_ACCOUNT_ID, null)

    fun setGoogleAccountId(accountId: String?) {
        prefs.edit().putString(KEY_GOOGLE_ACCOUNT_ID, accountId).apply()
    }

    companion object {
        private const val KEY_ONBOARDING_DONE = "onboarding_done"

        private const val KEY_CITY_NAME = "city_name"
        private const val KEY_CITY_COUNTRY = "city_country"
        private const val KEY_CITY_LAT = "city_lat"
        private const val KEY_CITY_LON = "city_lon"

        private const val KEY_USER_MODE = "user_mode"
        private const val KEY_GOOGLE_ACCOUNT_ID = "google_account_id"

        // Preference screen keys (must match res/xml/preferences.xml)
        const val PREF_CALC_METHOD = "pref_calc_method"
        const val PREF_MADHAB = "pref_madhab"
        const val PREF_NOTIFICATIONS_ENABLED = "pref_notifications_enabled"
        const val PREF_NOTIFY_BEFORE = "pref_notify_before"
        const val PREF_THEME_MODE = "pref_theme_mode"
    }
}
