package com.sudhanshu.tva.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sudhanshu.tva.data.ProfileRepository
import com.sudhanshu.tva.data.UserProfile
import com.sudhanshu.tva.network.CreatePersonRequest
import com.sudhanshu.tva.network.PersonDto
import com.sudhanshu.tva.network.RelayClient
import com.sudhanshu.tva.permissions.AppUsageSummary
import com.sudhanshu.tva.permissions.DeviceDataCollector
import com.sudhanshu.tva.permissions.PermissionManager
import com.sudhanshu.tva.permissions.TvaNotificationListenerService
import com.sudhanshu.tva.permissions.UsageStatsCollector
import kotlinx.coroutines.launch

/**
 * Step 11 (extended): own profile (Step 6) + full list of manually-added
 * Person entities + a Device Insights section (usage stats, notification
 * counts — Step 7 extended) + selective Contacts import.
 */
@Composable
fun PeopleScreen() {
    val context = LocalContext.current
    val repository = remember(context) { ProfileRepository(context) }
    val profile by repository.profileFlow.collectAsState(initial = UserProfile())
    val scope = rememberCoroutineScope()

    var people by remember { mutableStateOf<List<PersonDto>>(emptyList()) }
    var selectedPersonId by remember { mutableStateOf<String?>(null) }
    var showAddPerson by remember { mutableStateOf(false) }
    var showContactsPicker by remember { mutableStateOf(false) }

    var usageSummary by remember { mutableStateOf<List<AppUsageSummary>>(emptyList()) }
    var notificationCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    fun loadPeople() {
        scope.launch {
            val res = RelayClient.api.listPeople()
            if (res.isSuccessful) people = res.body()?.people ?: emptyList()
        }
    }

    LaunchedEffect(Unit) {
        loadPeople()
        if (PermissionManager.hasUsageStatsPermission(context)) {
            usageSummary = UsageStatsCollector.getTodayUsageSummary(context)
        }
        if (TvaNotificationListenerService.hasAccess(context)) {
            notificationCounts = TvaNotificationListenerService.getTodayCounts(context)
        }
    }

    selectedPersonId?.let { id ->
        PersonDetailScreen(personId = id, onBack = { selectedPersonId = null; loadPeople() })
        return
    }

    if (showAddPerson) {
        AddPersonScreen(
            onSave = { name, relationship, notes ->
                scope.launch {
                    RelayClient.api.createPerson(CreatePersonRequest(name, relationship, notes))
                    showAddPerson = false
                    loadPeople()
                }
            },
            onCancel = { showAddPerson = false }
        )
        return
    }

    if (showContactsPicker) {
        ContactsPickerScreen(
            onImport = { names ->
                scope.launch {
                    names.forEach { name ->
                        RelayClient.api.createPerson(CreatePersonRequest(name, "other", "Imported from contacts"))
                    }
                    showContactsPicker = false
                    loadPeople()
                }
            },
            onCancel = { showContactsPicker = false }
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddPerson = true }) { Text("+") }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("People", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Your Personal Temporal Profile",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            Card(modifier = Modifier.padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(profile.name.ifBlank { "(no name set)" }, style = MaterialTheme.typography.titleLarge)
                    if (profile.dateOfBirth.isNotBlank()) {
                        Text("DOB: ${profile.dateOfBirth}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (profile.bio.isNotBlank()) {
                        Text(profile.bio, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (profile.consentCalendarActivity && PermissionManager.hasPermission(context, android.Manifest.permission.READ_CALENDAR)) {
                        val events = remember { DeviceDataCollector.readCalendarEvents(context, limit = 3) }
                        if (events.isNotEmpty()) {
                            Text(
                                "\nRecent calendar events:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                            events.forEach { e -> Text("• ${e.title}", style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
            }

            // --- Device Insights: usage stats + notification counts ---
            if (usageSummary.isNotEmpty() || notificationCounts.isNotEmpty()) {
                Card(modifier = Modifier.padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Device Insights (today)", style = MaterialTheme.typography.titleLarge)
                        if (usageSummary.isNotEmpty()) {
                            Text(
                                "\nTop apps by time used:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            usageSummary.forEach { u ->
                                Text(
                                    "• ${u.packageName} — ${UsageStatsCollector.formatDuration(u.totalForegroundMillis)}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        if (notificationCounts.isNotEmpty()) {
                            Text(
                                "\nNotifications today:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            notificationCounts.entries.sortedByDescending { it.value }.take(5).forEach { (pkg, count) ->
                                Text("• $pkg — $count", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            TextButton(onClick = { showContactsPicker = true }, modifier = Modifier.padding(bottom = 8.dp)) {
                Text("Import from Contacts")
            }

            Text(
                "Other people",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (people.isEmpty()) {
                Text(
                    "None added yet. Tap + to add someone — always manual entry, never pulled from their accounts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            } else {
                LazyColumn {
                    items(people) { person ->
                        Card(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(person.name, style = MaterialTheme.typography.titleLarge)
                                Text(person.relationship, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                                Button(
                                    onClick = { selectedPersonId = person.id },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("View / Add Variants")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
