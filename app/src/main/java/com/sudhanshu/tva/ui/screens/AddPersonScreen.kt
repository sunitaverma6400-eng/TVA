package com.sudhanshu.tva.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val relationshipOptions = listOf("family", "friend", "colleague", "other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonScreen(onSave: (name: String, relationship: String, notes: String) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("friend") }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Add Person", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Manually entered by you — TVA never pulls this from anyone else's accounts or device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxSize().padding(bottom = 12.dp)
        )

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = relationship,
                onValueChange = {},
                readOnly = true,
                label = { Text("Relationship") },
                modifier = Modifier.fillMaxSize().menuAnchor().padding(bottom = 12.dp)
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                relationshipOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { relationship = option; expanded = false }
                    )
                }
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxSize().padding(bottom = 20.dp)
        )

        Button(
            onClick = { onSave(name.trim(), relationship, notes.trim()) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxSize()
        ) {
            Text("Save Person")
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxSize()) {
            Text("Cancel")
        }
    }
}
