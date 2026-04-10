package com.florian.waypoint

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyStatsSheet(tracks: List<Track>, imperial: Boolean, onDismiss: () -> Unit) {
    val today = remember(tracks) { filterToday(tracks) }
    val totals = remember(today) { aggregate(today) }

    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { DragHandle() }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Landscape, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Today's Stats", fontWeight = FontWeight.W600, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (today.isEmpty()) {
                Text(
                    "No tracks recorded today. Start recording to see stats here.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BigStat("Runs", totals.runs.toString())
                    BigStat("Vertical", formatVertical(totals.vertical, imperial))
                    BigStat("Max Speed", formatTrackSpeed(totals.maxSpeed))
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BigStat("Distance", formatDistance(totals.distance, imperial).replace(" away", ""))
                    BigStat("Time", formatDuration(totals.duration))
                    BigStat("Tracks", today.size.toString())
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Sessions Today", fontSize = 13.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(8.dp))

                today.forEach { track ->
                    val stats = remember(track) { computeTrackStats(track.points) }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.name, fontWeight = FontWeight.W500, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "${stats.runCount} runs  \u00B7  ${formatVertical(stats.verticalDescended ?: 0.0, imperial)}  \u00B7  ${formatDuration(stats.durationMs)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BigStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.W700, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline)
    }
}

private data class DailyTotals(
    val runs: Int,
    val vertical: Double,
    val maxSpeed: Double,
    val distance: Double,
    val duration: Long
)

private fun aggregate(tracks: List<Track>): DailyTotals {
    var runs = 0; var vertical = 0.0; var maxSpeed = 0.0; var distance = 0.0; var duration = 0L
    for (t in tracks) {
        val s = computeTrackStats(t.points)
        runs += s.runCount
        vertical += s.verticalDescended ?: 0.0
        if (s.maxSpeedKmh > maxSpeed) maxSpeed = s.maxSpeedKmh
        distance += s.distanceMeters
        duration += s.durationMs
    }
    return DailyTotals(runs, vertical, maxSpeed, distance, duration)
}

private fun filterToday(tracks: List<Track>): List<Track> {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val startOfDay = cal.timeInMillis
    return tracks.filter { it.startTime >= startOfDay }
}
