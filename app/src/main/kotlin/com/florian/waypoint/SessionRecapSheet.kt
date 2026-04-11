package com.florian.waypoint

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Auto-shown after a recording of 45+ minutes ends. A single celebratory card
 * with the headline numbers, a "Nice day!" tagline, and a prominent Share
 * button that hands off to [shareTrackImage] to render the PNG card and open
 * the system share sheet.
 *
 * This is what turns the hidden ShareImage feature into a moment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionRecapSheet(
    track: Track,
    imperial: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val stats = remember(track) { computeTrackStats(track.points) }
    val tos = remember(track) { computeTimeOnSnow(track.points) }

    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { DragHandle() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tagline based on the numbers
            val tagline = taglineFor(stats)
            Text("\uD83C\uDFBF", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                tagline,
                fontSize = 22.sp,
                fontWeight = FontWeight.W700,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Here's your day on the slopes",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Headline numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RecapStat("Runs", stats.runCount.toString())
                RecapStat("Vertical", formatVertical(stats.verticalDescended ?: 0.0, imperial))
                RecapStat("Max Speed", formatTrackSpeed(stats.maxSpeedKmh))
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RecapStat("Distance", formatDistance(stats.distanceMeters, imperial).replace(" away", ""))
                RecapStat("Time", formatDuration(stats.durationMs))
                RecapStat("On Snow", "${tos.snowPercent}%")
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Prominent share button — the whole point of this sheet
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .iosClickable {
                            shareTrackImage(context, track, imperial)
                        }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.IosShare,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Share Your Day",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W600,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                "Dismiss",
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .iosClickable(onDismiss)
                    .padding(vertical = 10.dp, horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun RecapStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.W700, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline)
    }
}

/** Pick an upbeat headline based on the session's numbers. */
private fun taglineFor(stats: TrackStatistics): String {
    val vert = stats.verticalDescended ?: 0.0
    val runs = stats.runCount
    val maxKmh = stats.maxSpeedKmh
    return when {
        vert >= 4000 -> "Epic day!"
        runs >= 15 -> "You crushed it!"
        maxKmh >= 80 -> "Speed demon!"
        vert >= 2000 -> "Solid session!"
        runs >= 8 -> "Good laps!"
        else -> "Nice day!"
    }
}
