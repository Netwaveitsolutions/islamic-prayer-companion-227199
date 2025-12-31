package org.example.app.prayer.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

object NotificationPermissionHelper {

    // PUBLIC_INTERFACE
    /**
     * Returns true if the app can post notifications right now.
     * On Android 13+ this requires the POST_NOTIFICATIONS runtime permission.
     */
    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    // PUBLIC_INTERFACE
    /** Request POST_NOTIFICATIONS permission on Android 13+. No-op on older versions. */
    fun requestPostNotifications(fragment: Fragment, requestCode: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        fragment.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), requestCode)
    }
}
