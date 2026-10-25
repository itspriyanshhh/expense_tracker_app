package com.example.pulseledger.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PulseDarkColorScheme = darkColorScheme(
    primary = DarkEmeraldNeon,
    onPrimary = DarkBackground,
    primaryContainer = DarkEmeraldGlow,
    onPrimaryContainer = DarkEmeraldNeon,

    secondary = DarkGlowingAmber,
    onSecondary = DarkBackground,
    secondaryContainer = DarkAmberGlow,
    onSecondaryContainer = DarkGlowingAmber,

    tertiary = DarkCrimsonCoral,
    onTertiary = DarkBackground,
    tertiaryContainer = DarkCrimsonGlow,
    onTertiaryContainer = DarkCrimsonCoral,

    background = DarkBackground,
    onBackground = DarkTextPrimary,

    surface = DarkCardSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCardSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,

    outline = DarkBorder,
    outlineVariant = DarkBorderFocused,

    error = DarkCrimsonCoral,
    onError = DarkTextPrimary,
    errorContainer = DarkCrimsonGlow,
    onErrorContainer = DarkCrimsonCoral
)

private val PulseLightColorScheme = lightColorScheme(
    primary = LightEmeraldNeon,
    onPrimary = LightCardSurface,
    primaryContainer = LightEmeraldGlow,
    onPrimaryContainer = LightEmeraldNeon,

    secondary = LightGlowingAmber,
    onSecondary = LightCardSurface,
    secondaryContainer = LightAmberGlow,
    onSecondaryContainer = LightGlowingAmber,

    tertiary = LightCrimsonCoral,
    onTertiary = LightCardSurface,
    tertiaryContainer = LightCrimsonGlow,
    onTertiaryContainer = LightCrimsonCoral,

    background = LightBackground,
    onBackground = LightTextPrimary,

    surface = LightCardSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCardSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,

    outline = LightBorder,
    outlineVariant = LightBorderFocused,

    error = LightCrimsonCoral,
    onError = LightCardSurface,
    errorContainer = LightCrimsonGlow,
    onErrorContainer = LightCrimsonCoral
)

object PulseTheme {
    val colors: PulseCustomColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPulseColors.current

    val typography
        @Composable
        @ReadOnlyComposable
        get() = PulseTypography
}

@Composable
fun PulseLedgerTheme(
    darkTheme: Boolean = true, // Defaults to signature Dark Fintech aesthetic
    pulseColors: PulseCustomColors? = null,
    content: @Composable () -> Unit
) {
    val effectiveColors = pulseColors ?: if (darkTheme) DarkPulseCustomColors else LightPulseCustomColors
    val effectiveColorScheme = if (darkTheme) PulseDarkColorScheme else PulseLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val windowInsetsController = WindowCompat.getInsetsController(window, view)
                // Dark background -> light status/nav bar icons (isAppearanceLight = false)
                // Light background -> dark status/nav bar icons (isAppearanceLight = true)
                windowInsetsController.isAppearanceLightStatusBars = !darkTheme
                windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
                window.statusBarColor = if (darkTheme) android.graphics.Color.BLACK else android.graphics.Color.parseColor("#F5F6F1")
                window.navigationBarColor = if (darkTheme) android.graphics.Color.BLACK else android.graphics.Color.parseColor("#F5F6F1")
            }
        }
    }

    CompositionLocalProvider(
        LocalPulseColors provides effectiveColors
    ) {
        MaterialTheme(
            colorScheme = effectiveColorScheme,
            typography = PulseTypography,
            content = content
        )
    }
}
