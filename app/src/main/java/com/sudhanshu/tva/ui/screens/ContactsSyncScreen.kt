package com.sudhanshu.tva.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sudhanshu.tva.data.DeviceIdentity
import com.sudhanshu.tva.network.ContactSyncItem
import com.sudhanshu.tva.network.ContactsSyncRequest
import com.sudhanshu.tva.network.RelayClient
import com.sudhanshu.tva.permissions.DeviceDataCollector
import com.sudhanshu.tva.permissions.PermissionManager
import kotlinx.coroutines.launch

@Composable
fun ContactsSyncScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Ready") }
    var syncing by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Contacts Sync", style = MaterialTheme.typography.headlineLarge)
        Text(
            "This uploads contact display names and whether each contact has a phone number. " +
                "Use only if you want your own contacts backed up to your TVA relay.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Button(
            enabled = !syncing,
            onClick = {
                scope.launch {
                    syncing = true
                    status = "Reading contacts…"
                    val contacts = DeviceDataCollector.readContacts(context, 1000)
                    if (!PermissionManager.hasContactsPermission(context)) {
                        status = "READ_CONTACTS permission is not granted."
                        syncing = false
                        return@launch
                    }
                    status = "Uploading ${contacts.size} contacts…"
                    try {
                        val response = RelayClient.api.syncContacts(
                            ContactsSyncRequest(
                                DeviceIdentity(context).getDeviceNameOrDefault(),
                                contacts.map { ContactSyncItem(it.name, it.hasPhoneNumber) }
                            )
                        )
                        status = if (response.isSuccessful) {
                            "Synced ${response.body()?.synced ?: contacts.size} contacts."
                        } else "Relay error ${response.code()}"
                    } catch (e: Exception) {
                        status = e.message ?: "Sync failed"
                    }
                    syncing = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Sync contacts now") }
        Text(status, modifier = Modifier.padding(top = 16.dp))
    }
}
