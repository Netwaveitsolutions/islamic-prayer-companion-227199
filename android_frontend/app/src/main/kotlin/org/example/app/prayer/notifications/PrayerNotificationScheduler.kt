package org.example.app.prayer.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import org.example.app.domain.PrayerName
import org.example.app.domain.PrayerTimes
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class PrayerNotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    fun scheduleForToday(times: PrayerTimes, notifyBeforeMinutes: Int) {
        // Re-schedule: cancel existing alarms for today first.
        cancelForDate(LocalDate.now())
        scheduleForDate(LocalDate.now(), times, notifyBeforeMinutes)
    }

    fun cancelAll() {
        // Best-effort: cancel today + tomorrow to cover alarms that were scheduled close to midnight.
        cancelForDate(LocalDate.now())
        cancelForDate(LocalDate.now().plusDays(1))
    }

    private fun scheduleForDate(date: LocalDate, times: PrayerTimes, notifyBeforeMinutes: Int) {
        val tz = runCatching { ZoneId.of(times.timezone) }.getOrNull() ?: ZoneId.systemDefault()

        val pairs = times.asPairs()
        for ((prayer, timeStr) in pairs) {
            val prayerTime = runCatching { LocalTime.parse(timeStr) }.getOrNull() ?: continue
            val triggerTime = prayerTime.minusMinutes(notifyBeforeMinutes.toLong())
            val triggerDateTime = LocalDateTime.of(date, triggerTime)

            val epochMillis = triggerDateTime.atZone(tz).toInstant().toEpochMilli()
            if (epochMillis <= System.currentTimeMillis()) {
                // Don't schedule past alarms.
                continue
            }

            val requestCode = requestCodeFor(date, prayer)
            val pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, PrayerAlarmReceiver::class.java).apply {
                    action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayer.name)
                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, timeStr)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pi)
        }

        // Record which date we last scheduled (for debugging/support).
        prefs.edit().putString(KEY_LAST_SCHEDULED_DATE, date.toString()).apply()
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

    private fun requestCodeFor(date: LocalDate, prayer: PrayerName): Int {
        // Keep it stable and unique per day/prayer.
        val yyyymmdd = date.year * 10_000 + date.monthValue * 100 + date.dayOfMonth
        return yyyymmdd * 10 + prayer.ordinal
    }

    companion object {
        private const val KEY_LAST_SCHEDULED_DATE = "notifications_last_scheduled_date"
    }
}
