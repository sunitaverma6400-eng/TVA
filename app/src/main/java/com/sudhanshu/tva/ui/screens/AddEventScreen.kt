package com.sudhanshu.tva.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sudhanshu.tva.util.TimeFormat

/**
 * Step 9: manual event entry. Later steps (calendar auto-import already
 * exists from Step 7, AI-inferred events later) add other sources — this
 * is the "source": "manual" path a user can always fall back to.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(onSave: (title: String, eventType: String, epochSeconds: Double, location: String, people: List<String>) -> Unit, onCancel: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf("general") }
    var location by remember { mutableStateOf("") }
    var peopleInput by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var selectedEpochMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Add Event", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Manually add something to your timeline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxSize().padding(bottom = 12.dp)
        )
        OutlinedTextField(
            value = eventType,
            onValueChange = { eventType = it },
            label = { Text("Type (e.g. career, personal, travel)") },
            modifier = Modifier.fillMaxSize().padding(bottom = 12.dp)
        )
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location (optional)") },
            modifier = Modifier.fillMaxSize().padding(bottom = 12.dp)
        )
        OutlinedTextField(
            value = peopleInput,
            onValueChange = { peopleInput = it },
            label = { Text("People (comma-separated, optional)") },
            modifier = Modifier.fillMaxSize().padding(bottom = 12.dp)
        )

        Text(
            "Date: ${TimeFormat.day(selectedEpochMillis)}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TextButton(onClick = { showDatePicker = true }) {
            Text("Change date")
        }

        Column(modifier = Modifier.fillMaxSize().padding(top = 20.dp)) {
            Button(
                onClick = {
                    val people = peopleInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onSave(title.trim(), eventType.trim().ifBlank { "general" }, selectedEpochMillis / 1000.0, location.trim(), people)
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxSize()
            ) {
                Text("Save Event")
            }
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxSize()) {
                Text("Cancel")
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedEpochMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
