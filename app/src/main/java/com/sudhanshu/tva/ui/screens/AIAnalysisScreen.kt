package com.sudhanshu.tva.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.sudhanshu.tva.network.CalibrationDto
import com.sudhanshu.tva.network.FuturePredictionDto
import com.sudhanshu.tva.network.RecordOutcomeRequest
import com.sudhanshu.tva.network.RelayClient
import com.sudhanshu.tva.network.StructuredAnalysisDto
import com.sudhanshu.tva.ui.theme.TvaAlertRed
import com.sudhanshu.tva.ui.theme.TvaTeal
import kotlinx.coroutines.launch

private sealed class ResultState<T> {
    class Idle<T> : ResultState<T>()
    class Loading<T> : ResultState<T>()
    data class Done<T>(val result: T) : ResultState<T>()
    data class Error<T>(val message: String) : ResultState<T>()
}

/**
 * Step 13/14 (extended with Prediction Ledger + calibration, brought in
 * after reviewing an alternate implementation's approach):
 *
 * - Every AI response keeps OBSERVATIONS (directly supported by data),
 *   INFERENCES (weaker, pattern-based reading), and SCENARIOS (speculative
 *   futures) visually separate — never blended into one confident paragraph.
 * - Every prediction is saved to a ledger and can later be marked with an
 *   outcome (supported/partially_supported/not_supported/inconclusive).
 * - Calibration shows the Brier score across resolved predictions — an
 *   honest measure of whether the AI's confidence has actually tracked reality.
 */
@Composable
fun AIAnalysisScreen() {
    var analysisState by remember { mutableStateOf<ResultState<StructuredAnalysisDto>>(ResultState.Idle()) }
    var predictionState by remember { mutableStateOf<ResultState<StructuredAnalysisDto>>(ResultState.Idle()) }
    var predictions by remember { mutableStateOf<List<FuturePredictionDto>>(emptyList()) }
    var calibration by remember { mutableStateOf<CalibrationDto?>(null) }
    val scope = rememberCoroutineScope()

    fun loadLedger() {
        scope.launch {
            val res = RelayClient.api.listPredictions(limit = 10)
            if (res.isSuccessful) predictions = res.body()?.predictions ?: emptyList()
            val cal = RelayClient.api.getCalibration()
            if (cal.isSuccessful) calibration = cal.body()
        }
    }

    LaunchedEffect(Unit) { loadLedger() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("AI Analysis", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Gemini + Groq reasoning over your primary timeline",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        Button(
            onClick = {
                scope.launch {
                    analysisState = ResultState.Loading()
                    analysisState = try {
                        val res = RelayClient.api.analyzeTimeline()
                        if (res.isSuccessful && res.body() != null) ResultState.Done(res.body()!!)
                        else ResultState.Error("Relay error ${res.code()}")
                    } catch (e: Exception) {
                        ResultState.Error(e.message ?: "Unreachable")
                    }
                }
            },
            enabled = analysisState !is ResultState.Loading,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("Analyze My Timeline")
        }

        StructuredResultView(analysisState)

        Text(
            "\nFuture Probability",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 32.dp, bottom = 4.dp)
        )
        Text(
            "Pattern-based speculation, never a guaranteed outcome.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                scope.launch {
                    predictionState = ResultState.Loading()
                    predictionState = try {
                        val res = RelayClient.api.predictFuture()
                        if (res.isSuccessful && res.body() != null) ResultState.Done(res.body()!!)
                        else ResultState.Error("Relay error ${res.code()}")
                    } catch (e: Exception) {
                        ResultState.Error(e.message ?: "Unreachable")
                    }
                    loadLedger()
                }
            },
            enabled = predictionState !is ResultState.Loading,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("Predict Possible Futures")
        }

        StructuredResultView(predictionState)

        // --- Prediction Ledger ---
        Text(
            "\nPrediction Ledger",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 32.dp, bottom = 4.dp)
        )
        Text(
            "Past predictions never get rewritten — mark what actually happened to build a calibration record.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        calibration?.let { cal ->
            Card(modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (cal.count == 0) {
                        Text(cal.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                    } else {
                        Text(
                            "Calibration — ${cal.count} resolved predictions",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Brier score: ${String.format("%.3f", cal.brier_score)} (lower = better calibrated)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Supported rate: ${((cal.supported_rate ?: 0.0) * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        predictions.forEach { prediction ->
            Card(modifier = Modifier.fillMaxSize().padding(bottom = 8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(prediction.context_summary.take(80), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Status: ${prediction.status}" + (prediction.horizon?.let { " · horizon: $it" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (prediction.status == "pending") MaterialTheme.colorScheme.tertiary else TvaTeal
                    )
                    if (prediction.status == "pending") {
                        Row(modifier = Modifier.padding(top = 8.dp)) {
                            listOf("supported", "not_supported", "inconclusive").forEach { statusOption ->
                                TextButton(onClick = {
                                    scope.launch {
                                        RelayClient.api.recordPredictionOutcome(
                                            prediction.id,
                                            RecordOutcomeRequest(status = statusOption)
                                        )
                                        loadLedger()
                                    }
                                }) {
                                    Text(statusOption.replace("_", " "), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StructuredResultView(state: ResultState<StructuredAnalysisDto>) {
    when (state) {
        is ResultState.Idle -> Text(
            "Tap the button above. This calls the AI relay, so it can take a few seconds.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary
        )
        is ResultState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is ResultState.Error -> Text(
            "Failed: ${state.message}",
            style = MaterialTheme.typography.bodyMedium,
            color = TvaAlertRed
        )
        is ResultState.Done -> {
            val r = state.result
            Card(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (r.observations.isNotEmpty()) {
                        SectionLabel("OBSERVATIONS — directly supported by your data")
                        r.observations.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                    }
                    if (r.inferences.isNotEmpty()) {
                        SectionLabel("INFERENCES — patterns read into the data, weaker claim")
                        r.inferences.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                    }
                    if (r.scenarios.isNotEmpty()) {
                        SectionLabel("SCENARIOS")
                        r.scenarios.forEach { s ->
                            Text("${s.label} — ${s.probability.toInt()}%", style = MaterialTheme.typography.titleLarge)
                            Text(s.reasoning, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (r.uncertainty.isNotBlank()) {
                        SectionLabel("UNCERTAINTY")
                        Text(r.uncertainty, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (r.data_gaps.isNotEmpty()) {
                        SectionLabel("WHAT WOULD HELP")
                        r.data_gaps.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                    }
                    Text(
                        "\nvia ${r.provider_used ?: "unknown"}" + (r.event_count?.let { " · $it events analyzed" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}
