package com.sudhanshu.tva.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sudhanshu.tva.MainActivity
import com.sudhanshu.tva.data.DeviceIdentity
import com.sudhanshu.tva.network.ContactSyncItem
import com.sudhanshu.tva.network.CreateEventRequest
import com.sudhanshu.tva.network.RelayClient
import com.sudhanshu.tva.network.TelemetryBatchRequest
import com.sudhanshu.tva.network.TelemetryItem
import com.sudhanshu.tva.permissions.DeviceDataCollector
import com.sudhanshu.tva.permissions.LocationCollector
import com.sudhanshu.tva.permissions.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull

/**
 * Explicitly enabled, visible foreground sync.
 *
 * It never hides itself: Android shows an ongoing notification while active.
 * Only categories already granted by the user are collected.
 */
class DeviceSyncService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loopJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        if (!SyncPreferences.isEnabled(this)) {
            stopSelf()
            return
        }
        createChannel()
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("TVA continuous sync is active")
            .setContentText("Only your consented device signals are being synced.")
            .setOngoing(true)
            .setContentIntent(openApp)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        loopJob = scope.launch {
            while (isActive) {
                syncOnce()
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    private suspend fun syncOnce() {
        val deviceName = DeviceIdentity(this@DeviceSyncService).getDeviceNameOrDefault()
        val profile = com.sudhanshu.tva.data.ProfileRepository(this@DeviceSyncService)
            .profileFlow.firstOrNull() ?: return

        if (!profile.onboardingComplete || !profile.consentContinuousSync) return

        val items = mutableListOf<TelemetryItem>()
        val now = System.currentTimeMillis() / 1000.0

        if (profile.consentDeviceSignals && PermissionManager.hasUsageStatsPermission(this)) {
            val usage = com.sudhanshu.tva.permissions.UsageStatsCollector
                .getTodayUsageSummary(this, 10)
            usage.forEach {
                items += TelemetryItem(
                    kind = "usage",
                    title = "App usage: ${it.packageName}",
                    event_time = now,
                    details = mapOf("foreground_minutes" to (it.totalForegroundMillis / 60000L).toString()),
                    source_device = deviceName
                )
            }

            val notifications = com.sudhanshu.tva.permissions.TvaNotificationListenerService
                .getTodayCounts(this)
            notifications.forEach { (pkg, count) ->
                items += TelemetryItem(
                    kind = "notification_count",
                    title = "Notifications: $pkg",
                    event_time = now,
                    details = mapOf("count" to count.toString()),
                    source_device = deviceName
                )
            }
        }

        if (profile.consentCalendarActivity && PermissionManager.hasCalendarPermission(this)) {
            // Calendar is already deduplicated server-side.
            DeviceDataCollector.readCalendarEvents(this, 100).forEach { event ->
                try {
                    RelayClient.api.createEvent(
                        CreateEventRequest(
                            title = event.title.ifBlank { "(untitled calendar event)" },
                            event_type = "calendar",
                            event_time = event.startTimeEpochMillis / 1000.0,
                            location = event.location,
                            source = "calendar",
                            source_device = deviceName
                        )
                    )
                } catch (_: Exception) {
                    // Other telemetry can still be attempted below.
                }
            }
        }

        if (items.isNotEmpty()) {
            try {
                RelayClient.api.uploadTelemetry(
                    TelemetryBatchRequest(deviceName, items.take(100))
                )
            } catch (_: Exception) {
                // A later cycle retries.
            }
        }

        syncContactsPeriodically(deviceName)
    }

    private suspend fun syncContactsPeriodically(deviceName: String) {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val last = prefs.getLong(KEY_CONTACTS_SYNC, 0L)
        if (System.currentTimeMillis() - last < CONTACT_SYNC_INTERVAL_MS) return

        val profile = com.sudhanshu.tva.data.ProfileRepository(this).profileFlow.firstOrNull() ?: return
        if (!profile.consentCalendarActivity || !PermissionManager.hasContactsPermission(this)) return

        val contacts = DeviceDataCollector.readContacts(this, 500).map {
            ContactSyncItem(it.name, it.hasPhoneNumber)
        }
        if (contacts.isEmpty()) return

        try {
            val response = RelayClient.api.syncContacts(
                com.sudhanshu.tva.network.ContactsSyncRequest(deviceName, contacts)
            )
            if (response.isSuccessful) {
                prefs.edit().putLong(KEY_CONTACTS_SYNC, System.currentTimeMillis()).apply()
            }
        } catch (_: Exception) {
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        loopJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "TVA continuous sync",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Visible status for the user's consented TVA foreground sync."
                }
            )
        }
    }

    companion object {
        private const val CHANNEL_ID = "tva_continuous_sync"
        private const val NOTIFICATION_ID = 4101
        private const val PREFS = "tva_sync_preferences"
        private const val KEY_CONTACTS_SYNC = "last_contacts_sync"
        private const val SYNC_INTERVAL_MS = 15 * 60 * 1000L
        private const val CONTACT_SYNC_INTERVAL_MS = 6 * 60 * 60 * 1000L

        fun start(context: android.content.Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(
                    context, Intent(context, DeviceSyncService::class.java)
                )
            } else {
                context.startService(Intent(context, DeviceSyncService::class.java))
            }
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, DeviceSyncService::class.java))
        }
    }
}
