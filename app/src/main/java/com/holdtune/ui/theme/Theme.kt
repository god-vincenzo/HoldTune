package com.holdtune.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    secondary = SecondaryTeal,
    background = DarkGray,
    surface = CardGray,
    onPrimary = TextWhite,
    onSecondary = DarkGray,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun HoldTuneTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
