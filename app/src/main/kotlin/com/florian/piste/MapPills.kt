package com.florian.piste

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Weather pill ────────────────────────────────────────────────
@Composable
internal fun WeatherPill(text: String, warning: Boolean = false, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .then(if (warning) Modifier.iosGlassTinted(MaterialTheme.colorScheme.error)
                  else Modifier.iosGlass())
            .semantics { contentDescription = text }
    ) {
        Text(
            text,
            modifier = Modifier.iosClickable(onClick).padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.W500,
            color = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Daylight pill ───────────────────────────────────────────────
/**
 * Shown when sunset is within 90 minutes. Turns red when under 30 minutes left
 * to warn the skier that it's time to head down.
 */
@Composable
internal fun DaylightPill(minutesLeft: Int) {
    val urgent = minutesLeft <= 30
    Box(
        modifier = Modifier
            .then(if (urgent) Modifier.iosGlassTinted(MaterialTheme.colorScheme.error)
                  else Modifier.iosGlass())
            .semantics { contentDescription = "$minutesLeft minutes of daylight remaining" }
    ) {
        Text(
            "\u2600\uFE0F ${formatMinutesRemaining(minutesLeft)} of daylight",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.W500,
            color = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Night skiing pill ──────────────────────────────────────────
/**
 * Shown when the user is still recording after sunset (minutesToSunset < 0),
 * or as a suggestion to switch to dark map tiles. Tapping it switches to dark mode.
 */
@Composable
internal fun NightSkiingPill(onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .iosGlass()
            .semantics { contentDescription = "Night skiing mode active" }
    ) {
        Text(
            "\uD83C\uDF19 Night skiing \u2014 tap for dark map",
            modifier = Modifier.iosClickable(onClick).padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.W500,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Avalanche awareness pill ───────────────────────────────────
/**
 * Shown when the current weather forecast contains at least one elevated-risk
 * signal (heavy new snow, wind-loading, or rapid temperature swing). Tapping it
 * opens a sheet explaining the reasoning and linking to the official bulletin.
 *
 * Deliberately worded as "awareness" not "forecast" -- we never fake a danger rating.
 */
@Composable
internal fun AwarenessPill(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .iosGlassTinted(MaterialTheme.colorScheme.error)
            .semantics { contentDescription = "Avalanche awareness warning, tap for details" }
    ) {
        Text(
            "\u26A0\uFE0F Avalanche awareness",
            modifier = Modifier.iosClickable(onClick).padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.W500,
            color = MaterialTheme.colorScheme.error
        )
    }
}

// ── Home arrow pill ─────────────────────────────────────────────
@Composable
internal fun HomeArrowPill(distanceText: String, bearing: Float, onClick: () -> Unit) {
    val arrowColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .iosGlass()
    ) {
        Row(
            modifier = Modifier
                .iosClickable(onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics { contentDescription = "Home waypoint $distanceText away" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("\uD83C\uDFE0", fontSize = 13.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Canvas(modifier = Modifier.size(14.dp).rotate(bearing)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx, 0f)
                    lineTo(cx + 4.dp.toPx(), size.height)
                    lineTo(cx, size.height - 3.dp.toPx())
                    lineTo(cx - 4.dp.toPx(), size.height)
                    close()
                }
                drawPath(path, arrowColor)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                distanceText,
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
