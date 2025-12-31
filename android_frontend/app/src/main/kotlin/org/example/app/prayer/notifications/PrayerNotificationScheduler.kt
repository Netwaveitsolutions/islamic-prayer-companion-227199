package org.example.app.prayer.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.preference.PreferenceManager
import org.example.app.domain.PrayerName
import org.example.app.domain.PrayerTimes
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class PrayerNotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    // PUBLIC_INTERFACE
    /**
     * Schedule alarms for today's prayer times.
     *
     * Reliability notes:
     * - De-dupes by canceling matching PendingIntents before scheduling.
     * - Best-effort exact alarms: uses setExactAndAllowWhileIdle when allowed; otherwise falls back to inexact.
     * - Records the next scheduled notification timestamp for UI display.
     */
    fun scheduleForToday(times: PrayerTimes, notifyBeforeMinutes: Int, reason: String = "unknown") {
        val today = LocalDate.now()
        // Re-schedule: cancel existing alarms for today first.
        cancelForDate(today)

        val scheduleMode = if (canScheduleExactAlarms()) "exact" else "inexact"
        var nextEpochMillis: Long? = null

        val tz = runCatching { ZoneId.of(times.timezone) }.getOrNull() ?: ZoneId.systemDefault()

        val pairs = times.asPairs()
        for ((prayer, timeStr) in pairs) {
            val prayerTime = runCatching { LocalTime.parse(timeStr) }.getOrNull() ?: continue
            val triggerTime = prayerTime.minusMinutes(notifyBeforeMinutes.toLong())
            val triggerDateTime = LocalDateTime.of(today, triggerTime)

            val epochMillis = triggerDateTime.atZone(tz).toInstant().toEpochMilli()
            if (epochMillis <= System.currentTimeMillis()) {
                // Don't schedule past alarms.
                continue
            }

            val requestCode = requestCodeFor(today, prayer)
            val pi = buildAlarmPendingIntent(
                requestCode = requestCode,
                prayer = prayer,
                prayerTime = timeStr
            )

            // De-dupe: explicitly cancel any existing matching PI before setting a new one.
            alarmManager.cancel(pi)

            scheduleAlarm(epochMillis, pi)

            if (nextEpochMillis == null || epochMillis < nextEpochMillis) {
                nextEpochMillis = epochMillis
            }
        }

        // Persist schedule metadata for debugging + in-app UX.
        prefs.edit()
            .putString(KEY_LAST_SCHEDULED_DATE, today.toString())
            .putString(KEY_LAST_SCHEDULED_REASON, reason)
            .putString(KEY_LAST_SCHEDULE_MODE, scheduleMode)
            .putLong(KEY_NEXT_SCHEDULED_EPOCH_MILLIS, nextEpochMillis ?: -1L)
            .apply()
    }

    // PUBLIC_INTERFACE
    /** Cancels all known alarms (today + tomorrow). */
    fun cancelAll() {
        // Best-effort: cancel today + tomorrow to cover alarms that were scheduled close to midnight.
        cancelForDate(LocalDate.now())
        cancelForDate(LocalDate.now().plusDays(1))

        prefs.edit()
            .putLong(KEY_NEXT_SCHEDULED_EPOCH_MILLIS, -1L)
            .putString(KEY_LAST_SCHEDULED_REASON, "cancelAll")
            .apply()
    }

    // PUBLIC_INTERFACE
    /** Returns next scheduled epoch millis if known, else null. */
    fun getNextScheduledEpochMillis(): Long? {
        val v = prefs.getLong(KEY_NEXT_SCHEDULED_EPOCH_MILLIS, -1L)
        return if (v > 0L) v else null
    }

    // PUBLIC_INTERFACE
    /** Record a human-readable status string for support/debugging (and to aid Settings UI). */
    fun recordLastStatus(status: String) {
        prefs.edit().putString(KEY_LAST_SCHEDULED_REASON, status).apply()
    }

    private fun cancelForDate(date: LocalDate) {
        for (prayer in PrayerName.entries) {
            val requestCode = requestCodeFor(date, prayer)
            val pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pi)
        }
    }

    private fun buildAlarmPendingIntent(
        requestCode: Int,
        prayer: PrayerName,
        prayerTime: String
    ): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
                putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayer.name)
                putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, prayerTime)
            },
            // UPDATE_CURRENT is fine because equality ignores extras; requestCode ensures uniqueness.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleAlarm(epochMillis: Long, pi: PendingIntent) {
        val now = System.currentTimeMillis()
        if (epochMillis <= now) return

        val useExact = canScheduleExactAlarms()

        if (useExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pi)
            return
        }

        // Best-effort fallback when exact alarms are restricted by policy/user setting.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pi)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, epochMillis, pi)
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { alarmManager.canScheduleExactAlarms() }.getOrDefault(false)
        } else {
            true
        }
    }

    private fun requestCodeFor(date: LocalDate, prayer: PrayerName): Int {
        // Keep it stable and unique per day/prayer.
        val yyyymmdd = date.year * 10_000 + date.monthValue * 100 + date.dayOfMonth
        return yyyymmdd * 10 + prayer.ordinal
    }

    companion object {
        private const val KEY_LAST_SCHEDULED_DATE = "notifications_last_scheduled_date"
        private const val KEY_LAST_SCHEDULED_REASON = "notifications_last_scheduled_reason"
        private const val KEY_LAST_SCHEDULE_MODE = "notifications_last_schedule_mode"
        private const val KEY_NEXT_SCHEDULED_EPOCH_MILLIS = "notifications_next_scheduled_epoch_millis"

        // PUBLIC_INTERFACE
        /** Formats an epoch millis timestamp (if any) into a user-friendly local date/time string. */
        fun formatNextScheduledLabel(epochMillis: Long?): String {
            if (epochMillis == null || epochMillis <= 0L) return "—"
            val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
            // Keep it simple and locale-safe without extra dependencies.
            val hh = dt.hour.toString().padStart(2, '0')
            val mm = dt.minute.toString().padStart(2, '0')
            return "${dt.toLocalDate()} $hh:$mm"
        }
    }
}
