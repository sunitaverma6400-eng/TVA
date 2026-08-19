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
import com.sudhanshu.tva.network.RelayClient
import com.sudhanshu.tva.network.TelemetryBatchRequest
import com.sudhanshu.tva.network.TelemetryItem
import com.sudhanshu.tva.permissions.LocationCollector
import com.sudhanshu.tva.permissions.PermissionManager
import kotlinx.coroutines.*

/**
 * Separate location foreground service. It is always visible and only runs
 * after the user explicitly starts it with location permission granted.
 */
class LocationTrackingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        if (!getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ENABLED, false) ||
            !PermissionManager.hasLocationPermission(this)) {
            stopSelf()
            return
        }
        createChannel()
        val pending = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("TVA location tracking is active")
            .setContentText("Visible foreground tracking. Tap to return to TVA.")
            .setOngoing(true)
            .setContentIntent(pending)
            .build()
        startForeground(NOTIFICATION_ID, notification)

        job = scope.launch {
            while (isActive) {
                sample()
                delay(5 * 60 * 1000L)
            }
        }
    }

    private suspend fun sample() {
        if (!PermissionManager.hasLocationPermission(this)) return
        val location = LocationCollector.getLastKnownLocation(this) ?: return
        val device = DeviceIdentity(this).getDeviceNameOrDefault()
        try {
            RelayClient.api.uploadTelemetry(
                TelemetryBatchRequest(
                    source_device = device,
                    items = listOf(
                        TelemetryItem(
                            kind = "location",
                            title = "Continuous location sample",
                            event_time = location.timestampMillis / 1000.0,
                            location = "${location.latitude},${location.longitude}",
                            details = mapOf("accuracy_meters" to location.accuracyMeters.toString()),
                            source_device = device
                        )
                    )
                )
            )
        } catch (_: Exception) {
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        job?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "TVA location tracking", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    companion object {
        private const val PREFS = "tva_sync_preferences"
        private const val KEY_ENABLED = "continuous_location_enabled"
        private const val CHANNEL_ID = "tva_location_tracking"
        private const val NOTIFICATION_ID = 4102

        fun start(context: android.content.Context) {
            if (!PermissionManager.hasLocationPermission(context)) return
            context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENABLED, true).apply()
            ContextCompat.startForegroundService(
                context, Intent(context, LocationTrackingService::class.java)
            )
        }

        fun stop(context: android.content.Context) {
            context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENABLED, false).apply()
            context.stopService(Intent(context, LocationTrackingService::class.java))
        }
    }
}
