package com.florian.waypoint

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailSheet(track: Track, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val stats = remember(track) { computeTrackStats(track.points) }

    ModalBottomSheet(onDismissRequest = onDismiss, dragHandle = { DragHandle() }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(Color(safeParseColor(track.color)), CircleShape))
                Spacer(modifier = Modifier.width(10.dp))
                Text(track.name, fontWeight = FontWeight.W600, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("Distance", formatDistance(stats.distanceMeters).replace(" away", ""))
                StatItem("Duration", formatDuration(stats.durationMs))
                StatItem("Avg Speed", formatTrackSpeed(stats.avgSpeedKmh))
            }

            if (stats.elevationGain != null || stats.elevationLoss != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    stats.elevationGain?.let { StatItem("Gain", "+${it.toInt()} m") }
                    stats.elevationLoss?.let { StatItem("Loss", "-${it.toInt()} m") }
                }
            }

            // Elevation profile
            val altitudes = track.points.mapNotNull { it.altitude?.takeIf { a -> a != 0.0 } }
            if (altitudes.size >= 3) {
                Spacer(modifier = Modifier.height(20.dp))
                Text("Elevation", fontSize = 13.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(10.dp))
                ElevationChart(altitudes = altitudes, color = Color(safeParseColor(track.color)))
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
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.W600, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.outline)
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
            // Labels
            Text("${maxAlt.toInt()} m", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 14.dp, top = 6.dp).align(Alignment.TopStart))
            Text("${minAlt.toInt()} m", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 14.dp, bottom = 6.dp).align(Alignment.BottomStart))
        }
    }
}
