package com.sudhanshu.tva.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.sudhanshu.tva.network.CreateVariantRequest
import com.sudhanshu.tva.network.PersonDetailDto
import com.sudhanshu.tva.network.RelayClient
import kotlinx.coroutines.launch

/**
 * Step 11: shows one Person's profile + their Variants (simulated alternate
 * possible paths — explicitly hypothetical "what if" branches, never framed
 * as an actual supernatural alternate self).
 */
@Composable
fun PersonDetailScreen(personId: String, onBack: () -> Unit) {
    var detail by remember { mutableStateOf<PersonDetailDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var showAddVariant by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            loading = true
            try {
                val res = RelayClient.api.getPerson(personId)
                if (res.isSuccessful) detail = res.body()
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(personId) { load() }

    if (showAddVariant) {
        AddVariantScreen(
            onSave = { label, description, divergence ->
                scope.launch {
                    RelayClient.api.createVariant(
                        personId,
                        CreateVariantRequest(label = label, description = description, divergence_point = divergence)
                    )
                    showAddVariant = false
                    load()
                }
            },
            onCancel = { showAddVariant = false }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        TextButton(onClick = onBack) { Text("← Back") }

        if (loading) {
            CircularProgressIndicator()
            return@Column
        }

        detail?.let { p ->
            Text(p.name, style = MaterialTheme.typography.headlineLarge)
            Text(
                p.relationship,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            if (p.notes.isNotBlank()) {
                Text(p.notes, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }

            Text(
                "\nVariants — simulated alternate possible paths",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Button(onClick = { showAddVariant = true }, modifier = Modifier.padding(bottom = 12.dp)) {
                Text("Add Variant")
            }

            if (p.variants.isEmpty()) {
                Text("No variants yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
            } else {
                LazyColumn {
                    items(p.variants) { v ->
                        Card(modifier = Modifier.fillMaxSize().padding(bottom = 8.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(v.label, style = MaterialTheme.typography.titleLarge)
                                Text(v.description, style = MaterialTheme.typography.bodyMedium)
                                if (v.divergence_point.isNotBlank()) {
                                    Text(
                                        "Diverged from: ${v.divergence_point}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } ?: Text("Could not load person.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AddVariantScreen(onSave: (label: String, description: String, divergence: String) -> Unit, onCancel: () -> Unit) {
    var label by remember { mutableStateOf("Variant A") }
    var description by remember { mutableStateOf("") }
    var divergence by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Add Variant", style = MaterialTheme.typography.headlineLarge)
        Text(
            "A hypothetical \"what if\" path — not a real alternate person.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Label (e.g. Variant A)") },
            modifier = Modifier.fillMaxSize().padding(bottom = 12.dp)
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description — what's different in this path?") },
            modifier = Modifier.fillMaxSize().padding(bottom = 12.dp)
        )
        OutlinedTextField(
            value = divergence,
            onValueChange = { divergence = it },
            label = { Text("Diverged from (optional, e.g. a decision or event)") },
            modifier = Modifier.fillMaxSize().padding(bottom = 20.dp)
        )

        Button(
            onClick = { onSave(label.trim().ifBlank { "Variant" }, description.trim(), divergence.trim()) },
            enabled = description.isNotBlank(),
            modifier = Modifier.fillMaxSize()
        ) {
            Text("Save Variant")
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxSize()) {
            Text("Cancel")
        }
    }
}
