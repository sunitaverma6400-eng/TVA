package com.sudhanshu.tva.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Generic "this section isn't built yet" screen so navigation can be fully
 * wired and tested before each section gets its real implementation
 * in a later step.
 */
@Composable
fun PlaceholderScreen(title: String, stepNote: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.headlineLarge)
            Text(
                text = stepNote,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
