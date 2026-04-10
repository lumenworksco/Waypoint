package com.florian.waypoint

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyStatsSheet(store: WaypointStore, tracks: List<Track>, imperial: Boolean, onDismiss: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) } // 0 = today, 1 = all time
    val today = remember(tracks) { filterToday(tracks) }
    val todayTotals = remember(today) { aggregate(today) }
    val allTimeTotals = remember(tracks) { aggregate(tracks) }
    val pb = remember { store.loadPersonalBests() }

    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { DragHandle() }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Landscape, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Ski Stats", fontWeight = FontWeight.W600, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab toggle
            Row(
                modifier = Modifier.fillMaxWidth().height(38.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                listOf("Today", "All Time").forEachIndexed { i, label ->
                    val selected = tab == i
                    Surface(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        shape = if (i == 0) RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                        else RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(modifier = Modifier.fillMaxSize().iosClickable { tab = i }, contentAlignment = Alignment.Center) {
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.W600,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (tab == 0) {
                // Today
                if (today.isEmpty()) {
                    Text(
                        "No tracks recorded today. Start recording to see stats here.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        BigStat("Runs", todayTotals.runs.toString())
                        BigStat("Vertical", formatVertical(todayTotals.vertical, imperial))
                        BigStat("Max Speed", formatTrackSpeed(todayTotals.maxSpeed))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        BigStat("Distance", formatDistance(todayTotals.distance, imperial).replace(" away", ""))
                        BigStat("Time", formatDuration(todayTotals.duration))
                        BigStat("Sessions", today.size.toString())
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
            } else {
                // All time
                if (tracks.isEmpty()) {
                    Text(
                        "No tracks recorded yet.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        BigStat("Tracks", tracks.size.toString())
                        BigStat("Total Runs", allTimeTotals.runs.toString())
                        BigStat("Total Vertical", formatVertical(allTimeTotals.vertical, imperial))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        BigStat("Distance", formatDistance(allTimeTotals.distance, imperial).replace(" away", ""))
                        BigStat("Time", formatDuration(allTimeTotals.duration))
                        BigStat("Days", countUniqueDays(tracks).toString())
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Personal Bests", fontSize = 13.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        BigStat("Top Speed", formatTrackSpeed(pb.maxSpeedKmh))
                        BigStat("Best Day", formatVertical(pb.maxDayVertical, imperial))
                        BigStat("Most Runs", pb.maxDayRuns.toString())
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

private data class Totals(
    val runs: Int,
    val vertical: Double,
    val maxSpeed: Double,
    val distance: Double,
    val duration: Long
)

private fun aggregate(tracks: List<Track>): Totals {
    var runs = 0; var vertical = 0.0; var maxSpeed = 0.0; var distance = 0.0; var duration = 0L
    for (t in tracks) {
        val s = computeTrackStats(t.points)
        runs += s.runCount
        vertical += s.verticalDescended ?: 0.0
        if (s.maxSpeedKmh > maxSpeed) maxSpeed = s.maxSpeedKmh
        distance += s.distanceMeters
        duration += s.durationMs
    }
    return Totals(runs, vertical, maxSpeed, distance, duration)
}

private fun filterToday(tracks: List<Track>): List<Track> {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val startOfDay = cal.timeInMillis
    return tracks.filter { it.startTime >= startOfDay }
}

private fun countUniqueDays(tracks: List<Track>): Int {
    val days = mutableSetOf<Long>()
    val cal = Calendar.getInstance()
    for (t in tracks) {
        cal.timeInMillis = t.startTime
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        days.add(cal.timeInMillis)
    }
    return days.size
}
