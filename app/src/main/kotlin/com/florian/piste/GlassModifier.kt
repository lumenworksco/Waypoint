package com.florian.piste

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apple-style glass background for pills, buttons, and sheets.
 *
 * Compose (as of 2026) still lacks a true backdrop-blur API without pulling
 * in external libraries like Haze. This modifier does the next best thing:
 * stacks a tinted base color, a vertical highlight gradient (brighter at the
 * top), and a hairline inner border — the same three layers Apple uses for
 * the .ultraThinMaterial look.
 *
 * Usage:
 *   Box(modifier = Modifier.iosGlass(cornerRadius = 20.dp)) { ... }
 *
 * Caveats:
 *   - Works on any Android version (no API checks needed)
 *   - Not an actual gaussian blur, but perceptually very close when used
 *     against busy content like map tiles
 *   - The highlight is tuned for light mode; in dark mode it's a touch
 *     subtler automatically because the base surface color is already dark
 */
@Composable
fun Modifier.iosGlass(
    cornerRadius: Dp = 20.dp,
    tintAlpha: Float = 0.88f,
): Modifier = composed {
    val shape: Shape = RoundedCornerShape(cornerRadius)
    val surface = MaterialTheme.colorScheme.surface
    val base = surface.copy(alpha = tintAlpha)
    val highlight = Color.White.copy(alpha = 0.15f)
    val innerShadow = Color.White.copy(alpha = 0.08f)
    val borderColor = Color.White.copy(alpha = 0.25f)

    this
        .clip(shape)
        .background(base, shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(highlight, Color.Transparent),
                startY = 0f,
                endY = 48f
            ),
            shape
        )
        // Subtle inner shadow gradient for extra depth
        .background(
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, innerShadow),
                startY = 0f,
                endY = 80f
            ),
            shape
        )
        .border(width = 0.5.dp, color = borderColor, shape = shape)
}

/**
 * Tinted glass for attention-grabbing pills (warning, error states).
 * Same three-layer approach but with the caller's color as the base tint.
 */
@Composable
fun Modifier.iosGlassTinted(
    tint: Color,
    cornerRadius: Dp = 20.dp,
): Modifier = composed {
    val shape: Shape = RoundedCornerShape(cornerRadius)
    val base = tint.copy(alpha = 0.15f)
    val highlight = Color.White.copy(alpha = 0.18f)
    val borderColor = tint.copy(alpha = 0.35f)

    this
        .clip(shape)
        .background(base, shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(highlight, Color.Transparent),
                startY = 0f,
                endY = 40f
            ),
            shape
        )
        .border(width = 0.5.dp, color = borderColor, shape = shape)
}
