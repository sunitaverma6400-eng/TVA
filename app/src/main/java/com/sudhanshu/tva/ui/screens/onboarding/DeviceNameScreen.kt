package com.sudhanshu.tva.ui.screens.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Multi-device support: if this is one of the user's own multiple phones,
 * naming it here (e.g. "Primary Phone", "Backup Phone") tags every event
 * this device syncs with that label, so the shared relay data shows which
 * of their own devices something came from. Purely a label — not a
 * separate account or trust boundary.
 */
@Composable
fun DeviceNameScreen(defaultName: String, onComplete: (String) -> Unit) {
    var name by remember { mutableStateOf(defaultName) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Name this device", style = MaterialTheme.typography.headlineLarge)
        Text(
            "If you use TVA on more than one of your own phones, naming each one helps you tell which device synced what data. Doesn't affect what data you see — all your devices share the same timeline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Device name") },
            modifier = Modifier.fillMaxSize().padding(bottom = 20.dp)
        )

        Button(
            onClick = { onComplete(name.trim().ifBlank { defaultName }) },
            modifier = Modifier.fillMaxSize()
        ) {
            Text("Continue")
        }
    }
}
