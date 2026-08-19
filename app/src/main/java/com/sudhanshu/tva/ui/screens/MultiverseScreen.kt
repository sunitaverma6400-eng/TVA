package com.sudhanshu.tva.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sudhanshu.tva.network.BranchDetailDto
import com.sudhanshu.tva.network.BranchDto
import com.sudhanshu.tva.network.CreateBranchRequest
import com.sudhanshu.tva.network.RelayClient
import com.sudhanshu.tva.network.TimelineEventDto
import com.sudhanshu.tva.util.TimeFormat
import kotlinx.coroutines.launch

/**
 * Step 12: Multiverse / Branch engine.
 *
 * A Branch is an alternate path for the USER'S OWN timeline, diverging from
 * a specific decision/event ("PRESENT -> Branch A/B/C -> Future A/B/C" per
 * the blueprint). Each branch has its own event history. This is distinct
 * from Step 11's Variant, which is a per-Person hypothetical, not the
 * user's own multiverse.
 */
@Composable
fun MultiverseScreen() {
    var branches by remember { mutableStateOf<List<BranchDto>>(emptyList()) }
    var selectedBranchId by remember { mutableStateOf<String?>(null) }
    var showAddBranch by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            val res = RelayClient.api.listBranches()
            if (res.isSuccessful) branches = res.body()?.branches ?: emptyList()
        }
    }

    LaunchedEffect(Unit) { load() }

    selectedBranchId?.let { id ->
        BranchDetailScreen(branchId = id, onBack = { selectedBranchId = null; load() })
        return
    }

    if (showAddBranch) {
        AddBranchScreen(
            onSave = { name, originEventId, description ->
                scope.launch {
                    RelayClient.api.createBranch(CreateBranchRequest(name, originEventId, description))
                    showAddBranch = false
                    load()
                }
            },
            onCancel = { showAddBranch = false }
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddBranch = true }) { Text("+") }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            Text("Multiverse", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Branches from your timeline's key decisions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (branches.isEmpty()) {
                Text(
                    "No branches yet. Tap + to create one from a key decision or event.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            } else {
                LazyColumn {
                    items(branches) { branch ->
                        Card(modifier = Modifier.fillMaxSize().padding(bottom = 8.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(branch.name, style = MaterialTheme.typography.titleLarge)
                                if (branch.description.isNotBlank()) {
                                    Text(branch.description, style = MaterialTheme.typography.bodyMedium)
                                }
                                Button(
                                    onClick = { selectedBranchId = branch.id },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Open branch")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBranchScreen(
    onSave: (name: String, originEventId: String?, description: String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("Branch A") }
    var description by remember { mutableStateOf("") }
    var originEvents by remember { mutableStateOf<List<TimelineEventDto>>(emptyList()) }
    var selectedOrigin by remember { mutableStateOf<TimelineEventDto?>(null) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val res = RelayClient.api.listEvents(limit = 100)
        if (res.isSuccessful) originEvents = res.body()?.events ?: emptyList()
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("New Branch", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Pick the decision/event this branch diverges from (optional).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Branch name") },
            modifier = Modifier.fillMaxSize().padding(bottom = 12.dp)
        )

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedOrigin?.title ?: "(none)",
                onValueChange = {},
                readOnly = true,
                label = { Text("Origin event") },
                modifier = Modifier.fillMaxSize().menuAnchor().padding(bottom = 12.dp)
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("(none)") }, onClick = { selectedOrigin = null; expanded = false })
                originEvents.forEach { event ->
                    DropdownMenuItem(
                        text = { Text("${event.title} — ${TimeFormat.display(event.event_time)}") },
                        onClick = { selectedOrigin = event; expanded = false }
                    )
                }
            }
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            modifier = Modifier.fillMaxSize().padding(bottom = 20.dp)
        )

        Button(
            onClick = { onSave(name.trim().ifBlank { "Branch" }, selectedOrigin?.id, description.trim()) },
            modifier = Modifier.fillMaxSize()
        ) {
            Text("Create Branch")
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxSize()) {
            Text("Cancel")
        }
    }
}

@Composable
private fun BranchDetailScreen(branchId: String, onBack: () -> Unit) {
    var detail by remember { mutableStateOf<BranchDetailDto?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(branchId) {
        loading = true
        val res = RelayClient.api.getBranch(branchId)
        if (res.isSuccessful) detail = res.body()
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        TextButton(onClick = onBack) { Text("← Back") }

        if (loading) {
            CircularProgressIndicator()
            return@Column
        }

        detail?.let { b ->
            Text(b.name, style = MaterialTheme.typography.headlineLarge)
            if (b.description.isNotBlank()) {
                Text(b.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
            b.origin_event?.let { origin ->
                Text(
                    "Diverged from: ${origin.title} (${TimeFormat.display(origin.event_time)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                "\nThis branch's history",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            if (b.events.isEmpty()) {
                Text("No events in this branch yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
            } else {
                LazyColumn {
                    items(b.events) { event ->
                        Card(modifier = Modifier.fillMaxSize().padding(bottom = 8.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(event.title, style = MaterialTheme.typography.titleLarge)
                                Text(TimeFormat.display(event.event_time), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        } ?: Text("Could not load branch.", style = MaterialTheme.typography.bodyMedium)
    }
}
