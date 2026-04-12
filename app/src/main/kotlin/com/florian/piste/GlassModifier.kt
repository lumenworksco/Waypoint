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
 * Uses a fully opaque surface base with a subtle top-highlight gradient and
 * a hairline border to create the appearance of frosted glass. No actual blur
 * is applied (Compose lacks a backdrop-blur API), but the effect is convincing
 * when placed over busy content like map tiles.
 *
 * The background is intentionally opaque to avoid artifacts with elevation
 * shadows — Material3's Surface elevation tinting draws a visible rectangle
 * behind semi-transparent backgrounds.
 */
@Composable
fun Modifier.iosGlass(
    cornerRadius: Dp = 20.dp,
): Modifier = composed {
    val shape: Shape = RoundedCornerShape(cornerRadius)
    val surface = MaterialTheme.colorScheme.surface
    val borderColor = Color.White.copy(alpha = 0.18f)

    this
        .clip(shape)
        .background(surface, shape)
        .background(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.10f),
                0.5f to Color.White.copy(alpha = 0.03f),
                1f to Color.Transparent
            ),
            shape
        )
        .border(width = 0.5.dp, color = borderColor, shape = shape)
}

/**
 * Tinted glass for attention-grabbing pills (warning, error states).
 * Uses a tinted overlay on an opaque surface base.
 */
@Composable
fun Modifier.iosGlassTinted(
    tint: Color,
    cornerRadius: Dp = 20.dp,
): Modifier = composed {
    val shape: Shape = RoundedCornerShape(cornerRadius)
    val surface = MaterialTheme.colorScheme.surface
    val borderColor = tint.copy(alpha = 0.30f)

    this
        .clip(shape)
        .background(surface, shape)
        .background(tint.copy(alpha = 0.12f), shape)
        .background(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.12f),
                0.5f to Color.White.copy(alpha = 0.03f),
                1f to Color.Transparent
            ),
            shape
        )
        .border(width = 0.5.dp, color = borderColor, shape = shape)
}
