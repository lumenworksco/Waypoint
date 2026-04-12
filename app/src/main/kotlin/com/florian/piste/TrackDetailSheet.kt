package com.florian.piste

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom sheet that displays the details of a recorded track.
 *
 * Shows distance, duration, vertical, max speed, time on snow, elevation gain/loss,
 * air time stats, an elevation profile chart, a run-by-run breakdown with difficulty
 * colors, and a share button that produces a PNG stats card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailSheet(
    track: Track,
    imperial: Boolean,
    onRename: (Track) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val stats = remember(track) { computeTrackStats(track.points) }
    val tos = remember(track) { computeTimeOnSnow(track.points) }
    val runs = remember(track) { computeRunBreakdown(track.points) }

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(track.name) }

    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { DragHandle() }) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(Color(safeParseColor(track.color)), CircleShape))
                Spacer(modifier = Modifier.width(10.dp))
                Text(track.name, fontWeight = FontWeight.W600, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().iosClickable { showRenameDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Edit, "Rename", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().iosClickable {
                            shareTrackImage(context, track, imperial)
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Share, "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("Distance", formatDistance(stats.distanceMeters, imperial).replace(" away", ""))
                StatItem("Duration", formatDuration(stats.durationMs))
                StatItem("Avg Speed", formatTrackSpeed(stats.avgSpeedKmh))
            }

            // Ski stats
            if (stats.verticalDescended != null || stats.maxSpeedKmh > 0 || stats.runCount > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    stats.verticalDescended?.let { StatItem("Vertical", formatVertical(it, imperial)) }
                    if (stats.maxSpeedKmh > 0) StatItem("Max Speed", formatTrackSpeed(stats.maxSpeedKmh))
                    if (stats.runCount > 0) StatItem("Runs", stats.runCount.toString())
                }
            }

            // Time on snow vs lifts
            if (tos.snowMs + tos.liftMs > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem("On Snow", "${tos.snowPercent}%")
                    StatItem("Snow Time", formatDuration(tos.snowMs))
                    StatItem("Lift Time", formatDuration(tos.liftMs))
                }
            }

            if (stats.elevationGain != null || stats.elevationLoss != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    stats.elevationGain?.let { StatItem("Gain", "+${formatVertical(it, imperial)}") }
                    stats.elevationLoss?.let { StatItem("Loss", "-${formatVertical(it, imperial)}") }
                }
            }

            // Air time
            if (track.biggestAirMs > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatItem("Biggest Air", "%.1f s".format(track.biggestAirMs / 1000.0))
                    StatItem("Jumps", track.airCount.toString())
                }
            }

            // Time of day
            if (track.points.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "${formatTimeOfDay(track.startTime)} \u2192 ${formatTimeOfDay(track.endTime)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Elevation profile
            val altitudes = track.points.mapNotNull { it.altitude?.takeIf { a -> a != 0.0 } }
            if (altitudes.size >= 3) {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Elevation", fontSize = 13.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(10.dp))
                ElevationChart(altitudes = altitudes, color = Color(safeParseColor(track.color)))
            }

            // Run breakdown
            if (runs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Runs", fontSize = 13.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(8.dp))
                runs.forEach { run -> RunRow(run = run, imperial = imperial) }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Delete
            Surface(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
            ) {
                Box(modifier = Modifier.fillMaxSize().iosClickable(onDelete), contentAlignment = Alignment.Center) {
                    Text("Delete Track", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.W600, fontSize = 17.sp)
                }
            }
        }
    }

    // Rename dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Track") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it.take(64) },
                    singleLine = true,
                    label = { Text("Track name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newName = renameText.trim().ifBlank { track.name }
                    onRename(track.copy(name = newName))
                    showRenameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.W600, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun RunRow(run: RunBreakdown, imperial: Boolean) {
    val diffColor = Color(difficultyColor(run.difficulty))
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(14.dp).background(diffColor, CircleShape))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Run ${run.index} \u00B7 ${run.difficulty.label}",
                fontWeight = FontWeight.W500, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "${formatVertical(run.verticalDrop, imperial)}  \u00B7  ${formatDuration(run.endTime - run.startTime)}",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.outline
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatTrackSpeed(run.maxSpeedKmh), fontSize = 13.sp, fontWeight = FontWeight.W600, color = MaterialTheme.colorScheme.onSurface)
            Text("max", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ElevationChart(altitudes: List<Double>, color: Color) {
    val smoothed = if (altitudes.size >= 5) {
        altitudes.indices.map { i ->
            altitudes.subList(maxOf(0, i - 2), minOf(altitudes.size, i + 3)).average()
        }
    } else altitudes

    val minAlt = smoothed.minOrNull() ?: return; val maxAlt = smoothed.maxOrNull() ?: return
    val range = (maxAlt - minAlt).coerceAtLeast(1.0)

    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
        Box {
            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp).padding(14.dp)) {
                val w = size.width; val h = size.height
                if (smoothed.size < 2) return@Canvas

                val line = Path(); val fill = Path()
                fill.moveTo(0f, h)
                smoothed.forEachIndexed { i, alt ->
                    val x = i.toFloat() / (smoothed.size - 1) * w
                    val y = h - ((alt - minAlt) / range * h).toFloat()
                    if (i == 0) { line.moveTo(x, y); fill.lineTo(x, y) }
                    else { line.lineTo(x, y); fill.lineTo(x, y) }
                }
                fill.lineTo(w, h); fill.close()
                drawPath(fill, color.copy(alpha = 0.12f))
                drawPath(line, color, style = Stroke(width = 2.dp.toPx()))
            }
            Text("${maxAlt.toInt()} m", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 14.dp, top = 6.dp).align(Alignment.TopStart))
            Text("${minAlt.toInt()} m", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 14.dp, bottom = 6.dp).align(Alignment.BottomStart))
        }
    }
}
