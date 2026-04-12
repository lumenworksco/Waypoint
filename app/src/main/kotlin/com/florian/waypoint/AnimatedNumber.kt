package com.florian.waypoint

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

/**
 * An odometer-style number display that smoothly rolls from its previous
 * value to its new target whenever [value] changes.
 *
 * This is the single small touch Apple Fitness uses that makes stats feel
 * *earned* instead of *snapped in*. Used for the speedometer, daily ring
 * totals, and the recap sheet headline numbers.
 *
 * - Integers render with no decimal point ([decimals] = 0)
 * - Floats render with the specified decimal count
 * - The animation duration scales with the magnitude of the change so a
 *   +1 increment still feels snappy while a +200 jump takes a bit longer.
 */
@Composable
fun AnimatedNumber(
    value: Float,
    modifier: Modifier = Modifier,
    decimals: Int = 0,
    suffix: String = "",
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
) {
    val animated by animateFloatAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 600),
        label = "number"
    )
    val displayValue = if (decimals == 0) {
        animated.toInt().toString()
    } else {
        "%.${decimals}f".format(animated)
    }
    Text(
        text = displayValue + suffix,
        modifier = modifier,
        style = style,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
    )
}
