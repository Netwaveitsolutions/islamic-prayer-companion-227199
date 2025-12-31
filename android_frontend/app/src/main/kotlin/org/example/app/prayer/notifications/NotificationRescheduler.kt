package org.example.app.prayer.notifications

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.app.data.prefs.AppPreferences
import org.example.app.data.prayertimes.PrayerTimesRepository
import java.time.LocalDate

object NotificationRescheduler {

    // PUBLIC_INTERFACE
    /**
     * Reschedule prayer notifications using the user's current settings (city/method/madhab/high-lat rule).
     *
     * Behavior:
     * - If notifications are disabled, cancels any existing alarms.
     * - If Android 13+ notification permission is not granted, does not schedule (best-effort).
     * - Uses cached prayer times if offline.
     */
    suspend fun rescheduleNow(context: Context, reason: String) {
        return withContext(Dispatchers.IO) {
            val appPrefs = AppPreferences(context)

            val scheduler = PrayerNotificationScheduler(context)
            if (!appPrefs.notificationsEnabled()) {
                scheduler.cancelAll()
                scheduler.recordLastStatus("disabled ($reason)")
                return@withContext
            }

            // If Android 13+ permission is missing, avoid scheduling (we also gate display in receiver).
            if (!NotificationPermissionHelper.canPostNotifications(context)) {
                scheduler.cancelAll()
                scheduler.recordLastStatus("missing_post_notifications_permission ($reason)")
                return@withContext
            }

            NotificationChannels.ensureCreated(context)

            val repo = PrayerTimesRepository(context)
            val settings = repo.currentSettings()

            val today = LocalDate.now()
            val result = repo.getPrayerTimesForDate(
                date = today,
                city = settings.city,
                method = settings.method,
                school = settings.school,
                highLatitudeRule = settings.highLatitudeRule
            )

            result.onSuccess { withSource ->
                scheduler.scheduleForToday(
                    times = withSource.times,
                    notifyBeforeMinutes = appPrefs.notifyBeforeMinutes(),
                    reason = "$reason:${withSource.source.name.lowercase()}"
                )
            }.onFailure {
                // Best effort: we couldn't fetch times (and cache wasn't available). Don't crash.
                scheduler.recordLastStatus("failed_fetch_times ($reason)")
            }
        }
    }
}
