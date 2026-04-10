package com.florian.waypoint

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color

// ── App-wide constants ──────────────────────────────────────────
/** Color for the pin of the currently-selected waypoint on the map. */
val SelectedPinColor = Color(0xFFDC5028)

/** Color for the polyline of a track currently being recorded. */
val RecordingTrackColor = Color(0xFFFF2D55)

/** Wind speed (km/h) at which a "lifts may close" warning is shown in the weather pill. */
const val WindClosureThresholdKmh = 50.0

val LightColors = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    error = Color(0xFFFF3B30),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF3C3C43),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFFC7C7CC),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    error = Color(0xFFFF453A),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFAEAEB2),
    outline = Color(0xFF636366),
    outlineVariant = Color(0xFF3A3A3C),
)

/** High-contrast "glare mode" — bold solid colors for bright snow conditions. */
val GlareColors = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color.White,
    error = Color(0xFFFF0000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF000000),
    outline = Color(0xFF000000),
    outlineVariant = Color(0xFF808080),
)

/** Indicates whether high-contrast glare mode is enabled. */
val LocalGlareMode = compositionLocalOf { false }

/**
 * Root theme wrapper for the app. Applies one of three color schemes:
 *  - [GlareColors] when [glareMode] is on (high-contrast UI for bright snow)
 *  - [DarkColors] when the system is in dark mode
 *  - [LightColors] otherwise
 *
 * Also provides [LocalGlareMode] so nested composables can query the current mode.
 */
@Composable
fun WaypointTheme(glareMode: Boolean = false, content: @Composable () -> Unit) {
    val scheme = when {
        glareMode -> GlareColors
        isSystemInDarkTheme() -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalGlareMode provides glareMode) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/**
 * iOS-style clickable modifier. Replaces Material's ripple effect with a simple
 * opacity fade (1.0 → 0.6) on press, matching Apple's interaction language.
 * Use everywhere in place of [Modifier.clickable] or Material buttons' `onClick`.
 */
@Composable
fun Modifier.iosClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val alpha by animateFloatAsState(if (isPressed) 0.6f else 1f, label = "press")
    return this
        .alpha(alpha)
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}
