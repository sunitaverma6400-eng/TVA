package com.sudhanshu.tva.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TvaColorScheme = darkColorScheme(
    primary = TvaOrange,
    secondary = TvaAmber,
    tertiary = TvaTeal,
    background = TvaBackground,
    surface = TvaSurface,
    surfaceVariant = TvaSurfaceVariant,
    onPrimary = TvaBackground,
    onBackground = TvaTextPrimary,
    onSurface = TvaTextPrimary,
    error = TvaAlertRed
)

@Composable
fun TvaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TvaColorScheme,
        typography = TvaTypography,
        content = content
    )
}
