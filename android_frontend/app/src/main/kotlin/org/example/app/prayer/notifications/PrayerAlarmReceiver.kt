package org.example.app.prayer.notifications

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.example.app.R

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PRAYER_ALARM) return

        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
        val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""

        NotificationChannels.ensureCreated(context)

        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_PRAYER_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Upcoming prayer: $prayerName")
            .setContentText(if (prayerTime.isNotBlank()) "Time: $prayerTime" else "It's almost time.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

        // Android 13+ requires POST_NOTIFICATIONS runtime permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission revoked or not granted at runtime; ignore safely.
        }
    }

    companion object {
        const val ACTION_PRAYER_ALARM = "org.example.app.ACTION_PRAYER_ALARM"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
    }
}
