package com.sudhanshu.tva.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sudhanshu.tva.network.ChatRequest
import com.sudhanshu.tva.network.RelayClient
import kotlinx.coroutines.launch

private data class ChatUiMessage(val role: String, val text: String)

@Composable
fun AIChatScreen() {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val messages = remember { mutableStateListOf<ChatUiMessage>() }

    LaunchedEffect(Unit) {
        try {
            val res = RelayClient.api.chatHistory()
            if (res.isSuccessful) {
                messages.clear()
                messages.addAll(
                    res.body()?.messages.orEmpty().map { ChatUiMessage(it.role, it.content) }
                )
            }
        } catch (_: Exception) { }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("TVA AI Chat", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Conversation-aware assistant using your TVA timeline and synced device signals as context.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            reverseLayout = false
        ) {
            items(messages) { message ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(if (message.role == "user") "YOU" else "TVA",
                            style = MaterialTheme.typography.labelMedium)
                        Text(message.text, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(4.dp))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Message") },
                modifier = Modifier.weight(1f),
                enabled = !sending,
                singleLine = false
            )
            Button(
                enabled = input.isNotBlank() && !sending,
                onClick = {
                    val text = input.trim()
                    input = ""
                    messages += ChatUiMessage("user", text)
                    scope.launch {
                        sending = true
                        error = null
                        try {
                            val res = RelayClient.api.chat(ChatRequest(text))
                            if (res.isSuccessful && res.body() != null) {
                                messages += ChatUiMessage("assistant", res.body()!!.answer)
                            } else {
                                error = "Relay error ${res.code()}"
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "Chat failed"
                        }
                        sending = false
                    }
                }
            ) { Text("Send") }
        }
    }
}
