package com.sudhanshu.tva.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sudhanshu.tva.network.RelayClient
import com.sudhanshu.tva.network.TemporalSearchRequest
import com.sudhanshu.tva.network.TemporalSearchResultDto
import kotlinx.coroutines.launch

private sealed class SearchState {
    data object Idle : SearchState()
    data object Loading : SearchState()
    data class Done(val result: TemporalSearchResultDto) : SearchState()
    data class Error(val message: String) : SearchState()
}

private val exampleQueries = listOf(
    "What major changes happened in my life recently?",
    "Who have I spent the most time with?",
    "What patterns do you see in my timeline?"
)

/**
 * Step 16: natural-language Q&A over the user's own timeline/people/branches
 * data. The AI only sees what's already in the database (Steps 8-12) —
 * nothing pulled live from the device here.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemporalSearchScreen() {
    var query by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<SearchState>(SearchState.Idle) }
    val scope = rememberCoroutineScope()

    fun search(q: String) {
        if (q.isBlank()) return
        scope.launch {
            state = SearchState.Loading
            state = try {
                val res = RelayClient.api.temporalSearch(TemporalSearchRequest(q))
                if (res.isSuccessful && res.body() != null) {
                    SearchState.Done(res.body()!!)
                } else {
                    SearchState.Error("Relay error ${res.code()}")
                }
            } catch (e: Exception) {
                SearchState.Error(e.message ?: "Unreachable")
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("Temporal Search", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Ask about your own timeline, people, and branches",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Ask a question") },
            modifier = Modifier.fillMaxSize().padding(bottom = 8.dp)
        )

        FlowRow(modifier = Modifier.padding(bottom = 16.dp)) {
            exampleQueries.forEach { example ->
                AssistChip(
                    onClick = { query = example; search(example) },
                    label = { Text(example, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.padding(end = 6.dp, bottom = 6.dp)
                )
            }
        }

        Button(
            onClick = { search(query) },
            enabled = query.isNotBlank() && state !is SearchState.Loading,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("Search")
        }

        when (val s = state) {
            is SearchState.Idle -> Text(
                "Try a question above, or tap an example.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
            is SearchState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is SearchState.Error -> Text(
                "Search failed: ${s.message}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            is SearchState.Done -> Card(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(s.result.answer, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "\nvia ${s.result.provider_used ?: "unknown"} · answered from your own data only",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}
