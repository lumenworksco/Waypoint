package com.florian.piste

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
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
    waypoints: List<Waypoint> = emptyList(),
    imperial: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val stats = remember(track) { computeTrackStatsCached(track) }
    val tos = remember(track) { computeTimeOnSnow(track.points) }
    val photos = remember(waypoints) { todaysPhotoPaths(waypoints) }
    var showMemories by remember { mutableStateOf(false) }

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

            // Share to Strava button
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFC4C02), // Strava orange
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .iosClickable {
                            shareToStrava(context, track, imperial)
                        }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("\uD83C\uDFC3", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Share to Strava",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W600,
                        color = Color.White
                    )
                }
            }

            // View Memories button — when today has photo waypoints
            if (photos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .iosClickable { showMemories = true }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.PhotoLibrary,
                            null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "View Memories",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.W600,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
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

    // Memories slideshow — shown on top of the sheet
    if (showMemories) {
        MemoriesOverlay(
            photos = photos,
            stats = stats,
            imperial = imperial,
            onDismiss = { showMemories = false }
        )
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
