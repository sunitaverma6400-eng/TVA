package com.sudhanshu.tva.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sudhanshu.tva.permissions.CollectedContact
import com.sudhanshu.tva.permissions.DeviceDataCollector

/**
 * User picks EXACTLY which contacts to add as People — nothing is
 * auto-imported in bulk. This respects "TVA never accesses anyone else's
 * private data secretly" (Step 6/11): reading the device contact list
 * requires the user's own explicit permission grant, and adding any one
 * of them as a Person still requires an individual tap.
 */
@Composable
fun ContactsPickerScreen(onImport: (List<String>) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val contacts = remember { DeviceDataCollector.readContacts(context) }
    val selected = remember { mutableStateOf(setOf<String>()) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Import from Contacts", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Pick who to add as People. Nothing is imported automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        if (contacts.isEmpty()) {
            Text(
                "No contacts found, or permission not granted.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(contacts) { contact: CollectedContact ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Checkbox(
                            checked = selected.value.contains(contact.name),
                            onCheckedChange = { checked ->
                                selected.value = if (checked) selected.value + contact.name
                                else selected.value - contact.name
                            }
                        )
                        Text(contact.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
        }

        Button(
            onClick = { onImport(selected.value.toList()) },
            enabled = selected.value.isNotEmpty(),
            modifier = Modifier.fillMaxSize().padding(top = 12.dp)
        ) {
            Text("Add ${selected.value.size} as People")
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxSize()) {
            Text("Cancel")
        }
    }
}
