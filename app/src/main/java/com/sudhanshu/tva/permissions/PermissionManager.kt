package com.sudhanshu.tva.permissions

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import androidx.core.content.ContextCompat

/**
 * Step 7: Permission/data collection layer.
 *
 * IMPORTANT: this module only ever REQUESTS permissions that map to a
 * category the user already consented to in ConsentScreen (Step 6). If the
 * user left "Calendar & activity" unchecked, TVA never asks for calendar
 * permission at all — the request simply doesn't happen, regardless of
 * whether Android would grant it.
 */
object PermissionManager {

    // Requested only if consentCalendarActivity == true
    val calendarActivityPermissions = arrayOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Requested only if consentDeviceSignals == true (standard runtime part;
    // usage-stats access is special and handled separately below)
    val deviceSignalsRuntimePermissions = arrayOf(
        Manifest.permission.POST_NOTIFICATIONS
    )

    fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun hasCalendarPermission(context: Context): Boolean =
        hasPermission(context, Manifest.permission.READ_CALENDAR)

    fun hasContactsPermission(context: Context): Boolean =
        hasPermission(context, Manifest.permission.READ_CONTACTS)

    fun hasCameraPermission(context: Context): Boolean =
        hasPermission(context, Manifest.permission.CAMERA)

    fun hasBackgroundLocationPermission(context: Context): Boolean =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else true

    fun hasLocationPermission(context: Context): Boolean =
        hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ||
            hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasAnyCalendarActivityPermission(context: Context): Boolean =
        calendarActivityPermissions.any { hasPermission(context, it) }

    /**
     * PACKAGE_USAGE_STATS can't be requested via the normal permission
     * dialog — it must be granted manually in system Settings. This checks
     * whether it's currently granted using AppOpsManager (the standard way).
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Notification listener access is a special permission like usage stats —
     * can't use the runtime dialog, must be granted in Settings.
     */
    fun hasNotificationListenerAccess(context: Context): Boolean =
        TvaNotificationListenerService.hasAccess(context)
}
