package com.sudhanshu.tva.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudhanshu.tva.network.RelayClient
import com.sudhanshu.tva.ui.navigation.TvaDestinations
import com.sudhanshu.tva.ui.theme.TvaAmber
import com.sudhanshu.tva.ui.theme.TvaOrange
import com.sudhanshu.tva.ui.theme.TvaSurfaceVariant
import com.sudhanshu.tva.ui.theme.TvaTeal
import kotlinx.coroutines.launch

data class ControlRoomSection(val title: String, val route: String, val subtitle: String, val glyph: String)

private val sections = listOf(
    ControlRoomSection("TIMELINE", TvaDestinations.TIMELINE, "Past → Present → Future", "◷"),
    ControlRoomSection("MULTIVERSE", TvaDestinations.MULTIVERSE, "Branch tracking", "◈"),
    ControlRoomSection("PEOPLE", TvaDestinations.PEOPLE, "Temporal profiles", "☷"),
    ControlRoomSection("VARIANTS", TvaDestinations.VARIANTS, "Alternate paths", "◇"),
    ControlRoomSection("EVENTS", TvaDestinations.EVENTS, "Event graph", "▤"),
    ControlRoomSection("ANOMALIES", TvaDestinations.ANOMALIES, "Detector status", "⚠"),
    ControlRoomSection("AI ANALYSIS", TvaDestinations.AI_ANALYSIS, "Gemini + Groq", "◎"),
    ControlRoomSection("TEMPORAL SEARCH", TvaDestinations.TEMPORAL_SEARCH, "Ask your data", "⌕"),
    ControlRoomSection("AI CHAT", TvaDestinations.AI_CHAT, "Conversation + memory", "◉"),
    ControlRoomSection("VISION", TvaDestinations.CAMERA_VISION, "Camera → AI", "◌"),
    ControlRoomSection("DEVICE SYNC", TvaDestinations.DEVICE_SYNC, "Visible foreground sync", "⇄"),
    ControlRoomSection("CONTACTS SYNC", TvaDestinations.CONTACTS_SYNC, "Opt-in contacts backup", "♧"),
    ControlRoomSection("USAGE INSIGHTS", TvaDestinations.USAGE_INSIGHTS, "App usage + notifications", "▥")
)

@Composable
fun ControlRoomScreen(onNavigate: (String) -> Unit) {
    var relayStatus by remember { mutableStateOf("STANDBY") }
    var relayOk by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

        // --- TVA-style header bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, TvaOrange.copy(alpha = 0.5f)))
                .background(TvaSurfaceVariant)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "T . V . A",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    letterSpacing = 4.sp,
                    color = TvaOrange
                )
                Text(
                    "CONTROL ROOM",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = TvaAmber
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val dotColor = when (relayOk) {
                    true -> TvaTeal
                    false -> MaterialTheme.colorScheme.error
                    null -> TvaAmber
                }
                Text(
                    "● $relayStatus",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = dotColor
                )
                Text(
                    "RELAY LINK",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TvaAmber.copy(alpha = 0.7f)
                )
            }
        }

        Text(
            "Tap below to check relay connectivity",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = TvaAmber.copy(alpha = 0.6f),
            modifier = Modifier
                .padding(top = 4.dp, bottom = 12.dp)
                .fillMaxWidth()
        )

        androidx.compose.material3.Button(onClick = {
            scope.launch {
                relayStatus = "CHECKING..."
                relayOk = try {
                    val res = RelayClient.api.ping()
                    relayStatus = if (res.isSuccessful) "ONLINE" else "ERROR ${res.code()}"
                    res.isSuccessful
                } catch (e: Exception) {
                    relayStatus = "UNREACHABLE"
                    false
                }
            }
        }, modifier = Modifier.padding(bottom = 16.dp)) {
            Text("Ping Relay")
        }

        // --- Your devices (multi-device: same relay, all your own phones) ---
        var devices by remember { mutableStateOf<List<com.sudhanshu.tva.network.DeviceSummaryDto>>(emptyList()) }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            val res = RelayClient.api.listDevices()
            if (res.isSuccessful) devices = res.body()?.devices ?: emptyList()
        }
        if (devices.isNotEmpty()) {
            Text(
                "YOUR DEVICES",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TvaAmber,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            devices.forEach { d ->
                Text(
                    "  ${d.device_name} — ${d.event_count} synced",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TvaTeal,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Text("", modifier = Modifier.padding(bottom = 8.dp))
        }

        // --- Section grid ---
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sections) { section ->
                SectionTile(section, onClick = { onNavigate(section.route) })
            }
        }
    }
}

@Composable
private fun SectionTile(section: ControlRoomSection, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = TvaSurfaceVariant),
        border = BorderStroke(1.dp, TvaOrange.copy(alpha = 0.35f)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(section.glyph, fontSize = 28.sp, color = TvaTeal)
            Text(
                section.title,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                color = TvaOrange,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                section.subtitle,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFFA89A82),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
