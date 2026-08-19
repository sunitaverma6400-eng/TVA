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
 * Step 6: collects the basic-profile fields (the one required consent
 * category). Runs after ConsentScreen, before the user reaches Control Room.
 */
@Composable
fun ProfileSetupScreen(onComplete: (name: String, dob: String, bio: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Your profile", style = MaterialTheme.typography.headlineLarge)
        Text(
            "This stays on your device only, for now.",
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
        OutlinedTextField(
            value = dob,
            onValueChange = { dob = it },
            label = { Text("Date of birth (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxSize().padding(bottom = 12.dp)
        )
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Short bio (optional)") },
            modifier = Modifier.fillMaxSize().padding(bottom = 20.dp)
        )

        Button(
            onClick = { onComplete(name.trim(), dob.trim(), bio.trim()) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxSize()
        ) {
            Text("Enter TVA")
        }
    }
}
