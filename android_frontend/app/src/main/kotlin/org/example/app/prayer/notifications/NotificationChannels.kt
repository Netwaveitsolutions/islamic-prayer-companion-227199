package org.example.app.prayer.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val CHANNEL_PRAYER_REMINDERS = "prayer_reminders"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = nm.getNotificationChannel(CHANNEL_PRAYER_REMINDERS)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_PRAYER_REMINDERS,
            "Prayer reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for upcoming prayer times"
        }

        nm.createNotificationChannel(channel)
    }
}
