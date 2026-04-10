package com.florian.waypoint

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom sheet showing ski statistics with two tabs — "Today" and "All Time".
 *
 * Today: runs, vertical, max speed, distance, time, on-snow percentage, first/last
 * session times, a pace-of-day chart, and a list of today's sessions.
 *
 * All Time: lifetime totals, personal bests (max speed, best day vertical, most runs
 * in a day), and a monthly breakdown (newest month first).
 *
 * Also shows a streak banner when the user has recorded tracks on consecutive days.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyStatsSheet(
    store: WaypointStore,
    tracks: List<Track>,
    imperial: Boolean,
    onCompare: () -> Unit,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) } // 0 = today, 1 = all time
    val today = remember(tracks) { filterToday(tracks) }
    val todayTotals = remember(today) { aggregate(today) }
    val allTimeTotals = remember(tracks) { aggregate(tracks) }
    val pb = remember(tracks) { computePersonalBests(tracks) }
    val streak = remember(tracks) { computeStreak(tracks) }

    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { DragHandle() }) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Landscape, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Ski Stats", fontWeight = FontWeight.W600, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                if (tracks.size >= 2) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(modifier = Modifier.fillMaxSize().iosClickable(onCompare), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.CompareArrows, "Compare", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Streak banner
            if (streak >= 2) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$streak day streak", fontSize = 14.sp, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab toggle
            Row(modifier = Modifier.fillMaxWidth().height(38.dp)) {
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
                // ── Today ───────────────────────────────────────────
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
                        BigStat("On Snow", "${todayTotals.snowPercent}%")
                    }

                    // First / last
                    if (today.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "First: ${formatTimeOfDay(today.minOf { it.startTime })}  \u00B7  Last: ${formatTimeOfDay(today.maxOf { it.endTime })}",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }

                    // Pace of day chart — cumulative vertical
                    val allPoints = remember(today) { today.flatMap { it.points } }
                    if (allPoints.size >= 5) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Vertical Over Time", fontSize = 13.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        PaceChart(tracks = today, imperial = imperial)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
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
                // ── All time ────────────────────────────────────────
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

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Personal Bests", fontSize = 13.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        BigStat("Top Speed", formatTrackSpeed(pb.maxSpeedKmh))
                        BigStat("Best Day", formatVertical(pb.maxDayVertical, imperial))
                        BigStat("Most Runs", pb.maxDayRuns.toString())
                    }

                    // Monthly breakdown
                    val months = remember(tracks) { computeMonthlyBreakdown(tracks) }
                    if (months.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("By Month", fontSize = 13.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))

                        months.forEach { m ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.label, fontSize = 14.sp, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        "${m.days} day${if (m.days != 1) "s" else ""}  \u00B7  ${m.runs} run${if (m.runs != 1) "s" else ""}  \u00B7  max ${formatTrackSpeed(m.maxSpeedKmh)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        formatVertical(m.vertical, imperial),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.W600,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        formatDistance(m.distance, imperial).replace(" away", ""),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
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

@Composable
private fun PaceChart(tracks: List<Track>, imperial: Boolean) {
    // Build cumulative vertical descended over time across all today's tracks
    val data = remember(tracks) {
        val out = mutableListOf<Pair<Long, Double>>()
        var cumVert = 0.0
        for (t in tracks.sortedBy { it.startTime }) {
            val runs = computeRunBreakdown(t.points)
            for (r in runs) {
                out.add(r.startTime to cumVert)
                cumVert += r.verticalDrop
                out.add(r.endTime to cumVert)
            }
        }
        out
    }
    if (data.size < 2) return

    val minT = data.first().first
    val maxT = data.last().first.coerceAtLeast(minT + 1)
    val maxV = data.maxOf { it.second }.coerceAtLeast(1.0)

    val color = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
        Box {
            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp).padding(14.dp)) {
                val w = size.width; val h = size.height
                val line = Path(); val fill = Path()
                fill.moveTo(0f, h)
                data.forEachIndexed { i, (t, v) ->
                    val x = ((t - minT).toFloat() / (maxT - minT)) * w
                    val y = h - (v / maxV * h).toFloat()
                    if (i == 0) { line.moveTo(x, y); fill.lineTo(x, y) }
                    else { line.lineTo(x, y); fill.lineTo(x, y) }
                }
                val lastX = ((data.last().first - minT).toFloat() / (maxT - minT)) * w
                fill.lineTo(lastX, h); fill.close()
                drawPath(fill, color.copy(alpha = 0.15f))
                drawPath(line, color, style = Stroke(width = 2.5f * density))
            }
            Text("${formatVertical(maxV, imperial)}", fontSize = 10.sp, color = outline,
                modifier = Modifier.padding(start = 14.dp, top = 6.dp).align(Alignment.TopStart))
            Text("${formatTimeOfDay(minT)} \u2192 ${formatTimeOfDay(maxT)}", fontSize = 10.sp, color = outline,
                modifier = Modifier.padding(start = 14.dp, bottom = 6.dp).align(Alignment.BottomStart))
        }
    }
}

private data class Totals(
    val runs: Int,
    val vertical: Double,
    val maxSpeed: Double,
    val distance: Double,
    val duration: Long,
    val snowPercent: Int
)

private fun aggregate(tracks: List<Track>): Totals {
    var runs = 0; var vertical = 0.0; var maxSpeed = 0.0; var distance = 0.0; var duration = 0L
    var snowMs = 0L; var totalMs = 0L
    for (t in tracks) {
        val s = computeTrackStats(t.points)
        val tos = computeTimeOnSnow(t.points)
        runs += s.runCount
        vertical += s.verticalDescended ?: 0.0
        if (s.maxSpeedKmh > maxSpeed) maxSpeed = s.maxSpeedKmh
        distance += s.distanceMeters
        duration += s.durationMs
        snowMs += tos.snowMs
        totalMs += tos.snowMs + tos.liftMs
    }
    val snowPct = if (totalMs > 0) ((snowMs * 100) / totalMs).toInt() else 0
    return Totals(runs, vertical, maxSpeed, distance, duration, snowPct)
}

private fun filterToday(tracks: List<Track>): List<Track> {
    val dayStart = startOfDay()
    return tracks.filter { it.startTime >= dayStart }
}

private fun countUniqueDays(tracks: List<Track>): Int =
    tracks.map { startOfDay(it.startTime) }.toSet().size
