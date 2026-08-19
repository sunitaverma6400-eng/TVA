package com.sudhanshu.tva.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sudhanshu.tva.network.AnomalyDto
import com.sudhanshu.tva.network.RelayClient
import com.sudhanshu.tva.ui.theme.TvaAlertRed
import com.sudhanshu.tva.ui.theme.TvaAmber
import kotlinx.coroutines.launch

/**
 * Step 15: Temporal anomaly detector. Rule-based, not AI — conflicting
 * locations, duplicate people, low-confidence data clusters. "TEMPORAL
 * ANOMALY DETECTED" per the blueprint's control-room aesthetic.
 */
@Composable
fun AnomaliesScreen() {
    var anomalies by remember { mutableStateOf<List<AnomalyDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var scanning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            loading = true
            val res = RelayClient.api.listAnomalies(resolved = "false")
            if (res.isSuccessful) anomalies = res.body()?.anomalies ?: emptyList()
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Anomalies", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Rule-based checks for data inconsistencies",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Button(
            onClick = {
                scope.launch {
                    scanning = true
                    try {
                        RelayClient.api.scanAnomalies()
                        load()
                    } finally {
                        scanning = false
                    }
                }
            },
            enabled = !scanning,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(if (scanning) "Scanning..." else "Scan for Anomalies")
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (anomalies.isEmpty()) {
            Text(
                "TEMPORAL STABILITY: no unresolved anomalies.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        } else {
            LazyColumn {
                items(anomalies) { anomaly ->
                    Card(modifier = Modifier.fillMaxSize().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "⚠ TEMPORAL ANOMALY DETECTED",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (anomaly.severity == "high") TvaAlertRed else TvaAmber
                            )
                            Text(
                                anomaly.anomaly_type.replace("_", " "),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(anomaly.description, style = MaterialTheme.typography.bodyMedium)
                            Button(
                                onClick = {
                                    scope.launch {
                                        RelayClient.api.resolveAnomaly(anomaly.id)
                                        load()
                                    }
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Mark Resolved")
                            }
                        }
                    }
                }
            }
        }
    }
}
