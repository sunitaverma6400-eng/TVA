package com.sudhanshu.tva.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sudhanshu.tva.data.ProfileRepository
import com.sudhanshu.tva.permissions.PermissionManager
import com.sudhanshu.tva.sync.DeviceSyncService
import com.sudhanshu.tva.sync.SyncPreferences
import com.sudhanshu.tva.sync.LocationTrackingService
import kotlinx.coroutines.flow.first

@Composable
fun DeviceSyncScreen() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(SyncPreferences.isEnabled(context)) }
    var notificationGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                PermissionManager.hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        )
    }
    var locationTracking by remember { mutableStateOf(context.getSharedPreferences("tva_sync_preferences", android.content.Context.MODE_PRIVATE).getBoolean("continuous_location_enabled", false)) }
    var backgroundLocation by remember {
        mutableStateOf(PermissionManager.hasBackgroundLocationPermission(context))
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notificationGranted = Build.VERSION.SDK_INT < 33 ||
            PermissionManager.hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Continuous Device Sync", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Runs as a visible Android foreground service. It can sync only the categories you already allowed. " +
                "Android keeps a persistent notification while it is running.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Status: ${if (enabled) "ACTIVE" else "OFF"}", style = MaterialTheme.typography.titleLarge)
                Text("Usage + notification counts: ${if (PermissionManager.hasUsageStatsPermission(context)) "available" else "grant Settings access"}")
                Text("Contacts: ${if (PermissionManager.hasContactsPermission(context)) "available" else "not granted"}")
                Text("Location background access: ${if (backgroundLocation) "available" else "not granted"}")
                Text("Continuous location: ${if (locationTracking) "ACTIVE" else "OFF"}")
                Text("Notification permission: ${if (notificationGranted) "available" else "not granted"}")
            }
        }

        if (!notificationGranted && Build.VERSION.SDK_INT >= 33) {
            Button(
                onClick = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) { Text("Allow sync notification") }
        }

        Button(
            onClick = {
                if (!enabled) {
                    SyncPreferences.setEnabled(context, true)
                    DeviceSyncService.start(context)
                    enabled = true
                } else {
                    SyncPreferences.setEnabled(context, false)
                    DeviceSyncService.stop(context)
                    enabled = false
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text(if (enabled) "Stop continuous sync" else "Start continuous sync")
        }

        Button(
            onClick = {
                if (!locationTracking) {
                    if (PermissionManager.hasLocationPermission(context)) {
                        LocationTrackingService.start(context)
                        locationTracking = true
                    }
                } else {
                    LocationTrackingService.stop(context)
                    locationTracking = false
                }
            },
            enabled = PermissionManager.hasLocationPermission(context),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(if (locationTracking) "Stop continuous location" else "Start continuous location")
        }

        if (!backgroundLocation) {
            Button(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Open app settings for background location") }
        }
    }
}
