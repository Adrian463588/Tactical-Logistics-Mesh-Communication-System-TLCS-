package com.example.tclszero.presentation.theme


import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TacticalColors.ReticleGreen,
    secondary = TacticalColors.WarningOrange,
    tertiary = TacticalColors.DarkGreen,
    error = TacticalColors.BrightRed,
    background = TacticalColors.DarkGreen,
    surface = TacticalColors.TacticalGray
)

@Composable
fun TlcsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
