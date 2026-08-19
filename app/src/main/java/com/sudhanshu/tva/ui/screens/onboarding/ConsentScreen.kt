package com.sudhanshu.tva.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ConsentCategory(
    val title: String,
    val description: String,
    val required: Boolean = false
)

private val categories = listOf(
    ConsentCategory(
        "Basic profile",
        "Name, date of birth, bio you type in yourself. Needed for TVA to work at all.",
        required = true
    ),
    ConsentCategory(
        "Device/app signals",
        "App usage time (which apps, how long) and notification counts per app (never message content) — only after you grant special Settings access."
    ),
    ConsentCategory(
        "Calendar & activity",
        "Calendar events (auto-import), contacts (you pick which to add as People), and location (only when you tap 'Log Location') — only if you grant the Android permission when asked."
    ),
    ConsentCategory(
        "Historical import",
        "Files or data you manually import (old chat exports, journals, etc)."
    ),
    ConsentCategory(
        "Continuous device sync",
        "Optional visible foreground-service sync every ~15 minutes for the categories you separately allow. Android shows a persistent notification while this is active; you can stop it anytime."
    )
)

/**
 * Step 6: shown once, before any profile exists. Every category is opt-in
 * and unchecked by default except the required basic-profile one (without
 * which the app has nothing to work with). The user can revisit this screen
 * later from Settings to change their mind on any category.
 */
@Composable
fun ConsentScreen(onComplete: (basicProfile: Boolean, deviceSignals: Boolean, calendar: Boolean, historical: Boolean, continuousSync: Boolean) -> Unit) {
    var basicProfile by remember { mutableStateOf(true) }
    var deviceSignals by remember { mutableStateOf(false) }
    var calendar by remember { mutableStateOf(false) }
    var historical by remember { mutableStateOf(false) }
    var continuousSync by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Before we begin", style = MaterialTheme.typography.headlineLarge)
        Text(
            "TVA only uses what you explicitly allow. Nothing is collected in a category you leave unchecked. " +
                "TVA never accesses anyone else's private data — only yours.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )

        Card(modifier = Modifier.padding(bottom = 12.dp)) {
            ConsentRow("Basic profile", categories[0].description, basicProfile, enabled = false) { }
        }
        Card(modifier = Modifier.padding(bottom = 12.dp)) {
            ConsentRow(categories[1].title, categories[1].description, deviceSignals) { deviceSignals = it }
        }
        Card(modifier = Modifier.padding(bottom = 12.dp)) {
            ConsentRow(categories[2].title, categories[2].description, calendar) { calendar = it }
        }
        Card(modifier = Modifier.padding(bottom = 12.dp)) {
            ConsentRow(categories[3].title, categories[3].description, historical) { historical = it }
        }
        Card(modifier = Modifier.padding(bottom = 20.dp)) {
            ConsentRow(categories[4].title, categories[4].description, continuousSync) { continuousSync = it }
        }

        Button(
            onClick = { onComplete(basicProfile, deviceSignals, calendar, historical, continuousSync) },
            modifier = Modifier.fillMaxSize()
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun ConsentRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
