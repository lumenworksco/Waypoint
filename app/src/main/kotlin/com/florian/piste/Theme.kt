package com.florian.piste

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// ── App-wide constants ──────────────────────────────────────────
/** Color for the pin of the currently-selected waypoint on the map. */
val SelectedPinColor = Color(0xFFDC5028)

/** Color for the polyline of a track currently being recorded. */
val RecordingTrackColor = Color(0xFFFF2D55)

/** Wind speed (km/h) at which a "lifts may close" warning is shown in the weather pill. */
const val WindClosureThresholdKmh = 50.0

// ── Typography ──────────────────────────────────────────────────
/**
 * Rounded sans-serif font family — Nunito, bundled as a variable TTF in
 * `res/font/nunito.ttf`. Nunito is the closest free equivalent to Apple's
 * SF Pro Rounded; its rounded terminals give every heading and body style
 * the friendly, unmistakably-Cupertino shape we want.
 *
 * A single TTF is declared for every weight; the variable font handles
 * each axis internally so Compose still gets the right glyph shape when
 * it asks for [FontWeight.W500] vs [FontWeight.W700].
 */
val NunitoFamily = FontFamily(
    Font(R.font.nunito, FontWeight.W400),
    Font(R.font.nunito, FontWeight.W500),
    Font(R.font.nunito, FontWeight.W600),
    Font(R.font.nunito, FontWeight.W700),
)

/** Material3 [Typography] with Nunito applied to every role. */
val PisteTypography: Typography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = NunitoFamily),
        displayMedium = base.displayMedium.copy(fontFamily = NunitoFamily),
        displaySmall = base.displaySmall.copy(fontFamily = NunitoFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = NunitoFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = NunitoFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = NunitoFamily),
        titleLarge = base.titleLarge.copy(fontFamily = NunitoFamily),
        titleMedium = base.titleMedium.copy(fontFamily = NunitoFamily),
        titleSmall = base.titleSmall.copy(fontFamily = NunitoFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = NunitoFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = NunitoFamily),
        bodySmall = base.bodySmall.copy(fontFamily = NunitoFamily),
        labelLarge = base.labelLarge.copy(fontFamily = NunitoFamily),
        labelMedium = base.labelMedium.copy(fontFamily = NunitoFamily),
        labelSmall = base.labelSmall.copy(fontFamily = NunitoFamily),
    )
}

// ── Colors ──────────────────────────────────────────────────────
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
 * All themes use [PisteTypography] — Nunito rounded — so the entire UI
 * speaks with a single friendly voice.
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
        MaterialTheme(colorScheme = scheme, typography = PisteTypography, content = content)
    }
}

/**
 * iOS-style clickable modifier. Replaces Material's ripple effect with a simple
 * opacity fade (1.0 → 0.7) on press, matching Apple's interaction language.
 * Use everywhere in place of [Modifier.clickable] or Material buttons' `onClick`.
 */
@Composable
fun Modifier.iosClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val alpha by animateFloatAsState(if (isPressed) 0.7f else 1f, label = "press")
    return this
        .alpha(alpha)
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}

/**
 * iOS-style clickable with long-press support. Same fade behaviour as
 * [iosClickable] but also calls [onLongPress] when the press lasts past the
 * platform threshold. Used for list rows that need to reveal a context menu
 * on long-press, mirroring iPhone Home Screen interaction.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.iosClickable(onClick: () -> Unit, onLongPress: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val alpha by animateFloatAsState(if (isPressed) 0.7f else 1f, label = "press")
    return this
        .alpha(alpha)
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = onLongPress,
        )
}

// ── Reusable animation specs ───────────────────────────────────
/** Spring spec reused across the app for press feedback and sheet transitions. */
val PisteSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 300f)

/**
 * iOS-style clickable for primary CTAs (Record, Share, etc.).
 * Combines an opacity fade (1.0 → 0.7) with a subtle scale-down (→ 0.97)
 * on press, giving buttons a tactile "push" feel via [graphicsLayer].
 */
@Composable
fun Modifier.iosScaleClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val alpha by animateFloatAsState(if (isPressed) 0.7f else 1f, label = "press")
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "scale"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .alpha(alpha)
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}
