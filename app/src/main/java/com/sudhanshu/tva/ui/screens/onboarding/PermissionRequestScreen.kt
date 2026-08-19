package com.sudhanshu.tva.ui.screens.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sudhanshu.tva.permissions.PermissionManager

/**
 * Step 7: only shows a request for a category the user already consented to
 * in ConsentScreen. If neither deviceSignals nor calendarActivity was
 * consented to, this screen just shows a "Continue" button with nothing to
 * request — it never asks for anything outside what was agreed to.
 */
@Composable
fun PermissionRequestScreen(
    needsCalendarActivity: Boolean,
    needsDeviceSignals: Boolean,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    var calendarGranted by remember {
        mutableStateOf(PermissionManager.hasCalendarPermission(context))
    }
    var contactsGranted by remember {
        mutableStateOf(PermissionManager.hasContactsPermission(context))
    }
    var locationGranted by remember {
        mutableStateOf(PermissionManager.hasLocationPermission(context))
    }
    var usageStatsGranted by remember {
        mutableStateOf(PermissionManager.hasUsageStatsPermission(context))
    }
    var notificationAccessGranted by remember {
        mutableStateOf(PermissionManager.hasNotificationListenerAccess(context))
    }

    val calendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        calendarGranted = PermissionManager.hasCalendarPermission(context)
        contactsGranted = PermissionManager.hasContactsPermission(context)
        locationGranted = PermissionManager.hasLocationPermission(context)
    }
    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        contactsGranted = PermissionManager.hasContactsPermission(context)
    }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        locationGranted = PermissionManager.hasLocationPermission(context)
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("One more thing", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Only categories you agreed to on the previous screen are requested here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
        )

        if (needsCalendarActivity) {
            Text(
                "Calendar: ${if (calendarGranted) "granted" else "not granted"} · " +
                    "Contacts: ${if (contactsGranted) "granted" else "not granted"} · " +
                    "Location: ${if (locationGranted) "granted" else "not granted"}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (!calendarGranted) {
                Button(
                    onClick = {
                        calendarLauncher.launch(
                            arrayOf(android.Manifest.permission.READ_CALENDAR)
                        )
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) { Text("Grant calendar access") }
            }
            if (!contactsGranted) {
                Button(
                    onClick = { contactsLauncher.launch(android.Manifest.permission.READ_CONTACTS) },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) { Text("Grant contacts access") }
            }
            if (!locationGranted) {
                Button(
                    onClick = {
                        locationLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    },
                    modifier = Modifier.padding(bottom = 20.dp)
                ) { Text("Grant location access") }
            }
        }

        if (needsDeviceSignals) {
            Text(
                if (usageStatsGranted) "✅ App usage access granted"
                else "App usage stats — you said yes to this. Android requires granting this in Settings directly.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (!usageStatsGranted) {
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        )
                    },
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Text("Open Settings for usage access")
                }
            }

            Text(
                if (notificationAccessGranted) "✅ Notification insights access granted"
                else "Notification insights — also requires Settings. Only app names + daily counts are read, never message content.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (!notificationAccessGranted) {
                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Text("Open Settings for notification insights")
                }
            }
        }

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Continue")
        }
    }
}
