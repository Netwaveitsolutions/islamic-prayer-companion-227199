package org.example.app.prayer.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        val action = intent?.action ?: "unknown"

        // goAsync() so we can safely do async work (network/cache read) without blocking the main thread.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationRescheduler.rescheduleNow(appContext, reason = "broadcast:$action")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_RESCHEDULE = "org.example.app.ACTION_RESCHEDULE_PRAYER_NOTIFICATIONS"
    }
}
