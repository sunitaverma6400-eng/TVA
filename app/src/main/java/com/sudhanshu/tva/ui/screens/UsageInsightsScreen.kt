package com.sudhanshu.tva.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sudhanshu.tva.data.DeviceIdentity
import com.sudhanshu.tva.network.RelayClient
import com.sudhanshu.tva.network.TelemetryBatchRequest
import com.sudhanshu.tva.network.TelemetryItem
import com.sudhanshu.tva.permissions.PermissionManager
import com.sudhanshu.tva.permissions.UsageStatsCollector
import com.sudhanshu.tva.permissions.TvaNotificationListenerService
import kotlinx.coroutines.launch

@Composable
fun UsageInsightsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var usage by remember { mutableStateOf(UsageStatsCollector.getTodayUsageSummary(context, 10)) }
    var notifications by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var status by remember { mutableStateOf("Ready") }

    LaunchedEffect(Unit) {
        notifications = TvaNotificationListenerService.getTodayCounts(context)
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Usage Insights", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Only aggregate app foreground time and notification counts are shown. Notification text is never collected.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Text("Top app usage today", style = MaterialTheme.typography.titleLarge)
        if (!PermissionManager.hasUsageStatsPermission(context)) {
            Text("Usage access is not granted in Android Settings.")
        } else {
            usage.forEach {
                Text("${it.packageName}: ${UsageStatsCollector.formatDuration(it.totalForegroundMillis)}")
            }
        }

        Text("Notification counts today", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp))
        if (!PermissionManager.hasNotificationListenerAccess(context)) {
            Text("Notification access is not granted.")
        } else {
            notifications.forEach { (pkg, count) -> Text("$pkg: $count") }
        }

        Button(
            onClick = {
                scope.launch {
                    usage = UsageStatsCollector.getTodayUsageSummary(context, 10)
                    notifications = TvaNotificationListenerService.getTodayCounts(context)
                    val device = DeviceIdentity(context).getDeviceNameOrDefault()
                    val now = System.currentTimeMillis() / 1000.0
                    val items = mutableListOf<TelemetryItem>()
                    usage.forEach {
                        items += TelemetryItem("usage", "App usage: ${it.packageName}", now,
                            mapOf("foreground_minutes" to (it.totalForegroundMillis / 60000L).toString()), source_device = device)
                    }
                    notifications.forEach { (pkg, count) ->
                        items += TelemetryItem("notification_count", "Notifications: $pkg", now,
                            mapOf("count" to count.toString()), source_device = device)
                    }
                    try {
                        val res = RelayClient.api.uploadTelemetry(TelemetryBatchRequest(device, items))
                        status = if (res.isSuccessful) "Synced ${res.body()?.accepted ?: 0} insights." else "Relay error ${res.code()}"
                    } catch (e: Exception) { status = e.message ?: "Sync failed" }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) { Text("Sync insights now") }

        Text(status, modifier = Modifier.padding(top = 8.dp))
    }
}
