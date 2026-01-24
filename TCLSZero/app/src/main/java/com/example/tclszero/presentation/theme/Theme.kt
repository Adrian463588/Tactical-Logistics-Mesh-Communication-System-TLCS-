package com.example.tclszero.presentation.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * TLCS Zero Theme
 * 
 * High-Contrast Light Mode optimized for:
 * - Outdoor visibility in bright sunlight
 * - Quick visual recognition in high-stress tactical environments
 * - Clean, no-gradient aesthetics
 */

// ═══════════════════════════════════════════════════════════════════════════════
// LIGHT COLOR SCHEME (High-Contrast Daytime Tactical)
// ═══════════════════════════════════════════════════════════════════════════════

private val TlcsLightColorScheme = lightColorScheme(
    // Primary - Black for maximum contrast
    primary = CoreColors.Primary,
    onPrimary = CoreColors.OnPrimary,
    primaryContainer = CoreColors.SurfaceVariant,
    onPrimaryContainer = CoreColors.Primary,
    
    // Secondary - White
    secondary = CoreColors.Secondary,
    onSecondary = CoreColors.OnSecondary,
    secondaryContainer = CoreColors.SurfaceVariant,
    onSecondaryContainer = CoreColors.OnSurface,
    
    // Tertiary - Logistics Blue accent
    tertiary = LogisticsColors.Primary,
    onTertiary = CoreColors.OnPrimary,
    tertiaryContainer = LogisticsColors.Surface,
    onTertiaryContainer = LogisticsColors.OnSurface,
    
    // Background & Surface
    background = CoreColors.Background,
    onBackground = CoreColors.OnBackground,
    surface = CoreColors.Surface,
    onSurface = CoreColors.OnSurface,
    surfaceVariant = CoreColors.SurfaceVariant,
    onSurfaceVariant = CoreColors.OnSurfaceVariant,
    
    // Outline
    outline = CoreColors.Outline,
    outlineVariant = CoreColors.OutlineVariant,
    
    // Error
    error = CoreColors.Error,
    onError = CoreColors.OnError,
    errorContainer = AlertColors.Surface,
    onErrorContainer = AlertColors.OnSurface,
    
    // Inverse (for snackbars)
    inverseSurface = CoreColors.Primary,
    inverseOnSurface = CoreColors.OnPrimary,
    inversePrimary = CoreColors.SurfaceVariant,
    
    // Scrim
    scrim = CoreColors.Primary.copy(alpha = 0.32f)
)

// ═══════════════════════════════════════════════════════════════════════════════
// THEME COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun TlcsTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = TlcsLightColorScheme  // Always use light mode for tactical visibility
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Black status bar for high contrast
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TlcsTypography,
        content = content
    )
}
