package com.sudhanshu.tva.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sudhanshu.tva.network.CreateEventRequest
import com.sudhanshu.tva.network.RelayClient
import com.sudhanshu.tva.network.TimelineEventDto
import com.sudhanshu.tva.permissions.DeviceDataCollector
import com.sudhanshu.tva.permissions.PermissionManager
import com.sudhanshu.tva.util.TimeFormat
import kotlinx.coroutines.launch

private sealed class LoadState {
    data object Loading : LoadState()
    data class Loaded(val events: List<TimelineEventDto>) : LoadState()
    data class Error(val message: String) : LoadState()
}

@Composable
fun TimelineScreen() {
    var loadState by remember { mutableStateOf<LoadState>(LoadState.Loading) }
    var showAddEvent by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val deviceIdentity = remember(context) { com.sudhanshu.tva.data.DeviceIdentity(context) }
    var deviceName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { deviceName = deviceIdentity.getDeviceNameOrDefault() }

    fun refresh() {
        scope.launch {
            loadState = LoadState.Loading
            loadState = try {
                val res = RelayClient.api.listEvents()
                if (res.isSuccessful) {
                    LoadState.Loaded(res.body()?.events ?: emptyList())
                } else {
                    LoadState.Error("Relay error ${res.code()}")
                }
            } catch (e: Exception) {
                LoadState.Error(e.message ?: "Unreachable")
            }
        }
    }

    fun syncCalendar() {
        if (!PermissionManager.hasPermission(context, android.Manifest.permission.READ_CALENDAR)) {
            syncStatus = "Calendar permission not granted (see Step 7 / People tab)"
            return
        }
        scope.launch {
            syncStatus = "Syncing..."
            try {
                val deviceEvents = DeviceDataCollector.readCalendarEvents(context, limit = 50)
                val existing = (loadState as? LoadState.Loaded)?.events ?: emptyList()
                val existingKeys = existing.map { "${it.title}|${it.event_time.toLong()}" }.toSet()

                var added = 0
                for (e in deviceEvents) {
                    val key = "${e.title}|${e.startTimeEpochMillis / 1000}"
                    if (key in existingKeys) continue // already synced, avoid duplicates
                    val res = RelayClient.api.createEvent(
                        CreateEventRequest(
                            title = e.title.ifBlank { "(untitled calendar event)" },
                            event_type = "calendar",
                            event_time = e.startTimeEpochMillis / 1000.0,
                            location = e.location,
                            source = "calendar",
                            source_device = deviceName
                        )
                    )
                    if (res.isSuccessful) added++
                }
                syncStatus = "Synced $added new event(s) from calendar"
                refresh()
            } catch (e: Exception) {
                syncStatus = "Sync failed: ${e.message}"
            }
        }
    }

    fun logLocation() {
        if (!com.sudhanshu.tva.permissions.LocationCollector.hasLocationPermission(context)) {
            syncStatus = "Location permission not granted (see Step 7 / People tab)"
            return
        }
        scope.launch {
            syncStatus = "Getting location..."
            val loc = com.sudhanshu.tva.permissions.LocationCollector.getLastKnownLocation(context)
            if (loc == null) {
                syncStatus = "No location available yet — try again after moving around a bit"
                return@launch
            }
            try {
                RelayClient.api.createEvent(
                    CreateEventRequest(
                        title = "Location check-in",
                        event_type = "location_checkin",
                        event_time = loc.timestampMillis / 1000.0,
                        location = "${loc.latitude}, ${loc.longitude}",
                        source = "location",
                        source_device = deviceName
                    )
                )
                syncStatus = "Location logged"
                refresh()
            } catch (e: Exception) {
                syncStatus = "Failed to log location: ${e.message}"
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    if (showAddEvent) {
        AddEventScreen(
            onSave = { title, type, epochSeconds, location, people ->
                scope.launch {
                    try {
                        RelayClient.api.createEvent(
                            CreateEventRequest(
                                title = title,
                                event_type = type,
                                event_time = epochSeconds,
                                location = location.ifBlank { null },
                                people = people,
                                source_device = deviceName
                            )
                        )
                        showAddEvent = false
                        refresh()
                    } catch (e: Exception) {
                        loadState = LoadState.Error("Failed to save: ${e.message}")
                        showAddEvent = false
                    }
                }
            },
            onCancel = { showAddEvent = false }
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddEvent = true }) {
                Text("+")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("Timeline", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Past → Present → Future",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            Button(onClick = { syncCalendar() }, modifier = Modifier.padding(bottom = 8.dp)) {
                Text("Sync Calendar")
            }
            Button(onClick = { logLocation() }, modifier = Modifier.padding(bottom = 8.dp)) {
                Text("Log Location")
            }
            syncStatus?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(bottom = 12.dp))
            }

            when (val state = loadState) {
                is LoadState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is LoadState.Error -> Column {
                    Text(
                        "Couldn't load timeline: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = { refresh() }, modifier = Modifier.padding(top = 12.dp)) {
                        Text("Retry")
                    }
                }
                is LoadState.Loaded -> {
                    if (state.events.isEmpty()) {
                        Text(
                            "No events yet. Tap + to add your first one, or grant calendar access (Step 7) to auto-import.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    } else {
                        TimelineEventList(state.events, onRefresh = { refresh() })
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineEventList(events: List<TimelineEventDto>, onRefresh: () -> Unit) {
    val now = TimeFormat.nowEpochSeconds()
    val past = events.filter { it.event_time < now - 3600 }
    val present = events.filter { it.event_time in (now - 3600)..(now + 3600) }
    val future = events.filter { it.event_time > now + 3600 }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (past.isNotEmpty()) {
            item { SectionHeader("PAST") }
            items(past) { EventCard(it, onRefresh) }
        }
        if (present.isNotEmpty()) {
            item { SectionHeader("PRESENT") }
            items(present) { EventCard(it, onRefresh) }
        }
        if (future.isNotEmpty()) {
            item { SectionHeader("FUTURE") }
            items(future) { EventCard(it, onRefresh) }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun EventCard(event: TimelineEventDto, onRefresh: () -> Unit) {
    val scope = rememberCoroutineScope()
    Card(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleLarge)
            Text(TimeFormat.display(event.event_time), style = MaterialTheme.typography.bodyMedium)
            if (!event.location.isNullOrBlank()) {
                Text(event.location, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
            }
            Text(
                "${event.event_type} · ${event.source}" + (event.source_device?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
            if (event.people.isNotEmpty()) {
                Text(
                    "with ${event.people.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (event.connected_event_ids.isNotEmpty()) {
                Text(
                    "🔗 ${event.connected_event_ids.size} connected event(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            TextButton(onClick = {
                scope.launch {
                    if (event.revoked) RelayClient.api.unrevokeEvent(event.id)
                    else RelayClient.api.revokeEvent(event.id)
                    onRefresh()
                }
            }) {
                Text(
                    if (event.revoked) "Revoked — tap to restore" else "Revoke (exclude from AI)",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
